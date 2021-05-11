package com.pi4j.crowpi.components.internal;

import com.pi4j.crowpi.components.Component;
import com.pi4j.crowpi.components.exceptions.NfcCommunicationException;
import com.pi4j.crowpi.components.exceptions.NfcException;
import com.pi4j.crowpi.components.exceptions.NfcTimeoutException;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.spi.Spi;

import java.util.Arrays;
import java.util.Set;

public class MFRC522 extends Component {
    private final DigitalOutput resetPin;
    private final Spi spi;

    private static final long PICC_COMMAND_TIMEOUT_MS = 250;

    public MFRC522(DigitalOutput resetPin, Spi spi) {
        this.resetPin = resetPin;
        this.spi = spi;
        this.init();
    }

    private void init() {
        reset();

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

    private void reset() {
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

    public void requestA(byte[] buffer) throws NfcException {
        requestOrWakeupA(PiccCommand.REQA, buffer);
    }

    public void wakeupA(byte[] buffer) throws NfcException {
        requestOrWakeupA(PiccCommand.WUPA, buffer);
    }

    protected void requestOrWakeupA(PiccCommand command, byte[] buffer) throws NfcException {
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
            throw new NfcCommunicationException("Received invalid response to REQA/WUPA command");
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
        var deadlineReached = true;
        final var deadline = System.currentTimeMillis() + PICC_COMMAND_TIMEOUT_MS;
        do {
            byte comIrqReg = readRegister(PcdRegister.COM_IRQ_REG);
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
            throw new NfcCommunicationException(earlyError);
        }

        // Receive data from PICC
        final var rxLength = readRegister(PcdRegister.FIFO_LEVEL_REG) & 0xFF;
        final var rxData = readRegister(PcdRegister.FIFO_DATA_REG, rxLength, rxAlignBits);
        final var rxLastBits = readRegister(PcdRegister.CONTROL_REG) & 0x07;

        // Check for collision error
        var lateError = PcdError.matchErrReg(errorReg, PcdError.COLL_ERR);
        if (lateError != null) {
            throw new NfcCommunicationException(lateError);
        }

        // TODO: Perform CRC checks if desired

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

        // Prepare result buffer and initialize counters
        final var result = new byte[length];
        int bufferIndex = 0;
        int resultIndex = 0;

        // Adjust first byte for bit-oriented frames
        if (rxAlignBits != 0) {
            // Create bitmask where LSB is shifted by given amount
            byte mask = (byte) ((0xFF << rxAlignBits) & 0xFF);
            // Mask received first byte and store into result buffer
            result[resultIndex++] = (byte) (buffer[bufferIndex++] & ~mask);
        }

        // Copy all pending bytes into the result buffer
        System.arraycopy(buffer, bufferIndex, result, resultIndex, length - bufferIndex);

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
     * IRQs used by the PCD
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
        WUPA(0x52);

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
