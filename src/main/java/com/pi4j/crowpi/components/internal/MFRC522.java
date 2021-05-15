package com.pi4j.crowpi.components.internal;

import com.pi4j.crowpi.components.Component;
import com.pi4j.crowpi.components.exceptions.NfcCollisionException;
import com.pi4j.crowpi.components.exceptions.NfcException;
import com.pi4j.crowpi.components.exceptions.NfcTimeoutException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

import java.util.*;

public class MFRC522 extends Component {
    private final DigitalOutput resetPin;
    private final Spi spi;

    private static final long PCD_CHECKSUM_TIMEOUT_MS = 100;
    private static final long PICC_COMMAND_TIMEOUT_MS = 250;

    public MFRC522(DigitalOutput resetPin, Spi spi) {
        this.resetPin = resetPin;
        this.spi = spi;
        this.init();
    }

    private void init() {
        resetSystem();
        resetTransmission();

        // Setup internal timer with 40kHz / 25us and 25ms auto-timeout
        writeRegister(PcdRegister.T_MODE_REG, (byte) 0b1000_0000); // TAuto[1], TGated[00], TAutoRestart[0], TPrescaler_Hi[0000]
        writeRegister(PcdRegister.T_PRESCALER_REG, (byte) 0b1010_1001); // TPrescaler_Lo[10101001]
        writeRegister(PcdRegister.T_RELOAD_REG_HIGH, PcdRegister.T_RELOAD_REG_LOW, (short) 1000); // TReloadReg[25ms]

        // Setup modulation and CRC coprocessor
        writeRegister(PcdRegister.TX_ASK_REG, (byte) 0b0100_0000); // Force100ASK[1]
        writeRegister(PcdRegister.MODE_REG, (byte) 0b0011_1101); // TxWaitRF[1], PolMFin[1], CRCPreset[01] (= 0x6363 / ISO 14443-3 CRC_A)

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
        } catch (NfcCollisionException e) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
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
        if (!this.resetPin.isHigh()) {
            this.resetPin.high();
        } else {
            this.executePcd(PcdCommand.SOFT_RESET);
        }

        // Give the PCD some time to startup
        sleep(50);

