package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.helpers.ByteHelpers;

import java.util.List;


public final class RfidUid {
    private final byte[] uid;
    private final byte sak;
    private final String serial;

    public RfidUid(byte[] uid, byte sak) {
        this.uid = uid;
        this.sak = sak;
        this.serial = ByteHelpers.toString(uid);
    }

    public RfidUid(List<Byte> uidBytes, byte sak) {
        this(ByteHelpers.toArray(uidBytes), sak);
    }

    public byte[] getUid() {
        return uid;
    }

    public int getUidLength() {
        return uid.length;
    }

    public byte getSak() {
        return sak;
    }

    public String getSerial() {
        return serial;
    }

    @Override
    public String toString() {
        return "Tag{uid=" + serial + ", sak=" + ByteHelpers.toString(sak) + "}";
    }
}
