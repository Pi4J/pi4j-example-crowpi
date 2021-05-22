package com.pi4j.crowpi.components.internal.rfid;

/**
 * COM IRQs used by the PCD
 */
enum PcdComIrq {
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

    /**
     * Helper method to define if the specified COM IRQ register (previously read elsewhere) has this specific IRQ set.
     *
     * @param comIrqReg Value of COM IRQ register
     * @return True if set, false if unset
     */
    public boolean isSet(byte comIrqReg) {
        return (comIrqReg & getValue()) != 0;
    }
}
