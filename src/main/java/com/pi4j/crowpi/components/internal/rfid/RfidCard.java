package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.exceptions.RfidException;
import com.pi4j.crowpi.components.helpers.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Abstract base class to be implemented by all supported RFID cards.
 * Provides various helper methods to abstract away internal details of various PICC types.
 */
public abstract class RfidCard {
    /**
     * Determined UID of this card
     */
    private final RfidCardUid uid;

    /**
     * Logger instance
     */
    protected final Logger logger = new Logger(this.getClass());

    /**
     * Creates a new RFID card instance for the given PICC UID.
     *
     * @param uid UID of PICC
     */
    public RfidCard(RfidCardUid uid) {
        this.uid = uid;
    }

    /**
     * Returns the human-readable serial of this card, based on the UID.
     *
     * @return Human-readable card serial
     */
    public String getSerial() {
        return uid.getSerial();
    }

    /**
     * Stores a single Java object onto the card by serializing the passed object and writing a GZIP-compressed byte stream.
     * While this method may be called multiple times, only a single object can be stored at once on the card.
     * Please note that the capacity of RFID cards is limited, so this command might fail if the given object is too large.
     *
     * @param data Serializable object to be stored on card
     * @throws RfidException Object serialization or write process failed
     */
    public synchronized void writeObject(Object data) throws RfidException {
        // Serialize object into compressed byte stream
        // All exceptions will be converted into RfidException for simplicity
        final var byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            final var gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            final var objectStream = new ObjectOutputStream(gzipOutputStream);
            objectStream.writeObject(data);
            objectStream.close();
            gzipOutputStream.finish();
        } catch (Exception e) {
            throw new RfidException("Could not write object to card: " + e.getMessage(), e);
        }

        // Write serialized bytes to card
        logger.debug("Writing object with {} bytes to card", byteArrayOutputStream.size());
        writeBytes(byteArrayOutputStream.toByteArray());
    }

    /**
     * Reads a single Java object from the card without casting the result into a more-specific type.
     *
     * @see #readObject(Class)
     */
    public synchronized Object readObject() throws RfidException {
        return readObject(Object.class);
    }

    /**
     * Reads a single Java object from the card by deserializing a GZIP-compressed byte stream previously written onto the card.
     * This method can only read a single object from the card, so calling it multiple times results in the same value every time.
     * The deserialized object is automatically casted into the given {@code type} to simplify usage.
     *
     * @param type Class instance of target type for deserialized object
     * @param <T>  Target type for deserialized object, determined by {@code type} parameter.
     * @return Deserialized object read from card
     * @throws RfidException Object deserialization or read process failed
     */
    public synchronized <T> T readObject(Class<T> type) throws RfidException {
        // Read all available bytes from tag
        final var data = readBytes();

        // Deserialize compressed byte stream into object
        // All exceptions will be converted into RfidException for simplicity
        final Object object;
        try {
            final var byteArrayInputStream = new ByteArrayInputStream(data);
            final var gzipInputStream = new GZIPInputStream(byteArrayInputStream);
            final var objectStream = new ObjectInputStream(gzipInputStream);
            object = objectStream.readObject();
        } catch (Exception e) {
            throw new RfidException("Could not read object from card: " + e.getMessage(), e);
        }

        // Attempt to cast object into specified target type
        // Failure to do so will be casted into RfidException for simplicity
        try {
            return type.cast(object);
        } catch (ClassCastException e) {
            throw new RfidException("Could not read object with unexpected type [" + object.getClass().getCanonicalName() + "] from card");
        }
    }

    /**
     * Returns the UID of this card.
     *
     * @return Card/PICC UID
     */
    protected RfidCardUid getUid() {
        return uid;
    }

    /**
     * Returns the maximum capacity in bytes this card can store.
     * This method should already subtract all unavailable blocks / sectors, e.g. trailer blocks.
     *
     * @return Maximum capacity in bytes
     */
    public abstract int getCapacity();

    /**
     * Reads all available blocks from the card and returns a byte array.
     * This method targets the same blocks as {@link #writeBytes(byte[])} does.
     *
     * @return Byte array of available data on card
     * @throws RfidException Reading data from card failed
     */
    protected abstract byte[] readBytes() throws RfidException;

    /**
     * Writes the given data to the card, using as many blocks as needed.
     * This method will only write to safe blocks and does not overwrite any internal blocks.
     *
     * @param data Data to write to the card
     * @throws RfidException            Writing data to card failed
     * @throws IllegalArgumentException Given data exceeds card capacity
     */
    protected abstract void writeBytes(byte[] data) throws RfidException, IllegalArgumentException;
}