        // Ensure that soft power-down mode is no longer active
        while (true) {
            final var softPowerDown = (readRegister(PcdRegister.COMMAND_REG) & (1 << 4)) != 0;
            if (!softPowerDown) {
                break;
            }
            sleep(10);
        }
    }

    protected Tag select() throws NfcException {
        return select(0);
    }

    @SuppressWarnings("SameParameterValue")
    private Tag select(int validBits) throws NfcException {
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
                            throw new NfcException("Received invalid SAK from PICC, expected exactly 24 bits");
                        }

                        // Break out of this anti-collision loop
                        selectComplete = true;
                    } else {
                        // This has been an ANTI COLLISION, we must continue but now know all bits of this cascade level
                        System.out.println("Handling ANTI COLLISION response...");
                        knownLevelBits = 32;
                    }
                } catch (NfcCollisionException ignored) {
                    System.out.println("Handling collision exception...");

                    // Retrieve collision position register from PICC
                    final byte collReg = readRegister(PcdRegister.COLL_REG);
                    if ((collReg & 0x20) != 0) {
                        throw new NfcCollisionException("Can not continue due to in valid collision position");
                    }

                    // Calculate collision position as a value between 1 and 32
                    // The PCD returns 0 for the 32th bit, so we map 0 to 32
                    final int collisionPos = (collReg & 0x1F) != 0 ? (collReg & 0x1F) : 32;

                    // Abort if we made no progress in this iteration
                    if (collisionPos <= knownLevelBits) {
                        throw new NfcCollisionException("Can not continue due to lack of progress in anti collision routine");
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
                throw new NfcException("Could not finish selection of target PICC as part of anti-collision routine");
            }

            // Verify CRC_A checksum of SAK
            final byte[] actualChecksum = new byte[]{responseBuffer[1], responseBuffer[2]};
            final byte[] expectedChecksum = calculateCrc(new byte[]{responseBuffer[0]});
            System.out.println("Expected SAK checksum: " + ByteHelpers.toString(expectedChecksum));
            System.out.println("Actual SAK checksum: " + ByteHelpers.toString(actualChecksum));
            if (!Arrays.equals(actualChecksum, expectedChecksum)) {
                throw new NfcException("Checksum of SAK does not match expected value");
            }

            // Calculate how many UID bytes where retrieved
            final boolean hasCascadeTag = buffer[2] == PiccCommand.CASCADE_TAG.getValue();
            final int bufferOffset = hasCascadeTag ? 3 : 2;
            final int bytesToCopy = hasCascadeTag ? 3 : 4;

            // Copy retrieved UID bytes into result buffer
            for (int i = 0; i < bytesToCopy; i++) {
                uidBytes.add(buffer[bufferOffset + i]);
                if (uidBytes.size() != uidOffset + i + 1) {
                    throw new NfcException("UID result buffer has invalid size");
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
        return new Tag(uidBytes, uidSak);
    }

    protected final static class Tag {
        private final byte[] uid;
        private final byte sak;
        private final String serial;

        public Tag(byte[] uid, byte sak) {
            this.uid = uid;
            this.sak = sak;
            this.serial = ByteHelpers.toString(uid);
        }

        public Tag(List<Byte> uidBytes, byte sak) {
            this(ByteHelpers.toArray(uidBytes), sak);
        }

        public byte[] getUid() {
            return uid;
        }

        public byte getSak() {
            return sak;
        }

        public String getSerial() {
            return serial;
        }

        @Override
        public String toString() {
            return "Tag{uid=" + serial + ", sak=" + ByteHelpers.toString(sak) + "}";
        }
    }

    private byte[] calculateCrc(byte[] data) throws NfcTimeoutException {
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
        throw new NfcTimeoutException("CRC calculation deadline reached after " + PCD_CHECKSUM_TIMEOUT_MS + " milliseconds");
    }

    protected void requestA(byte[] buffer) throws NfcException {
        requestOrWakeupA(PiccCommand.REQA, buffer);
    }

    protected void wakeupA(byte[] buffer) throws NfcException {
        requestOrWakeupA(PiccCommand.WUPA, buffer);
    }

    private void requestOrWakeupA(PiccCommand command, byte[] buffer) throws NfcException {
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
            throw new NfcException("Received invalid response to REQA/WUPA command");
        }

        System.out.println(response);
    }

    private PiccResponse transceivePicc(byte[] txData, int txLastBits) throws NfcException {
        return transceivePicc(txData, txLastBits, 0);
    }

    private PiccResponse transceivePicc(byte[] txData, int txLastBits, int rxAlignBits) throws NfcException {
        final var waitIrq = Set.of(PcdComIrq.RX_IRQ, PcdComIrq.IDLE_IRQ);
        return sendPiccRequest(PcdCommand.TRANSCEIVE, waitIrq, txData, txLastBits, rxAlignBits);
    }

    private PiccResponse sendPiccRequest(PcdCommand command, Set<PcdComIrq> waitIrq, byte[] txData) throws NfcException {
        return sendPiccRequest(command, waitIrq, txData, 0, 0);
    }

    private PiccResponse sendPiccRequest(PcdCommand command, Set<PcdComIrq> waitIrq, byte[] txData, int txLastBits) throws NfcException {
        return sendPiccRequest(command, waitIrq, txData, txLastBits, 0);
    }

    private PiccResponse sendPiccRequest(PcdCommand command, Set<PcdComIrq> waitIrq, byte[] txData, int txLastBits, int rxAlignBits) throws NfcException {
        return sendPiccRequest(command, waitIrq, txData, txLastBits, 0, false);
    }

    private PiccResponse sendPiccRequest(PcdCommand command, Set<PcdComIrq> waitIrq, byte[] txData, int txLastBits, int rxAlignBits, boolean checkCrc) throws NfcException {
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
            if (waitIrq.stream().anyMatch(irq -> irq.isSet(comIrqReg)) || PcdComIrq.TIMER_IRQ.isSet(comIrqReg)) {
                deadlineReached = false;
                break;
            }
        } while (System.currentTimeMillis() < deadline);

        // Clear StartSend flag which may have been previously set
        clearBitMask(PcdRegister.BIT_FRAMING_REG, (byte) 0x80); // StartSend[0]

        // Handle potential timeout of PICC command execution
        if (deadlineReached) {
            throw new NfcTimeoutException("Deadline reached after " + PICC_COMMAND_TIMEOUT_MS + " milliseconds");
        }

        // Throw an exception if error register contains any unexpected error
        final byte errorReg = readRegister(PcdRegister.ERROR_REG);
        final var earlyError = PcdError.matchErrReg(errorReg, PcdError.BUFFER_OVFL, PcdError.PARITY_ERR, PcdError.PROTOCOL_ERR);
        if (earlyError != null) {
            throw new NfcException(earlyError);
        }

        // Receive data from PICC
        final var rxLength = readRegister(PcdRegister.FIFO_LEVEL_REG) & 0xFF;
        final var rxData = readRegister(PcdRegister.FIFO_DATA_REG, rxLength, rxAlignBits);
        final var rxLastBits = readRegister(PcdRegister.CONTROL_REG) & 0x07;

        // Check for collision error
        var lateError = PcdError.matchErrReg(errorReg, PcdError.COLL_ERR);
        if (lateError != null) {
            throw new NfcCollisionException();
        }

        // Perform CRC checks if enabled and response is not empty
        if (rxLength > 0 && checkCrc) {
            // NAK responses from MIFARE Classic PICCs are unsupported
            if (rxLength == 1 && rxLastBits == 4) {
                throw new NfcException("MIFARE lassic NAK can not be used with CRC checksum verification");
            }

            // At least two full CRC_A checksum bytes are required
            if (rxLength < 2 || rxLastBits != 0) {
                throw new NfcException("Receive buffer is too small for CRC checksum verification");
            }

            // Determine actual checksum and calculate expected checksum using CRC_A
            final var actualChecksum = new byte[]{rxData[rxLength - 2], rxData[rxLength - 1]};
            final var expectedChecksum = calculateCrc(Arrays.copyOfRange(rxData, 0, rxLength - 2));

            // Throw an exception if the checksums do not match
            if (!Arrays.equals(actualChecksum, expectedChecksum)) {
                throw new NfcException("CRC checksum mismatch during verification");
            }
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

    /**
     * Registers used for communicating with the PCD
     */
    @SuppressWarnings("SpellCheckingInspection")
    private enum PcdRegister {
        /**
         * starts and stops command execution
         */
        COMMAND_REG(0x01),
        /**
         * enable and disable interrupt request control bits
         */
        COM_I_EN_REG(0x02),
        /**
         * enable and disable interrupt request control bits
         */
        DIV_I_EN_REG(0x02),
        /**
         * interrupt request bits
         */
        COM_IRQ_REG(0x04),
        /**
         * interrupt request bits
         */
        DIV_IRQ_REG(0x05),
        /**
         * error bits showing the error status of the last command executed
         */
        ERROR_REG(0x06),
        /**
         * communication status bits
         */
        STATUS_1_REG(0x07),
        /**
         * receiver and transmitter status bits
         */
        STATUS_2_REG(0x08),
        /**
         * input and output of 64 byte FIFO buffer
         */
        FIFO_DATA_REG(0x09),
        /**
         * number of bytes stored in the FIFO buffer
         */
        FIFO_LEVEL_REG(0x0A),
        /**
         * level for FIFO underflow and overflow warning
         */
        WATER_LEVEL_REG(0x0B),
        /**
         * miscellaneous control registers
         */
        CONTROL_REG(0x0C),
        /**
         * adjustments for bit-oriented frames
         */
        BIT_FRAMING_REG(0x0D),
        /**
         * bit position of the first bit-collision detected on the RF interface
         */
        COLL_REG(0x0E),

        /**
         * defines general modes for transmitting and receiving
         */
        MODE_REG(0x11),
        /**
         * defines transmission data rate and framing
         */
        TX_MODE_REG(0x12),
        /**
         * defines reception data rate and framing
         */
        RX_MODE_REG(0x13),
        /**
         * controls the logical behavior of the antenna driver pins TX1 and TX2
         */
        TX_CONTROL_REG(0x14),
        /**
         * controls the setting of the transmission modulation
         */
        TX_ASK_REG(0x15),
        /**
         * selects the internal sources for the antenna driver
         */
        TX_SEL_REG(0x16),
        /**
         * selects internal receiver settings
         */
        RX_SEL_REG(0x17),
        /**
         * selects thresholds for the bit decoder
         */
        RX_THRESHOLD_REG(0x18),
        /**
         * defines demodulator settings
         */
        DEMOD_REG(0x19),
        /**
         * controls some MIFARE communication transmit parameters
         */
        MF_TX_REG(0x1C),
        /**
         * controls some MIFARE communication receive parameters
         */
        MF_RX_REG(0x1D),
        /**
         * selects the speed of the serial UART interface
         */
        SERIAL_SPEED_REG(0x1F),

        /**
         * shows the MSB value of the CRC calculation
         */
        CRC_RESULT_REG_HIGH(0x21),
        /**
         * shows the LSB value of the CRC calculation
         */
        CRC_RESULT_REG_LOW(0x22),
        /**
         * controls the ModWidth setting
         */
        MOD_WIDTH_REG(0x24),
        /**
         * configures the receiver gain
         */
        RF_CFG_REG(0x26),
        /**
         * selects the conductance of the antenna driver pins TX1 and TX2 for modulation
         */
        GS_N_REG(0x27),
        /**
         * defines the conductance of the p-driver output during periods of no modulation
         */
        CW_GS_P_REG(0x28),
        /**
         * defines the conductance of the p-driver output during periods of modulation
         */
        MOD_GS_P_REG(0x29),
        /**
         * defines settings for the internal timer
         */
        T_MODE_REG(0x2A),
        /**
         * defines settings for the internal timer
         */
        T_PRESCALER_REG(0x2B),
        /**
         * defines the MSB of the 16-bit timer reload value
         */
        T_RELOAD_REG_HIGH(0x2C),
        /**
         * defines the LSB of the 16-bit timer reload value
         */
        T_RELOAD_REG_LOW(0x2D),
        /**
         * shows the MSB of the 16-bit timer value
         */
        T_COUNTER_VAL_REG_HIGH(0x2E),
        /**
         * shows the LSB of the 16-bit timer value
         */
        T_COUNTER_VAL_REG_LOW(0x2F);

        private final byte value;
        private final byte writeAddress;
        private final byte readAddress;

        PcdRegister(int value) {
            this((byte) value);
        }

        PcdRegister(byte value) {
            this.value = value;
            this.writeAddress = (byte) ((value << 1) & 0x7E);
            this.readAddress = (byte) (writeAddress | 0x80);
        }

        public byte getValue() {
            return this.value;
        }

        public byte getReadAddress() {
            return readAddress;
        }

        public byte getWriteAddress() {
            return writeAddress;
        }
    }

    public enum PcdError {
        PROTOCOL_ERR(0x1, "Protocol error"),
        PARITY_ERR(0x2, "Parity check failed"),
        CRC_ERR(0x4, "CRC checksum mismatch"),
        COLL_ERR(0x8, "Bit-collision detected"),
        BUFFER_OVFL(0x10, "FIFO buffer overflow"),
        TEMP_ERR(0x40, "Temperature error due to overheating"),
        WR_ERR(0x80, "Illegal write error");

        private final byte value;
        private final String description;

        PcdError(int value, String description) {
            this((byte) value, description);
        }

        PcdError(byte value, String description) {
            this.value = value;
            this.description = description;
        }

        public static PcdError matchErrReg(byte errReg, PcdError... errors) {
            for (final var error : errors) {
                if (error.isSet(errReg)) {
                    return error;
                }
            }
            return null;
        }

        public byte getValue() {
            return this.value;
        }

        public String getDescription() {
            return this.description;
        }

        public boolean isSet(byte errReg) {
            return (errReg & getValue()) != 0;
        }

        public boolean isClear(byte errReg) {
            return !isSet(errReg);
        }
    }

    /**
     * COM IRQs used by the PCD
     */
    private enum PcdComIrq {
        /**
         * the timer decrements the timer value in register TCounterValReg to zero
         */
        TIMER_IRQ(0x1),
        /**
         * any error bit in the ErrorReg register is set
         */
        ERR_IRQ(0x2),
        /**
         * Status1Reg register’s LoAlert bit is set
         */
        LO_ALERT_IRQ(0x4),
        /**
         * the Status1Reg register’s HiAlert bit is set
         */
        HI_ALERT_IRQ(0x8),
        /**
         * if a command terminates, for example, when the CommandReg changes its value from any command to the Idle command.
         * if an unknown command is started, the CommandReg register Command[3:0] value changes to the idle state and the IdleIRq bit is set.
         * the microcontroller starting the Idle command does not set the IdleIRq bit.
         */
        IDLE_IRQ(0x10),
        /**
         * receiver has detected the end of a valid data stream
         */
        RX_IRQ(0x20),
        /**
         * set immediately after the last bit of the transmitted data was sent out
         */
        TX_IRQ(0x40);

        private final byte value;

        PcdComIrq(int value) {
            this((byte) value);
        }

        PcdComIrq(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }

        public boolean isSet(byte comIrqReg) {
            return (comIrqReg & getValue()) != 0;
        }

        public boolean isClear(byte comIrqReg) {
            return !isSet(comIrqReg);
        }
    }

    /**
     * DIV IRQs used by the PCD
     */
    private enum PcdDivIrq {
        /**
         * the CalcCRC command is active and all data is processed
         */
        CRC_IRQ(0x4),
        /**
         * MFIN is active
         * this interrupt is set when either a rising or falling signal edge is detected
         */
        MFIN_ACT_IRQ(0x10);

        private final byte value;

        PcdDivIrq(int value) {
            this((byte) value);
        }

        PcdDivIrq(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }

        public boolean isSet(byte divIrqReg) {
            return (divIrqReg & getValue()) != 0;
        }

        public boolean isClear(byte divIrqReg) {
            return !isSet(divIrqReg);
        }
    }

    /**
     * Commands used for communicating with the PCD
     */
    @SuppressWarnings("SpellCheckingInspection")
    private enum PcdCommand {
        /**
         * no action, cancels current command execution
         */
        IDLE(0b0000),
        /**
         * stores 25 bytes into the internal buffer
         */
        MEM(0b0001),
        /**
         * generates a 10-byte random ID number
         */
        GENERATE_RANDOM_ID(0b0010),
        /**
         * activates the CRC coprocessor or performs a self test
         */
        CALC_CRC(0b0011),
        /**
         * transmits data from the FIFO buffer
         */
        TRANSMIT(0b0100),
        /**
         * no command change, can be used to modify the CommandReg register bits without affecting the command, for example, the PowerDown bit
         */
        NO_CMD_CHANGE(0b0111),
        /**
         * activates the receiver circuits
         */
        RECEIVE(0b1000),
        /**
         * transmits data from FIFO buffer to antenna and automatically activates the receiver after transmission
         */
        TRANSCEIVE(0b1100),
        /**
         * performs the MIFARE standard authentication as a reader
         */
        MF_AUTHENT(0b1110),
        /**
         * resets the MFRC522
         */
        SOFT_RESET(0b1111);

        private final byte value;

        PcdCommand(int value) {
            this((byte) value);
        }

        PcdCommand(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    /**
     * Commands used for communicating with the PICC
     */
    @SuppressWarnings("SpellCheckingInspection")
    private enum PiccCommand {
        /**
         * REQuest command, Type A.
         * Invites PICCs in state IDLE to go to READY and prepare for anticollision or selection. 7 bit frame.
         */
        REQA(0x26),
        /**
         * Wake-UP command, Type A.
         * Invites PICCs in state IDLE and HALT to go to READY(*) and prepare for anticollision or selection. 7 bit frame.
         */
        WUPA(0x52),
        /**
         * Cascade Tag.
         * Not really a command, but used during anti collision.
         */
        CASCADE_TAG(0x88),
        /**
         * Anti collision/Select, Cascade Level 1.
         */
        SEL_CL1(0x93),
        /**
         * Anti collision/Select, Cascade Level 2.
         */
        SEL_CL2(0x95),
        /**
         * Anti collision/Select, Cascade Level 3.
         */
        SEL_CL3(0x97);

        private final byte value;

        PiccCommand(int value) {
            this.value = (byte) value;
        }

        PiccCommand(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    private enum PiccCascadeLevel {
        CL1(PiccCommand.SEL_CL1, 0, 4),
        CL2(PiccCommand.SEL_CL2, 3, 7),
        CL3(PiccCommand.SEL_CL3, 6);

        private final PiccCommand command;
        private final int uidIndex;
        private final int nextThreshold;

        PiccCascadeLevel(PiccCommand command, int uidIndex) {
            this(command, uidIndex, 0);
        }

        PiccCascadeLevel(PiccCommand command, int uidIndex, int nextThreshold) {
            this.command = command;
            this.uidIndex = uidIndex;
            this.nextThreshold = nextThreshold;
        }

        public PiccCascadeLevel getNext() {
            return PiccCascadeLevel.values()[ordinal() + 1];
        }

        public PiccCommand getCommand() {
            return command;
        }

        public int getUidOffset() {
            return uidIndex;
        }

        public int getNextThreshold() {
            return nextThreshold;
        }
    }

    public static final class PiccResponse {
        private final byte[] bytes;
        private final int length;
        private final int lastBits;

        public PiccResponse(byte[] bytes, int length, int lastBits) {
            this.bytes = bytes;
            this.length = length;
            this.lastBits = lastBits & 0x7;
        }

        public byte[] getBytes() {
            return this.bytes;
        }

        public int getLength() {
            return this.length;
        }

        public int getLastBits() {
            return this.lastBits;
        }

        @Override
        public String toString() {
            return "PiccResponse{" +
                "bytes=" + Arrays.toString(bytes) +
                ", length=" + length +
                ", lastBits=" + lastBits +
                '}';
        }
    }
}
