package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.exceptions.RfidException;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class RfidCard {
    private final RfidUid uid;

    public RfidCard(RfidUid uid) {
        this.uid = uid;
    }

    public String getSerial() {
        return uid.getSerial();
    }

    public synchronized void writeObject(Object data) throws IOException, RfidException {
        // Serialize object into compressed byte stream
        final var byteArrayOutputStream = new ByteArrayOutputStream();
        final var gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        final var objectStream = new ObjectOutputStream(gzipOutputStream);
        objectStream.writeObject(data);
        objectStream.close();
        gzipOutputStream.finish();

        System.out.println("!!! Writing " + byteArrayOutputStream.size() + " bytes to card");

        // Write serialized bytes to card
        writeBytes(byteArrayOutputStream.toByteArray());
    }

    public synchronized Object readObject() throws IOException, ClassNotFoundException, RfidException {
        return readObject(Object.class);
    }

    public synchronized <T> T readObject(Class<T> type) throws IOException, ClassNotFoundException, RfidException {
        // Read all available bytes from tag
        final var data = readBytes();

        // Deserialize compressed byte stream into object
        final var byteArrayInputStream = new ByteArrayInputStream(data);
        final var gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        final var objectStream = new ObjectInputStream(gzipInputStream);
        final var object = objectStream.readObject();

        return type.cast(object);
    }

    protected RfidUid getUid() {
        return uid;
    }

    public abstract int getCapacity();

    protected abstract byte[] readBytes() throws RfidException;

    protected abstract void writeBytes(byte[] data) throws RfidException;
}
