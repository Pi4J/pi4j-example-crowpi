package com.pi4j.crowpi.components.internal.rfid;

/**
 * Cascade levels used during SELECT and ANTICOLLISION process with PICCs
 */
enum PiccCascadeLevel {
    /**
     * Cascade Level 1, used by MIFARE Classic with 4 byte UIDs
     */
    CL1(PiccCommand.SEL_CL1, 0, 4),
    /**
     * Cascade Level 2, used by MIFARE Ultralight with 7 byte UIDs
     */
    CL2(PiccCommand.SEL_CL2, 3, 7),
    /**
     * Cascade Level 3, seems currently unused, 10 byte UIDs
     */
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

    /**
     * Returns the next higher cascade level or throws {@link IndexOutOfBoundsException} if already the highest cascade level.
     *
     * @return Next cascade level after the current one
     */
    public PiccCascadeLevel getNext() {
        return PiccCascadeLevel.values()[ordinal() + 1];
    }

    public PiccCommand getCommand() {
        return command;
    }

    /**
     * Returns the offset in bytes to indicate where this response should be stored as part of the UID
     *
     * @return Offset in bytes
     */
    public int getUidOffset() {
        return uidIndex;
    }

    /**
     * Returns the threshold in bytes before the next cascade level must be used
     *
     * @return Threshold in bytes
     */
    public int getNextThreshold() {
        return nextThreshold;
    }
}
