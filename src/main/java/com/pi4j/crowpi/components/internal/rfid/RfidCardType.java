package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.helpers.ByteHelpers;

/**
 * Known RFID card types based on SAK detection
 */
public enum RfidCardType {
    NOT_COMPLETE(0x04),
    MIFARE_MINI(0x09),
    MIFARE_CLASSIC_1K(0x08),
    MIFARE_CLASSIC_4K(0x18),
    MIFARE_ULTRALIGHT(0x00),
    MIFARE_PLUS_1(0x10),
    MIFARE_PLUS_2(0x11),
    TNP3XXX(0x01),
    ISO_14443_4(0x20),
    ISO_18092(0x40);

    private final byte sak;

    RfidCardType(int sak) {
        this((byte) sak);
    }

    RfidCardType(byte sak) {
        this.sak = sak;
    }

    public byte getSak() {
        return sak;
    }

    /**
     * Returns the detected card type based on SAK.
     *
     * @param sak SAK byte to be analyzed
     * @return Detected card type
     * @throws IllegalArgumentException Unknown SAK byte
     */
    public static RfidCardType fromSak(byte sak) {
        for (final var type : RfidCardType.values()) {
            if (type.getSak() == sak) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SAK value: " + ByteHelpers.toString(sak));
    }
}
