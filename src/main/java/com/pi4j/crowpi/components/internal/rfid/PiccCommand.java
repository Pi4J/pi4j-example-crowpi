package com.pi4j.crowpi.components.internal.rfid;

/**
 * Commands used for communicating with the PICC
 */
@SuppressWarnings("SpellCheckingInspection")
enum PiccCommand {
    /**
     * REQuest command, Type A.
     * Invites PICCs in state IDLE to go to READY and prepare for anticollision or selection. 7 bit frame.
     */
    REQA(0x26),
    /**
     * HaLT command, Type A.
     * Instructs an ACTIVE PICC to go to state HALT.
     */
    HLTA(0x50),
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
    SEL_CL3(0x97),

    /**
     * [MIFARE Classic/Ultralight] Reads one 16 byte block from the authenticated sector of the PICC.
     * Also used for MIFARE Ultralight.
     */
    MF_READ(0x30),
    /**
     * [MIFARE Classic/Ultralight] Writes one 16 byte block to the authenticated sector of the PICC.
     * Called "COMPATIBILITY WRITE" for MIFARE Ultralight.
     */
    MF_WRITE(0xA0),
    /**
     * [MIFARE Classic] Perform authentication with Key A
     */
    MF_AUTH_KEY_A(0x60),
    /**
     * [MIFARE Classic] Perform authentication with Key B
     */
    MF_AUTH_KEY_B(0x61);

    private final byte value;

    PiccCommand(int value) {
        this((byte) value);
    }

    PiccCommand(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }
}
