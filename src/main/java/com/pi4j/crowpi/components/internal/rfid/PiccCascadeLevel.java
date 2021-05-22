package com.pi4j.crowpi.components.internal.rfid;

enum PiccCascadeLevel {
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
