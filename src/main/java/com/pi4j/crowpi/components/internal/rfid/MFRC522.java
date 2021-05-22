package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.Component;
import com.pi4j.crowpi.components.exceptions.RfidCollisionException;
import com.pi4j.crowpi.components.exceptions.RfidException;
import com.pi4j.crowpi.components.exceptions.RfidTimeoutException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

import java.util.*;

public class MFRC522 extends Component {
    private final DigitalOutput resetPin;
    private final Spi spi;

    private static final long PCD_CHECKSUM_TIMEOUT_MS = 100;
    private static final long PICC_COMMAND_TIMEOUT_MS = 250;
    private static final byte PICC_MIFARE_ACK = 0xA;

    public MFRC522(DigitalOutput resetPin, Spi spi) {
        this.resetPin = resetPin;
        this.spi = spi;
        this.init();
    }

    private void init() {
        // Reset component to initial state
        reset();

        // Setup internal timer with 40kHz / 25us and 25ms auto-timeout
        writeRegister(PcdRegister.T_MODE_REG, (byte) 0b1000_0000); // TAuto[1], TGated[00], TAutoRestart[0], TPrescaler_Hi[0000]
        writeRegister(PcdRegister.T_PRESCALER_REG, (byte) 0b1010_1001); // TPrescaler_Lo[10101001]
        writeRegister(PcdRegister.T_RELOAD_REG_HIGH, PcdRegister.T_RELOAD_REG_LOW, (short) 1000); // TReloadReg[25ms]

        // Setup modulation and CRC coprocessor
        writeRegister(PcdRegister.TX_ASK_REG, (byte) 0b0100_0000); // Force100ASK[1]
        writeRegister(PcdRegister.MODE_REG, (byte) 0b0011_1101); // TxWaitRF[1], PolMFin[1], CRCPreset[01] (= 0x6363 / ISO 14443-3 CRC_A)

        // Enable antenna to communicate with nearby PICCs
        setAntennaState(true);
    }

    public void setAntennaState(boolean on) {
        if (on) {
            setBitMask(PcdRegister.TX_CONTROL_REG, (byte) 0x03); // Tx1RFEn[1], Tx1RFEn[1]
        } else {
            clearBitMask(PcdRegister.TX_CONTROL_REG, (byte) 0x03); // Tx1RFEn[0], Tx1RFEn[0]
        }
    }

