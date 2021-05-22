package com.pi4j.crowpi.components.internal.rfid;

import java.util.Arrays;

final class PiccResponse {
    private final byte[] bytes;
    private final int length;
    private final int lastBits;

    public PiccResponse(byte[] bytes, int length, int lastBits) {
        this.bytes = bytes;
        this.length = length;
        this.lastBits = lastBits & 0x7;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public int getLength() {
        return this.length;
    }

    public int getLastBits() {
        return this.lastBits;
    }

    @Override
    public String toString() {
        return "PiccResponse{" +
            "bytes=" + Arrays.toString(bytes) +
            ", length=" + length +
            ", lastBits=" + lastBits +
            '}';
    }
}
