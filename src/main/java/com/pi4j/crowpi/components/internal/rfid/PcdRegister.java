package com.pi4j.crowpi.components.internal.rfid;

/**
 * Registers used for communicating with the PCD
 */
@SuppressWarnings("SpellCheckingInspection")
enum PcdRegister {
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

    /**
     * Pre-calculates the read and write address for this register and stores them.
     * To differentiate between a read and a write, reads have the MSB set, whereas writes have the MSB clear.
     * While this could be calculated on-the-fly, these values are used so often that it makes sense to cache them.
     *
     * @param value Raw address of register with MSB unset, used for calculating R/W addresses
     */
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
