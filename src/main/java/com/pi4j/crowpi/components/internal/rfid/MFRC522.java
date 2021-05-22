package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.Component;
import com.pi4j.crowpi.components.exceptions.RfidCollisionException;
import com.pi4j.crowpi.components.exceptions.RfidException;
import com.pi4j.crowpi.components.exceptions.RfidTimeoutException;
import com.pi4j.crowpi.components.exceptions.RfidUnsupportedCardException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Implementation of MFRC522 RFID Reader/Writer used for interacting with RFID cards.
 * Uses SPI via Pi4J for communication with the PCD (Proximity Coupling Device).
 * The official name for cards is PICC (Proximity Integrated Circuit Card).
 */
public class MFRC522 extends Component {
    /**
     * Pi4J digital output optionally used as reset pin for the MFRC522
     */
    protected final DigitalOutput resetPin;

    /**
     * Pi4J SPI instance
     */
    protected final Spi spi;

    /**
     * Timeout in milliseconds when calculating CRC_A checksums on the PCD
     */
    private static final long PCD_CHECKSUM_TIMEOUT_MS = 100;

    /**
     * Timeout in milliseconds for communication with a PICC
     */
    private static final long PICC_COMMAND_TIMEOUT_MS = 250;

    /**
     * Well-known value used by MIFARE PICCs as ACKnowledge response
     */
    private static final byte PICC_MIFARE_ACK = 0xA;

    /**
     * Creates a new MFRC522 instance without a reset pin for the given SPI instance from Pi4J.
     *
     * @param spi SPI instance
     */
    public MFRC522(Spi spi) {
        this(null, spi);
    }

    /**
     * Creates a new MFRC522 instance using the given reset pin and SPI instance from Pi4J.
     *
     * @param resetPin Digital output used as reset pin for MFRC522, high is considered as power-on, low as power-off
     * @param spi      SPI instance
     */
    public MFRC522(DigitalOutput resetPin, Spi spi) {
        this.resetPin = resetPin;
        this.spi = spi;
        this.reset();
    }

    /**
     * Resets the PCD into a well-known state and calls {@link #init()} to achieve a well-known state.
     * Internally this will either trigger a hard- or soft-reset depending on the current PCD condition.
     * After calling this method, the PCD will be ready for communication with PICCs.
     */
    public void reset() {
        resetSystem();
        resetTransmission();
        init();
    }

