package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.helpers.ByteHelpers;

final class PiccResponse {
    private final byte[] bytes;
    private final int length;
    private final int lastBits;

    public PiccResponse(byte[] bytes, int length, int lastBits) {
        // Ensure provided response has same length as expected length
        if (bytes.length != length) {
            throw new IllegalArgumentException("Provided response buffer does not match expected length");
        }

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
        return "PiccResponse(" + length + "," + lastBits + ")[" + ByteHelpers.toString(bytes) + "]";
    }
}
