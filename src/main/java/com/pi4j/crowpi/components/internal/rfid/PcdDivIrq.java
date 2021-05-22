package com.pi4j.crowpi.components.internal.rfid;

/**
 * DIV IRQs used by the PCD
 */
enum PcdDivIrq {
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

    /**
     * Helper method to define if the specified DIV IRQ register (previously read elsewhere) has this specific IRQ set.
     *
     * @param divIrqReg Value of DIV IRQ register
     * @return True if set, false if unset
     */
    public boolean isSet(byte divIrqReg) {
        return (divIrqReg & getValue()) != 0;
    }
}
