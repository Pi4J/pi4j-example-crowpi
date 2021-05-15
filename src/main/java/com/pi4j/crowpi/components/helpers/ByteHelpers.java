package com.pi4j.crowpi.components.helpers;

import java.util.List;

public class ByteHelpers {
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    public static byte[] toArray(List<Byte> values) {
        final var result = new byte[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    public static String toString(byte value) {
        return "" + HEX_CHARS[(value >>> 4) & 0xF] + HEX_CHARS[value & 0xF];
    }

    public static String toString(byte[] bytes) {
        if (bytes == null) {
            return "<null>";
        }

        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            chars[i * 2] = HEX_CHARS[(value >>> 4) & 0xF];
            chars[i * 2 + 1] = HEX_CHARS[value & 0xF];
        }
        return new String(chars);
    }
}