    /**
     * This will also setup the internal timer to use for timeout handling and enables the CRC coprocessor.
     * The antennas of the PCD will automatically be enabled as part of this routine.
     */
    private void init() {
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

    /**
     * Returns a boolean if at least one new PICC is in the proximity of the PCD.
     * This means that only PICCs in IDLE state are considered, meaning a REQA gets sent.
     *
     * @see #isCardPresent(boolean)
     */
    public boolean isNewCardPresent() {
        return isCardPresent(true);
    }

    /**
     * Returns a boolean if at least one PICC is in the proximity of the PCD.
     * This means that al PICCs in either IDLE or HALT state are considered, meaning a WUPA gets sent.
     *
     * @see #isCardPresent(boolean)
     */
    public boolean isAnyCardPresent() {
        return isCardPresent(false);
    }

    /**
     * Returns a boolean if at least one PICC is currently in the proximity of the PCD.
     * Any potential communication errors are silently ignored and result in true.
     * As anti-collision is handled later on, {@link RfidCollisionException} is treated as success.
     * <p>
     * The parameter {@code onlyNew} determines if PICCs in HALT state should be ignored (true) or not (false).
     * This allows for processing multiple cards one after another as the state of a PICC may differ:
     * - Any newly approached PICC defaults to IDLE state and would get picked up in either situation
     * - Any previously read PICC transitions to HALT state after calling {@link #uninitializeCard()}
     *
     * @param onlyNew True if only new PICCs (IDLE state) should be considered, otherwise false
     * @return True if PICC is near PCD, otherwise false
     */
    private boolean isCardPresent(boolean onlyNew) {
        resetTransmission();
        try {
            if (onlyNew) {
                requestA(new byte[2]);
            } else {
                wakeupA(new byte[2]);
            }
            return true;
        } catch (RfidCollisionException ignored) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Selects a single PICC and transitions it from READY to ACTIVE state, then returns an appropriate {@link RfidCard} instance.
     * Must only be called when a previous call to {@link #isNewCardPresent()} returned true.
     *
     * @return Card instance for further interaction
     * @throws RfidException Communication with PICC failed or card is unsupported
     */
    public RfidCard initializeCard() throws RfidException {
        final var cardUid = select();
        final var cardType = RfidCardType.fromSak(cardUid.getSak());

        //noinspection SwitchStatementWithTooFewBranches
        switch (cardType) {
            case MIFARE_CLASSIC_1K:
                return new Mifare1K(this, cardUid);
            default:
                throw new RfidUnsupportedCardException(cardType);
        }
    }

    /**
     * Uninitializes the currently active card by sending it back into HALT state and stopping encrypted communication.
     * Any previously created instance of {@link RfidCard} will be INVALID and must no longer be used after calling this method.
     */
    public void uninitializeCard() throws RfidException {
        haltA();
        mifareStopCrypto1();
    }

    /**
     * Enables or disables the TX1 and TX2 antennas required for powering the PICCs.
     * Must be called after each reset as antennas are disabled by default.
     *
     * @param on True to enable, false to disable antennas
     */
    protected void setAntennaState(boolean on) {
        if (on) {
            setBitMask(PcdRegister.TX_CONTROL_REG, (byte) 0x03); // Tx1RFEn[1], Tx2RFEn[1]
        } else {
            clearBitMask(PcdRegister.TX_CONTROL_REG, (byte) 0x03); // Tx1RFEn[0], Tx2RFEn[0]
        }
    }

    /**
     * Reset the transmission and modulation framing of the MFRC522 PCD.
     * This should always be called after a hard- or soft-reset.
     */
    private void resetTransmission() {
        // Reset baud rates
        writeRegister(PcdRegister.TX_MODE_REG, (byte) 0x00); // TxSpeed[000] = 106kBd
        writeRegister(PcdRegister.RX_MODE_REG, (byte) 0x00); // TxSpeed[000] = 106kBd

        // Reset modulation width
        writeRegister(PcdRegister.MOD_WIDTH_REG, (byte) 0x26); // ModWidth[0x26]
    }

    /**
     * Pulls the reset pin to HIGH to enable the PCD or alternatively triggers a soft-reset if already powered or not present.
     * This method will sleep a short amount of time (<0.2s usually) to ensure the PCD is ready.
     */
    private void resetSystem() {
        // If reset pin is present and LOW (= device is shutdown), set it to HIGH to leave power-down mode
        // Otherwise execute a soft-reset command on the PCD
        if (this.resetPin != null && this.resetPin.isLow()) {
            this.resetPin.high();
        } else {
            this.executePcd(PcdCommand.SOFT_RESET);
        }

        // Give the PCD some time to startup
        sleep(50);

        // Ensure that soft power-down mode is not active
        while ((readRegister(PcdRegister.COMMAND_REG) & (1 << 4)) != 0) {
            sleep(10);
        }

        // Deauthenticate from previous PICC, this is not covered by soft resets
        mifareStopCrypto1();
    }

    /**
     * Selects a single PICC by executing the ANTICOLLISION and SELECT procedure according to ISO 14443.
     * This method expects at least one PICC in READY state, which can be achieved using {@link #requestA(byte[])} or {@link #wakeupA(byte[])}.
     * <p>
     * Upon successful completion, a single PICC will now be in ACTIVE state and ready for communication.
     * All other PICCs will return to their IDLE or HALT state and no longer conflict with each other.
     * The UID of the targeted PICC will be returned which should be stored for further interaction.
     *
     * @return UID of PICC transitioned into ACTIVE state
     * @throws RfidException Unable to select PICC, e.g. timeout, missing presence, protocol error, ...
     */
    protected RfidCardUid select() throws RfidException {
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
            logger.debug("Processing UID cascade level {}", cascadeLevel);

            // Initialize for current cascade level and determine if cascade tag must be parsed
            buffer[0] = cascadeLevel.getCommand().getValue();
            final int uidOffset = cascadeLevel.getUidOffset();
            final boolean useCascadeTag = uidBytes.size() > cascadeLevel.getNextThreshold();

            // We do not know any bits of this cascade level yet
            int knownLevelBits = 0;

            // Add cascade tag command to buffer if needed
            if (useCascadeTag) {
                buffer[2] = PiccCommand.CASCADE_TAG.getValue();
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

                    // Print log message about SELECT request
                    logger.debug("Preparing SELECT request with payload {}", ByteHelpers.toString(buffer));
                } else {
                    // We do not know all bits in this cascade level and must treat this as ANTI COLLISION

                    // Calculate required size of transmit buffer
                    txFullBytes = (knownLevelBits / 8) + 2; // Calculate number of fully known bytes (+2 for SEL and NVB)
                    txExtraBits = knownLevelBits % 8; // Calculate number of extra bits

                    // Update NVB with calculated size
                    buffer[1] = (byte) (((txFullBytes & 0xF) << 4) + (txExtraBits & 0xF));

                    // Copy result into unused part of buffer (starts after previously filled bytes)
                    rxTargetOffset = txFullBytes;
                    rxBytes = buffer.length - txFullBytes;

                    // Print log message about ANTI COLLISION request
                    logger.debug("Preparing ANTI COLLISION request with payload {}", ByteHelpers.toString(buffer));
                }

                // Prepare request buffer based on required size for storing all full bytes and extra bits
                final int txBytes = txFullBytes + (txExtraBits != 0 ? 1 : 0);
                final var requestBuffer = Arrays.copyOfRange(buffer, 0, txBytes);

                // Try to receive UID for this cascade level from PICC
                try {
                    // Transceive request buffer to PCD and use same amount of TX/RX extra bits
                    final var response = transceivePicc(requestBuffer, txExtraBits, txExtraBits);

                    // Store buffer and length in result variables outside of this loop
                    responseBuffer = response.getBytes();
                    responseLength = response.getLength();

                    // Copy response back into buffer starting at previously determined offset
                    System.arraycopy(responseBuffer, 0, buffer, rxTargetOffset, Math.min(rxBytes, responseLength));
                    logger.debug("Received PICC response: {}", ByteHelpers.toString(responseBuffer));

                    // Determine how to continue
                    if (knownLevelBits >= 32) {
                        // This has been a SELECT, we can finalize processing
                        logger.debug("Handling SELECT response with {} known bits on this cascade level", knownLevelBits);

                        // Verify that SAK (Select Acknowledge) of response is valid
                        if (responseLength != 3 || txExtraBits != 0) {
                            throw new RfidException("Received invalid SAK from PICC, expected exactly 24 bits");
                        }

                        // Break out of this anti-collision loop
                        selectComplete = true;
                    } else {
                        // This has been an ANTI COLLISION, we must continue but now know all bits of this cascade level
                        logger.debug("Handling ANTI COLLISION response with {} known bits on this cascade level", knownLevelBits);
                        knownLevelBits = 32;
                    }
                } catch (RfidCollisionException ignored) {
                    logger.debug("Handling detected collision between multiple PICCs");

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
            logger.debug("SAK checksum: expected={} actual={}", ByteHelpers.toString(expectedChecksum), ByteHelpers.toString(actualChecksum));
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
        return new RfidCardUid(uidBytes, uidSak);
    }

    /**
     * Authenticates the sector to which the specified block belongs for MIFARE PICCs.
     * A valid key A or key B must be provided based on the required access privileges.
     * Do not forget to call {@link #mifareStopCrypto1()} once PICC communication is complete.
     *
     * @param key       Authentication key to use for this sector
     * @param blockAddr Block address for which sector gets authenticated
     * @param uid       UID of the PICC, required as part of the authentication
     * @throws RfidException Authentication against PICC has failed
     */
    protected void mifareAuth(MifareKey key, byte blockAddr, RfidCardUid uid) throws RfidException {
        // Prepare buffer for authentication command
        final byte[] buffer = new byte[12];
        buffer[0] = key.getType().getCommand().getValue();
        buffer[1] = blockAddr;
        System.arraycopy(key.getBytes(), 0, buffer, 2, Math.min(6, key.getLength()));
        System.arraycopy(uid.getUid(), uid.getUidLength() - 4, buffer, 8, Math.min(4, uid.getUidLength()));

        // Log authentication buffer
        logger.debug("Using MIFARE authentication against PICC with {}", ByteHelpers.toString(buffer));

        // Start authentication against PICC
        sendPiccRequest(PcdCommand.MF_AUTHENT, PcdComIrq.IDLE_IRQ, buffer);
    }

    /**
     * Stops the encrypted communication towards the PICC, must be called when {@link #mifareAuth(MifareKey, byte, RfidCardUid)} was used.
     * Without calling this method, communication with other PICCs is impossible aside from resetting the PCD.
     */
    protected void mifareStopCrypto1() {
        clearBitMask(PcdRegister.STATUS_2_REG, (byte) 0x08); // MFCrypto1On[0]
    }

    /**
     * Reads the specified block from a MIFARE PICC using {@link PiccCommand#MF_READ}.
     * The affected sector must be authenticated in advance using {@link #mifareAuth(MifareKey, byte, RfidCardUid)}.
     * WARNING: Not all block contain user-provided data, make sure to pay attention to this when deciding which blocks to read.
     *
     * @param blockAddr Block address to read from
     * @return Data read from PICC, length varies depending on type
     * @throws RfidException Reading data from specified block has failed
     */
    protected byte[] mifareRead(byte blockAddr) throws RfidException {
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

    /**
     * Writes the specified amount of data using {@link PiccCommand#MF_WRITE} to a MIFARE PICC.
     * The affected sector must be authenticated in advance using {@link #mifareAuth(MifareKey, byte, RfidCardUid)}.
     * WARNING: Overwriting critical blocks of data, e.g. the sector trailers, will PERMANENTLY brick the PICC.
     *
     * @param blockAddr  Block address to write to
     * @param dataBuffer Data buffer to write, must have correct length for used PICC
     * @throws RfidException Writing data to specified block has failed
     */
    protected void mifareWrite(byte blockAddr, byte[] dataBuffer) throws RfidException {
        // Generate command buffer for first step
        final var cmdBuffer = new byte[]{PiccCommand.MF_WRITE.getValue(), blockAddr};
        logger.debug("Executing first step for MIFARE write using {}", ByteHelpers.toString(cmdBuffer));
        mifareTransceive(cmdBuffer);

        // Transmit actual payload as second step
        logger.debug("Executing second step for MIFARE write using {}", ByteHelpers.toString(dataBuffer));
        mifareTransceive(dataBuffer);
    }

    /**
     * Transceives data to a MIFARE PICC by using CRC_A checksums and expecting a {@link #PICC_MIFARE_ACK} response.
     *
     * @param payload Data to be sent to the MIFARE PICC
     * @throws RfidException Invalid response from PICC or received MIFARE NACK
     */
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
        logger.debug("Response from MIFARE transceive: {}", ByteHelpers.toString(response.getBytes()));
        if (response.getBytes().length != 1 || response.getLastBits() != 4) {
            throw new RfidException("PICC response must be exactly 4 bits for MIFARE ACK");
        }
        if (response.getBytes()[0] != PICC_MIFARE_ACK) {
            throw new RfidException("Received MIFARE NACK from PICC due to unknown error");
        }
    }

    /**
     * Calculates the CRC_A checksum on the PCD for the given payload.
     * This operation is also subject to the default {@link #PCD_CHECKSUM_TIMEOUT_MS} timeout.
     *
     * @param data Payload for which checksum should be calculated
     * @return Calculated CRC_A checksum, exactly two bytes
     * @throws RfidTimeoutException Checksum operation timed out on PCD
     */
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

    /**
     * Sends a REQA command to the PICC to transition from IDLE into READY state.
     *
     * @see #requestOrWakeupA(PiccCommand, byte[])
     */
    private void requestA(byte[] buffer) throws RfidException {
        requestOrWakeupA(PiccCommand.REQA, buffer);
    }

    /**
     * Sends a WUPA command to the PICC to transition from HALT or IDLE into READY state.
     *
     * @see #requestOrWakeupA(PiccCommand, byte[])
     */
    private void wakeupA(byte[] buffer) throws RfidException {
        requestOrWakeupA(PiccCommand.WUPA, buffer);
    }

    /**
     * Sends a REQA or WUPA command to the PICC to start communication with a PICC.
     * REQA will transition a PICC from IDLE into READY state.
     * WUPA will transition a PICC in either HALT or IDLE into READY state.
     *
     * @param command Must be either {@link PiccCommand#REQA} or {@link PiccCommand#WUPA}
     * @param buffer  Buffer to transmit as part of the REQA/WUPA command, must be exactly two bytes
     * @throws RfidException Invalid response received from PICC
     */
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
    }

    /**
     * Halts the PICC which is currently in ACTIVE state
     *
     * @throws RfidException Halting of PICC failed
     */
    private void haltA() throws RfidException {
        // Construct payload and calculate CRC_A checksum
        final var payload = new byte[]{PiccCommand.HLTA.getValue(), 0};
        final var checksum = calculateCrc(payload);

        // Build buffer based on payload and CRC_A checksum
        final var buffer = new byte[payload.length + checksum.length];
        System.arraycopy(payload, 0, buffer, 0, payload.length);
        System.arraycopy(checksum, 0, buffer, payload.length, checksum.length);

        // Send the command to the PICC and expect a timeout
        // Funnily enough this is the only acceptable result, a valid response or any other error is considered as a failure
        try {
            transceivePicc(buffer);
            throw new RfidException("Could not halt currently active PICC");
        } catch (RfidTimeoutException e) {
            // Do nothing here, this is what we expect as a timeout signals success
        }
    }

    /**
     * Transceives data to the PICC expecting no partial bytes in either direction with CRC_A checksum processing disabled by default.
     *
     * @see #transceivePicc(byte[], int, int, boolean)
     */
    private PiccResponse transceivePicc(byte[] txData) throws RfidException {
        return transceivePicc(txData, 0);
    }

    /**
     * Transceives data to the PICC expecting a reply with no partial bytes with CRC_A checksum processing disabled by default.
     *
     * @see #transceivePicc(byte[], int, int, boolean)
     */
    private PiccResponse transceivePicc(byte[] txData, int txLastBits) throws RfidException {
        return transceivePicc(txData, txLastBits, 0);
    }

    /**
     * Transceives data to the PICC with CRC_A checksum processing disabled by default.
     *
     * @see #transceivePicc(byte[], int, int, boolean)
     */
    private PiccResponse transceivePicc(byte[] txData, int txLastBits, int rxAlignBits) throws RfidException {
        return transceivePicc(txData, txLastBits, rxAlignBits, false);
    }

    /**
     * Transmits data to the PICC and receives the resulting data, also known as transceiving.
     * Uses the PCD command {@link PcdCommand#TRANSCEIVE} for interacting with the PICC.
     *
     * @param txData      Data to be transmitted to the PICC
     * @param txLastBits  Number of valid bits to be transmitted as part of the last byte, 0 means all 8 bits are valid
     * @param rxAlignBits Position of first valid bit in received data, 0 means all 8 bits are valid
     * @param checkCrc    Boolean which specifies if CRC_A checksum according to ISO-14443 should be processed
     * @return Response from PICC with result data
     * @throws RfidException Timeout, collision or generic error occurred during PICC request
     */
    private PiccResponse transceivePicc(byte[] txData, int txLastBits, int rxAlignBits, boolean checkCrc) throws RfidException {
        final var waitIrq = Set.of(PcdComIrq.RX_IRQ, PcdComIrq.IDLE_IRQ);
        return sendPiccRequest(PcdCommand.TRANSCEIVE, waitIrq, txData, txLastBits, rxAlignBits, checkCrc);
    }

    /**
     * Sends a request to the PICC with a single wait IRQ and CRC_A checksum processing disabled by default.
     *
     * @see #sendPiccRequest(PcdCommand, Set, byte[], int, int, boolean)
     */
    private PiccResponse sendPiccRequest(PcdCommand command, PcdComIrq waitIrq, byte[] txData) throws RfidException {
        return sendPiccRequest(command, Set.of(waitIrq), txData);
    }

    /**
     * Sends a request to the PICC with CRC_A checksum processing disabled by default.
     *
     * @see #sendPiccRequest(PcdCommand, Set, byte[], int, int, boolean)
     */
    private PiccResponse sendPiccRequest(PcdCommand command, Set<PcdComIrq> waitIrq, byte[] txData) throws RfidException {
        return sendPiccRequest(command, waitIrq, txData, 0, 0, false);
    }

    /**
     * Sends a request to the PICC, waits for an appropriate response and processes it accordingly.
     * Common error IRQs are automatically handled and a timeout of {@link #PICC_COMMAND_TIMEOUT_MS} applies.
     *
     * @param command     PCD command to use which will trigger PICC communication
     * @param waitIrq     IRQs to consider as an indicator for a valid response
     * @param txData      Bytes used as data for the PCD command, effectively influencing what is sent to the PICC
     * @param txLastBits  Number of valid bits to be transmitted as part of the last byte, 0 means all 8 bits are valid
     * @param rxAlignBits Position of first valid bit in received data, 0 means all 8 bits are valid
     * @param checkCrc    Boolean which specifies if CRC_A checksum according to ISO-14443 should be processed
     * @return Response from PICC with payload
     * @throws RfidException Timeout, collision or generic error occurred during PICC request
     */
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

    /**
     * Executes the specified command on the PCD by writing to {@link PcdRegister#COMMAND_REG}.
     *
     * @param command Command to execute.
     */
    private void executePcd(PcdCommand command) {
        writeRegister(PcdRegister.COMMAND_REG, command.getValue());
    }

    /**
     * Writes a single byte to the specified PCD register.
     *
     * @param register PCD register to write
     * @param value    Byte to be written
     */
    private void writeRegister(PcdRegister register, byte value) {
        spi.transfer(new byte[]{register.getWriteAddress(), value});
    }

    /**
     * Writes one or more bytes to the specified PCD register.
     *
     * @param register PCD register to write
     * @param values   Bytes to be written
     */
    private void writeRegister(PcdRegister register, byte[] values) {
        final var buffer = new byte[values.length + 1];
        buffer[0] = register.getWriteAddress();
        System.arraycopy(values, 0, buffer, 1, values.length);

        spi.transfer(buffer);
    }

    /**
     * Writes a short to the specified PCD registers by splitting into two bytes.
     *
     * @param registerHigh PCD register where upper half (MSB) is stored
     * @param registerLow  PCD register where lower half (LSB) is stored
     * @param value        Short to be written to registers
     */
    private void writeRegister(PcdRegister registerHigh, PcdRegister registerLow, short value) {
        int tmp = value & 0xFFFF;
        writeRegister(registerHigh, (byte) ((tmp >> 8) & 0xFF));
        writeRegister(registerLow, (byte) (tmp & 0xFF));
    }

    /**
     * Reads a single byte from the specified PCD register.
     *
     * @param register PCD register to read
     * @return Byte read from register
     */
    private byte readRegister(PcdRegister register) {
        final var buffer = new byte[]{register.getReadAddress(), 0};
        spi.transfer(buffer);
        return buffer[1];
    }

    /**
     * Reads the specified amount of bytes from the specified PCD register.
     * Supports bit-oriented frames where the first relevant bit is shifted accordingly.
     *
     * @param register    PCD register to read
     * @param length      Amount of bytes to read from the PCD including the partial byte (only applicable if rxAlignBits != 0)
     * @param rxAlignBits Position of first bit which is relevant, specify 0 to consider all 8 bits valid
     * @return Byte array with retrieved data
     */
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

    /**
     * Manipulates the specified PCD register by setting all bits according to the bitmask
     *
     * @param register PCD register to manipulate
     * @param mask     Bitmask to set
     */
    private void setBitMask(PcdRegister register, byte mask) {
        final byte oldValue = readRegister(register);
        final byte newValue = (byte) (oldValue | mask);
        if (oldValue != newValue) {
            writeRegister(register, (byte) (oldValue | mask));
        }
    }

    /**
     * Manipulates the specified PCD register by clearing all bits according to the bitmask
     *
     * @param register PCD register to manipulate
     * @param mask     Bitmask to clear
     */
    private void clearBitMask(PcdRegister register, byte mask) {
        final byte oldValue = readRegister(register);
        final byte newValue = (byte) (oldValue & ~mask);
        if (oldValue != newValue) {
            writeRegister(register, (byte) (oldValue & ~mask));
        }
    }
}
