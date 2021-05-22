package com.pi4j.crowpi.components.internal.rfid;

/**
 * Helper class for authenticating against MIFARE cards using either Key A or Key B.
 * Provides {@link #getDefaultKeyA()} and {@link #getDefaultKeyB()} for retrieving the factory default keys (all FF).
 */
public final class MifareKey {
    /**
     * Type of this specific authentication key
     */
    private final Type type;
    /**
     * Contents of this key, must be exactly 6 bytes
     */
    private final byte[] bytes;

    /**
     * Creates a new MIFARE key which can be used for authenticating against a card.
     *
     * @param type  Type of authentication key
     * @param bytes Contents of key, must be exactly 6 bytes
     */
    public MifareKey(Type type, byte[] bytes) {
        if (bytes.length != 6) {
            throw new IllegalArgumentException("Length of key must be exactly 6 bytes");
        }

        this.type = type;
        this.bytes = bytes;
    }

    /**
     * Returns the default factory key (six times 0xFF) to be used as {@link MifareKey.Type#KEY_A}.
     *
     * @return Instance of default key A
     */
    public static MifareKey getDefaultKeyA() {
        return new MifareKey(Type.KEY_A, getDefaultKey());
    }

    /**
     * Returns the default factory key (six times 0xFF) to be used as {@link MifareKey.Type#KEY_B}.
     *
     * @return Instance of default key B
     */
    public static MifareKey getDefaultKeyB() {
        return new MifareKey(Type.KEY_B, getDefaultKey());
    }

    /**
     * Helper method which returns the contents of a factory default key, represented as six times 0xFF.
     *
     * @return Byte array with 6 bytes all set to 0xFF
     */
    private static byte[] getDefaultKey() {
        return new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    }

    /**
     * Returns the raw content / bytes for this key.
     *
     * @return Byte array of key contents
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Returns the length of this key, should always be 6.
     *
     * @return Length of key
     */
    public int getLength() {
        return bytes.length;
    }

    /**
     * Returns the type of this key.
     *
     * @return Type of key
     */
    public Type getType() {
        return type;
    }

    /**
     * All available key types on MIFARE cards
     */
    public enum Type {
        /**
         * KEY_A is the primary key which can be granted access to a sector
         */
        KEY_A(PiccCommand.MF_AUTH_KEY_A),
        /**
         * KEY_B is the secondary key which can be granted access to a sector
         */
        KEY_B(PiccCommand.MF_AUTH_KEY_B);

        private final PiccCommand command;

        Type(PiccCommand command) {
            this.command = command;
        }

        PiccCommand getCommand() {
            return command;
        }
    }
}