    public boolean isCardPresent() {
        resetTransmission();
        try {
            requestA(new byte[2]);
            return true;
        } catch (RfidCollisionException ignored) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void reset() {
        resetSystem();
        resetTransmission();
    }

    private void resetTransmission() {
        // Reset baud rates
        writeRegister(PcdRegister.TX_MODE_REG, (byte) 0x00); // TxSpeed[000] = 106kBd
        writeRegister(PcdRegister.RX_MODE_REG, (byte) 0x00); // TxSpeed[000] = 106kBd

        // Reset modulation width
        writeRegister(PcdRegister.MOD_WIDTH_REG, (byte) 0x26); // ModWidth[0x26]
    }

    private void resetSystem() {
        // If reset pin is LOW (= device is shutdown), set it to HIGH to leave power-down mode
        // Otherwise execute a soft-reset command
//        if (this.resetPin.isLow()) {
//            this.resetPin.high();
//        } else {
//            this.executePcd(PcdCommand.SOFT_RESET);
//        }

        // FIXME
        if (this.resetPin.isHigh()) {
            this.resetPin.low();
            sleep(100);
        }
        this.resetPin.high();

        // Give the PCD some time to startup
        sleep(50);

        // Ensure that soft power-down mode is no longer active
        while ((readRegister(PcdRegister.COMMAND_REG) & (1 << 4)) != 0) {
            sleep(10);
        }

        // Deauthenticate from previous PICC, this is not covered by soft resets
        mifareStopCrypto1();
    }

    protected RfidUid select() throws RfidException {
        return select(0);
    }

    @SuppressWarnings("SameParameterValue")
    private RfidUid select(int validBits) throws RfidException {
        // Prepare buffer with 9 bytes length (7 byte UID + 2 byte CRC_A)
        // This buffer must contain the following structure:
        //      Byte 0: SEL         Cascade Level Selector
        //      Byte 1: NVB         Number of Valid Bits, high nibble = full bytes, low nibble = extra bits
        //      Byte 2: UID or CT   UID data or Cascade Tag
        //      Byte 3: UID         UID data
        //      Byte 4: UID         UID data
        //      Byte 5: UID         UID data
        //      Byte 6: BCC         Block Check Character, equals XOR of bytes 2-5
        //      Byte 7: CRC_A       Checksum
        //      Byte 8: CRC_A       Checksum
        // The BCC and CRC_A may only be transmitted once all UID bits are known.
        final var buffer = new byte[9];

        // Clear all received bits after a collision
        writeRegister(PcdRegister.COLL_REG, (byte) 0x80); // ValuesAfterColl[1]

        // Initialize state for cascading loop
        final var uidBytes = new ArrayList<Byte>();
        boolean uidComplete = false;
        byte uidSak = 0;
        PiccCascadeLevel cascadeLevel = PiccCascadeLevel.CL1;

        // Loop until all required cascading levels have been processed
        while (!uidComplete) {
            System.out.println("Processing cascade level: " + cascadeLevel);

            // Initialize for current cascade level and determine if cascade tag must be parsed
            buffer[0] = cascadeLevel.getCommand().getValue();
            final int uidOffset = cascadeLevel.getUidOffset();
            final boolean useCascadeTag = (validBits != 0) && (uidBytes.size() > cascadeLevel.getNextThreshold());

            // Calculate how many bits should be known at the current cascade level
            // If result is negative, clamp to zero
            int knownLevelBits = Math.max(0, validBits - (8 * uidOffset));

            // Add cascade tag command to buffer if needed
            int bufferIndex = 2;
            if (useCascadeTag) {
                buffer[bufferIndex++] = PiccCommand.CASCADE_TAG.getValue();
            }

            // Copy required amount of bytes into buffer to represent all known bits
            final int bytesRequired = knownLevelBits / 8 + (knownLevelBits % 8 == 0 ? 0 : 1);
            if (bytesRequired > 0) {
                // Restrict copying to four bytes or three bytes when using CT
                final int maxBytesToCopy = useCascadeTag ? 3 : 4;
                final int bytesToCopy = Math.min(bytesRequired, maxBytesToCopy);

                // Copy bytes into buffer starting from UID offset
                for (int i = 0; i < bytesToCopy; i++) {
                    buffer[bufferIndex++] = uidBytes.get(uidOffset + i);
                }
            }

            // Add size of cascade tag to current known bits after copying data
            if (useCascadeTag) {
                knownLevelBits += 8;
            }

            // Initialize variables used by anti-collision loop
            byte[] responseBuffer = null;
            int responseLength = 0;

            // Repeat anti-collision loop until all UID bits + BCC have been transmitted
            // Abort if no full UID has been retrieved after 32 iterations
            boolean selectComplete = false;
            int iterationsLeft = 32;
            while (!selectComplete && iterationsLeft-- > 0) {
                final int txFullBytes;
                final int txExtraBits;
                final int rxTargetOffset;
                final int rxBytes;

                // Determine if we are handling SELECT or ANTI COLLISION based on known bits for this cascade level
                if (knownLevelBits >= 32) {
                    // We know all bits in this cascade level and can treat this as a SELECT
                    System.out.println("Prepare SELECT request");

                    // Set NVB and BCC values in buffer
                    buffer[1] = 0x70; // Set NVB to 7 full bytes and 0 extra bits
                    buffer[6] = (byte) (buffer[2] ^ buffer[3] ^ buffer[4] ^ buffer[5]); // Calculate BCC using XOR

                    // Calculate CRC_A checksum of first 7 bytes and copy into buffer
                    final var checksum = calculateCrc(Arrays.copyOfRange(buffer, 0, 7));
                    System.arraycopy(checksum, 0, buffer, 7, Math.min(2, checksum.length));

                    // Transmit all 9 bytes without any extra bits
                    txFullBytes = 9;
                    txExtraBits = 0;

                    // Copy result into buffer starting at 6th byte to get BCC and CRC_A (3 bytes)
                    rxTargetOffset = 6;
                    rxBytes = 3;
                } else {
                    // We do not know all bits in this cascade level and must treat this as ANTI COLLISION
                    System.out.println("Prepare ANTI COLLISION request");

                    // Calculate required size of transmit buffer
                    txFullBytes = (knownLevelBits / 8) + 2; // Calculate number of fully known bytes (+2 for SEL and NVB)
                    txExtraBits = knownLevelBits % 8; // Calculate number of extra bits

                    // Update NVB with calculated size
                    buffer[1] = (byte) (((txFullBytes & 0xF) << 4) + (txExtraBits & 0xF));

                    // Copy result into unused part of buffer (starts after previously filled bytes)
                    rxTargetOffset = txFullBytes;
                    rxBytes = buffer.length - txFullBytes;
                }

                // Prepare request buffer based on required size for storing all full bytes and extra bits
                final int txBytes = txFullBytes + (txExtraBits != 0 ? 1 : 0);
                final var requestBuffer = Arrays.copyOfRange(buffer, 0, txBytes);

                System.out.println("Request buffer: " + ByteHelpers.toString(requestBuffer));
                System.out.println("Request buffer length: " + requestBuffer.length);

                // Try to receive UID for this cascade level from PICC
                try {
                    // Transceive request buffer to PCD and use same amount of TX/RX extra bits
                    final var response = transceivePicc(requestBuffer, txExtraBits, txExtraBits);

                    // Store buffer and length in result variables outside of this loop
                    responseBuffer = response.getBytes();
                    responseLength = response.getLength();
                    System.out.println("Response offset: " + rxTargetOffset);

                    // Copy response back into buffer starting at previously determined offset
                    System.arraycopy(responseBuffer, 0, buffer, rxTargetOffset, Math.min(rxBytes, responseLength));

                    // Debug
                    System.out.println("Response buffer: " + ByteHelpers.toString(responseBuffer) + " (" + responseBuffer.length + " bytes)");
                    System.out.println("Response length: " + responseLength);

                    // Determine how to continue
                    System.out.println("Known bits of cascade level: " + knownLevelBits);
                    if (knownLevelBits >= 32) {
                        // This has been a SELECT, we can finalize processing
                        System.out.println("Handling SELECT response...");

                        // Verify that SAK (Select Acknowledge) of response is valid
                        if (responseLength != 3 || txExtraBits != 0) {
                            throw new RfidException("Received invalid SAK from PICC, expected exactly 24 bits");
                        }

                        // Break out of this anti-collision loop
                        selectComplete = true;
                    } else {
                        // This has been an ANTI COLLISION, we must continue but now know all bits of this cascade level
                        System.out.println("Handling ANTI COLLISION response...");
                        knownLevelBits = 32;
                    }
                } catch (RfidCollisionException ignored) {
                    System.out.println("Handling collision exception...");

                    // Retrieve collision position register from PICC
                    final byte collReg = readRegister(PcdRegister.COLL_REG);
                    if ((collReg & 0x20) != 0) {
                        throw new RfidCollisionException("Can not continue due to in valid collision position");
                    }

                    // Calculate collision position as a value between 1 and 32
                    // The PCD returns 0 for the 32th bit, so we map 0 to 32
                    final int collisionPos = (collReg & 0x1F) != 0 ? (collReg & 0x1F) : 32;

                    // Abort if we made no progress in this iteration
                    if (collisionPos <= knownLevelBits) {
                        throw new RfidCollisionException("Can not continue due to lack of progress in anti collision routine");
                    }

                    // Calculate the index of the byte and bit where the collision occurred
                    final int collisionBit = (collisionPos - 1) % 8;
                    final int collisionByte = (collisionPos / 8) + (collisionBit != 0 ? 1 : 0);

                    // Set the bit at the determined position and update known level bits
                    buffer[collisionByte] |= (1 << collisionBit);
                    knownLevelBits = collisionPos;
                }
            }
            // End of anti-collision loop, UID is either complete or failed due to too many iterations

            // Throw an exception if we received no valid response
            if (responseBuffer == null || responseLength < 1) {
                throw new RfidException("Could not finish selection of target PICC as part of anti-collision routine");
            }

            // Verify CRC_A checksum of SAK
            final byte[] actualChecksum = new byte[]{responseBuffer[1], responseBuffer[2]};
            final byte[] expectedChecksum = calculateCrc(new byte[]{responseBuffer[0]});
            System.out.println("Expected SAK checksum: " + ByteHelpers.toString(expectedChecksum));
            System.out.println("Actual SAK checksum: " + ByteHelpers.toString(actualChecksum));
            if (!Arrays.equals(actualChecksum, expectedChecksum)) {
                throw new RfidException("Checksum of SAK does not match expected value");
            }

            // Calculate how many UID bytes where retrieved
            final boolean hasCascadeTag = buffer[2] == PiccCommand.CASCADE_TAG.getValue();
            final int bufferOffset = hasCascadeTag ? 3 : 2;
            final int bytesToCopy = hasCascadeTag ? 3 : 4;

            // Copy retrieved UID bytes into result buffer
            for (int i = 0; i < bytesToCopy; i++) {
                uidBytes.add(buffer[bufferOffset + i]);
                if (uidBytes.size() != uidOffset + i + 1) {
                    throw new RfidException("UID result buffer has invalid size");
                }
            }

            // Determine if we need to continue or have the whole UID
            if ((responseBuffer[0] & 0x04) != 0) {
                // Cascade bit is set, we have to go deeper :-)
                cascadeLevel = cascadeLevel.getNext();
            } else {
                // Cascade bit is unset, we have everything
                uidComplete = true;
                uidSak = responseBuffer[0];
            }
        }

        // Return new tag instance
        return new RfidUid(uidBytes, uidSak);
    }

    protected void mifareAuth(MifareKey key, byte blockAddr, RfidUid uid) throws RfidException {
        // Prepare buffer for authentication command
        final byte[] buffer = new byte[12];
        buffer[0] = key.getType().getCommand().getValue();
        buffer[1] = blockAddr;
        System.arraycopy(key.getBytes(), 0, buffer, 2, Math.min(6, key.getLength()));
        System.arraycopy(uid.getUid(), uid.getUidLength() - 4, buffer, 8, Math.min(4, uid.getUidLength()));

        System.out.println("Auth buffer: " + ByteHelpers.toString(buffer));

        // Start authentication against PICC
        sendPiccRequest(PcdCommand.MF_AUTHENT, PcdComIrq.IDLE_IRQ, buffer);
    }

    public void mifareStopCrypto1() {
        clearBitMask(PcdRegister.STATUS_2_REG, (byte) 0x08); // MFCrypto1On[0]
    }

    public byte[] mifareRead(byte blockAddr) throws RfidException {
        // Construct payload and calculate CRC_A checksum
        final var payload = new byte[]{PiccCommand.MF_READ.getValue(), blockAddr};
        final var checksum = calculateCrc(payload);

        // Build buffer based on payload and CRC_A checksum
        final var buffer = new byte[payload.length + checksum.length];
        System.arraycopy(payload, 0, buffer, 0, payload.length);
        System.arraycopy(checksum, 0, buffer, payload.length, checksum.length);

        final var response = transceivePicc(buffer, 0, 0, true);
        return response.getBytes();
    }

    public void mifareWrite(byte blockAddr, byte[] dataBuffer) throws RfidException {
        final var cmdBuffer = new byte[]{PiccCommand.MF_WRITE.getValue(), blockAddr};
        System.out.println("MIFARE Write Part 1: " + ByteHelpers.toString(cmdBuffer));
        mifareTransceive(cmdBuffer);
        System.out.println("MIFARE Write Part 2: " + ByteHelpers.toString(dataBuffer));
        mifareTransceive(dataBuffer);
    }

    private void mifareTransceive(byte[] payload) throws RfidException {
        // Ensure payload is not too long
        if (payload == null || payload.length > 16) {
            throw new IllegalArgumentException("Payload must be a byte array with up to 16 bytes");
        }

        // Calculate CRC_A checksum for payload
        final var checksum = calculateCrc(payload);

        // Generate final buffer with payload and checksum
        final var buffer = new byte[payload.length + checksum.length];
        System.arraycopy(payload, 0, buffer, 0, payload.length);
        System.arraycopy(checksum, 0, buffer, payload.length, checksum.length);

        // Transceive buffer to PICC and verify response
        final var response = transceivePicc(buffer);
        System.out.println("Response: " + ByteHelpers.toString(response.getBytes()));
        System.out.println("Response Bits: " + response.getLastBits());
        if (response.getBytes().length != 1 || response.getLastBits() != 4) {
            throw new RfidException("PICC response must be exactly 4 bits for MIFARE ACK");
        }
        if (response.getBytes()[0] != PICC_MIFARE_ACK) {
            throw new RfidException("Received MIFARE NACK from PICC due to unknown error");
        }
    }

    private byte[] calculateCrc(byte[] data) throws RfidTimeoutException {
        // Trigger CRC_A checksum calculation on PCD
        executePcd(PcdCommand.IDLE); // Pause any active command
        writeRegister(PcdRegister.DIV_IRQ_REG, (byte) 0x04); // Clear CRCIRq interrupt request bits
        writeRegister(PcdRegister.FIFO_LEVEL_REG, (byte) 0x80); // FlushBuffer[7], FIFOLevel[0]
        writeRegister(PcdRegister.FIFO_DATA_REG, data); // Write data to FIFO buffer
        executePcd(PcdCommand.CALC_CRC); // Start the CRC calculation

        // Wait for CRC calculation to complete
        final long deadline = System.currentTimeMillis() + PCD_CHECKSUM_TIMEOUT_MS;
        do {
            byte divIrqReg = readRegister(PcdRegister.DIV_IRQ_REG);
            if (PcdDivIrq.CRC_IRQ.isSet(divIrqReg)) {
                // Stop CRC calculation for future FIFO buffer content
                executePcd(PcdCommand.IDLE);

                // Transfer calculated checksum from PCD into result buffer
                final var result = new byte[2];
                result[0] = readRegister(PcdRegister.CRC_RESULT_REG_LOW);
                result[1] = readRegister(PcdRegister.CRC_RESULT_REG_HIGH);
                return result;
            }
        } while (System.currentTimeMillis() < deadline);

        // Throw exception if timeout was reached
        throw new RfidTimeoutException("CRC calculation deadline reached after " + PCD_CHECKSUM_TIMEOUT_MS + " milliseconds");
    }

    private void requestA(byte[] buffer) throws RfidException {
        requestOrWakeupA(PiccCommand.REQA, buffer);
    }

    private void wakeupA(byte[] buffer) throws RfidException {
        requestOrWakeupA(PiccCommand.WUPA, buffer);
    }

    private void requestOrWakeupA(PiccCommand command, byte[] buffer) throws RfidException {
        // Ensure command is supported by this method
        if (command != PiccCommand.REQA && command != PiccCommand.WUPA) {
            throw new IllegalArgumentException("Command must be either REQA or WUPA");
        }

        // Ensure buffer is valid and contains two bytes
        if (buffer == null || buffer.length != 2) {
            throw new IllegalArgumentException("Buffer for REQA/WUPA command must be two bytes long");
        }

        // Clear all received bits after a collision
        writeRegister(PcdRegister.COLL_REG, (byte) 0x80); // ValuesAfterColl[1]

        // Use 7-bit frames for REQA or WUPA and transceive to PICC
        final var response = transceivePicc(new byte[]{command.getValue()}, 7);

        // Ensure response is valid
        if (response.getLength() != 2 || response.getLastBits() != 0) {
            throw new RfidException("Received invalid response to REQA/WUPA command");
        }

        System.out.println(response);
    }

    private PiccResponse transceivePicc(byte[] txData) throws RfidException {
        return transceivePicc(txData, 0);
    }

    private PiccResponse transceivePicc(byte[] txData, int txLastBits) throws RfidException {
        return transceivePicc(txData, txLastBits, 0);
    }

    private PiccResponse transceivePicc(byte[] txData, int txLastBits, int rxAlignBits) throws RfidException {
        return transceivePicc(txData, txLastBits, rxAlignBits, false);
    }

    private PiccResponse transceivePicc(byte[] txData, int txLastBits, int rxAlignBits, boolean checkCrc) throws RfidException {
        final var waitIrq = Set.of(PcdComIrq.RX_IRQ, PcdComIrq.IDLE_IRQ);
        return sendPiccRequest(PcdCommand.TRANSCEIVE, waitIrq, txData, txLastBits, rxAlignBits, checkCrc);
    }

    private PiccResponse sendPiccRequest(PcdCommand command, PcdComIrq waitIrq, byte[] txData) throws RfidException {
        return sendPiccRequest(command, Set.of(waitIrq), txData);
    }

    private PiccResponse sendPiccRequest(PcdCommand command, Set<PcdComIrq> waitIrq, byte[] txData) throws RfidException {
        return sendPiccRequest(command, waitIrq, txData, 0, 0, false);
    }

    private PiccResponse sendPiccRequest(PcdCommand command, Set<PcdComIrq> waitIrq, byte[] txData, int txLastBits, int rxAlignBits, boolean checkCrc) throws RfidException {
        // Calculate adjustments for bit-oriented frames
        // BitFramingReg[6..4] => RxAlign, position of first bit to be stored in FIFO, 0 = use all bits
        // BitFramingReg[2..0] => TxLastBits, number of transmitted bits in last byte, 0 = use all bits
        final var bitFraming = (byte) (((rxAlignBits & 0x7) << 4) + (txLastBits & 0x7));

        // Prepare PCD for communication with PICC
        executePcd(PcdCommand.IDLE);
        writeRegister(PcdRegister.COM_IRQ_REG, (byte) 0b01111111); // Clear all interrupt request bits
        writeRegister(PcdRegister.FIFO_LEVEL_REG, (byte) 0b10000000); // FlushBuffer[1], FIFOLevel[0000000]
        writeRegister(PcdRegister.FIFO_DATA_REG, txData); // Write TX data to FIFO
        writeRegister(PcdRegister.BIT_FRAMING_REG, bitFraming); // Set bit adjustments for RX/TX
        executePcd(command);

        // Enable StartSend=1 flag in BitFramingReg for transceive command to start transmission
        if (command == PcdCommand.TRANSCEIVE) {
            setBitMask(PcdRegister.BIT_FRAMING_REG, (byte) 0x80); // StartSend[1]
        }

        // Wait for completion of command execution with timeout
        boolean deadlineReached = true;
        final long deadline = System.currentTimeMillis() + PICC_COMMAND_TIMEOUT_MS;
        do {
            final byte comIrqReg = readRegister(PcdRegister.COM_IRQ_REG);
            // Did we receive any of the expected IRQs? If so, break out of loop without timeout
            if (waitIrq.stream().anyMatch(irq -> irq.isSet(comIrqReg))) {
                deadlineReached = false;
                break;
            }
            // Did we receive a timer IRQ? Nothing happened for 25ms (see init() method), abort with timeout
            if (PcdComIrq.TIMER_IRQ.isSet(comIrqReg)) {
                break;
            }
        } while (System.currentTimeMillis() < deadline);

        // Clear StartSend flag which may have been previously set
        clearBitMask(PcdRegister.BIT_FRAMING_REG, (byte) 0x80); // StartSend[0]

        // Handle potential timeout of PICC command execution
        if (deadlineReached) {
            throw new RfidTimeoutException("Deadline reached after " + PICC_COMMAND_TIMEOUT_MS + " milliseconds");
        }

        // Throw an exception if error register contains any unexpected error
        final byte errorReg = readRegister(PcdRegister.ERROR_REG);
        final var earlyError = PcdError.matchErrReg(errorReg, PcdError.BUFFER_OVFL, PcdError.PARITY_ERR, PcdError.PROTOCOL_ERR);
        if (earlyError != null) {
            throw new RfidException(earlyError);
        }

        // Prepare default response values
        byte[] rxData = new byte[0];
        int rxLength = 0;
        int rxLastBits = 0;

        // Receive data from PICC if not authentication
        if (command != PcdCommand.MF_AUTHENT) {
            rxLength = readRegister(PcdRegister.FIFO_LEVEL_REG) & 0xFF;
            rxData = readRegister(PcdRegister.FIFO_DATA_REG, rxLength, rxAlignBits);
            rxLastBits = readRegister(PcdRegister.CONTROL_REG) & 0x07;
        }

        // Check for collision error
        var lateError = PcdError.matchErrReg(errorReg, PcdError.COLL_ERR);
        if (lateError != null) {
            throw new RfidCollisionException();
        }

        // Perform CRC checks if enabled and response is not empty
        if (rxLength > 0 && checkCrc) {
            // NAK responses from MIFARE Classic PICCs are unsupported
            if (rxLength == 1 && rxLastBits == 4) {
                throw new RfidException("MIFARE lassic NAK can not be used with CRC checksum verification");
            }

            // At least two full CRC_A checksum bytes are required
            if (rxLength < 2 || rxLastBits != 0) {
                throw new RfidException("Receive buffer is too small for CRC checksum verification");
            }

            // Determine actual checksum and calculate expected checksum using CRC_A
            final var actualChecksum = new byte[]{rxData[rxLength - 2], rxData[rxLength - 1]};
            final var expectedChecksum = calculateCrc(Arrays.copyOfRange(rxData, 0, rxLength - 2));

            // Throw an exception if the checksums do not match
            if (!Arrays.equals(actualChecksum, expectedChecksum)) {
                throw new RfidException("CRC checksum mismatch during verification");
            }

            // Strip CRC from response
            rxLength -= 2;
            rxData = Arrays.copyOfRange(rxData, 0, rxLength);
        }

        return new PiccResponse(rxData, rxLength, rxLastBits);
    }

    private void executePcd(PcdCommand command) {
        writeRegister(PcdRegister.COMMAND_REG, command.getValue());
    }

    private void writeRegister(PcdRegister register, byte value) {
        spi.transfer(new byte[]{register.getWriteAddress(), value});
    }

    private void writeRegister(PcdRegister register, byte[] values) {
        final var buffer = new byte[values.length + 1];
        buffer[0] = register.getWriteAddress();
        System.arraycopy(values, 0, buffer, 1, values.length);

        spi.transfer(buffer);
    }

    private void writeRegister(PcdRegister registerHigh, PcdRegister registerLow, short value) {
        int tmp = value & 0xFFFF;
        writeRegister(registerHigh, (byte) ((tmp >> 8) & 0xFF));
        writeRegister(registerLow, (byte) (tmp & 0xFF));
    }

    private byte readRegister(PcdRegister register) {
        final var buffer = new byte[]{register.getReadAddress(), 0};
        spi.transfer(buffer);
        return buffer[1];
    }

    private byte[] readRegister(PcdRegister register, int length, int rxAlignBits) {
        // Break out early if zero-length was given
        if (length == 0) {
            return new byte[]{};
        }

        // Create buffer for retrieving data
        final var buffer = new byte[length + 1];
        for (int i = 0; i < length; i++) {
            buffer[i] = register.getReadAddress();
        }
        buffer[buffer.length - 1] = 0;

        // Transfer buffer
        spi.transfer(buffer);

        // Prepare result buffer
        final var result = new byte[length];
        int resultIndex = 0;

        // Start at buffer position 1 as the first byte (where the command was stored) is always zero
        int bufferIndex = 1;

        // Adjust first byte for bit-oriented frames
        if (rxAlignBits != 0) {
            // Create bitmask where LSB is shifted by given amount
            byte mask = (byte) ((0xFF << rxAlignBits) & 0xFF);
            // Mask received first byte and store into result buffer
            result[resultIndex++] = (byte) (buffer[bufferIndex++] & ~mask);
        }

        // Copy all pending bytes into the result buffer
        System.arraycopy(buffer, bufferIndex, result, resultIndex, length - bufferIndex + 1);

        return result;
    }

    private void setBitMask(PcdRegister register, byte mask) {
        final byte oldValue = readRegister(register);
        final byte newValue = (byte) (oldValue | mask);
        if (oldValue != newValue) {
            writeRegister(register, (byte) (oldValue | mask));
        }
    }

    private void clearBitMask(PcdRegister register, byte mask) {
        final byte oldValue = readRegister(register);
        final byte newValue = (byte) (oldValue & ~mask);
        if (oldValue != newValue) {
            writeRegister(register, (byte) (oldValue & ~mask));
        }
    }

}
