package com.pi4j.crowpi.components.internal.rfid;

final class MifareKey {
    private final Type type;
    private final byte[] bytes;

    public MifareKey(Type type, byte[] bytes) {
        if (bytes.length != 6) {
            throw new IllegalArgumentException("Length of key must be exactly 6 bytes");
        }

        this.type = type;
        this.bytes = bytes;
    }

    public static MifareKey getDefaultKeyA() {
        return new MifareKey(Type.KEY_A, getDefaultKey());
    }

    public static MifareKey getDefaultKeyB() {
        return new MifareKey(Type.KEY_B, getDefaultKey());
    }

    private static byte[] getDefaultKey() {
        return new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getLength() {
        return bytes.length;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        KEY_A(PiccCommand.MF_AUTH_KEY_A),
        KEY_B(PiccCommand.MF_AUTH_KEY_B);

        private final PiccCommand command;

        Type(PiccCommand command) {
            this.command = command;
        }

        public PiccCommand getCommand() {
            return command;
        }
    }
}
