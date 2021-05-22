package com.pi4j.crowpi.components.internal.rfid;

/**
 * Commands used for communicating with the PCD
 */
@SuppressWarnings("SpellCheckingInspection")
enum PcdCommand {
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
