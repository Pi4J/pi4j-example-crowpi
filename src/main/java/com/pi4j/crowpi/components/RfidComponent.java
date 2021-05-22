package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.exceptions.RfidException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;
import com.pi4j.crowpi.components.internal.MFRC522;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RfidComponent extends MFRC522 {
    protected static final int DEFAULT_POWER_PIN = 25;
    protected static final int DEFAULT_SPI_CHANNEL = 0;
    protected static final int DEFAULT_SPI_BAUD_RATE = 1000000;

    public RfidComponent(Context pi4j) {
        this(pi4j, DEFAULT_POWER_PIN, DEFAULT_SPI_CHANNEL, DEFAULT_SPI_BAUD_RATE);
    }

    public RfidComponent(Context pi4j, int gpioPowerPin, int spiChannel, int spiBaud) {
        super(
            pi4j.create(buildDigitalOutputConfig(pi4j, gpioPowerPin)),
            pi4j.create(buildSpiConfig(pi4j, spiChannel, spiBaud))
        );
    }

    public Card initializeCard() throws RfidException {
        final var tag = select();

        if (tag.getSak() == 0x08) {
            return new Mifare1K(tag);
        } else {
            throw new RfidException("Unsupported SAK " + ByteHelpers.toString(tag.getSak()) + ", only MIFARE Classic 1K is supported");
        }
    }

    private static DigitalOutputConfig buildDigitalOutputConfig(Context pi4j, int address) {
        return DigitalOutput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("RFID Reset Pin")
            .address(address)
            .initial(DigitalState.LOW)
            .shutdown(DigitalState.LOW)
            .build();
    }

    private static SpiConfig buildSpiConfig(Context pi4j, int channel, int baud) {
        return Spi.newConfigBuilder(pi4j)
            .id("SPI" + channel)
            .name("RFID SPI")
            .address(channel)
            .baud(baud)
            .build();
    }

    public abstract static class Card {
        private final Tag tag;

        public Card(Tag tag) {
            this.tag = tag;
        }

        public String getSerial() {
            return tag.getSerial();
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

        protected Tag getTag() {
            return tag;
        }

        public abstract int getCapacity();

        protected abstract byte[] readBytes() throws RfidException;

        protected abstract void writeBytes(byte[] data) throws RfidException;
    }

    public final class Mifare1K extends Card {
        private static final byte SECTOR_COUNT = 16;
        private static final byte BLOCKS_PER_SECTOR = 4;
        private static final byte BYTES_PER_BLOCK = 16;
        private static final byte BLOCK_COUNT = SECTOR_COUNT * BLOCKS_PER_SECTOR;

        private final Set<Integer> forbiddenBlocks;
        private final int totalCapacity;
        private int lastAuthedSector = -1;

        public Mifare1K(Tag tag) {
            super(tag);
            this.forbiddenBlocks = determineForbiddenBlocks();
            this.totalCapacity = (BLOCK_COUNT - forbiddenBlocks.size()) * BYTES_PER_BLOCK;
            System.out.println(this.forbiddenBlocks);
        }

        public int getCapacity() {
            return totalCapacity;
        }

        @Override
        protected byte[] readBytes() throws RfidException {
            final var buffer = new ByteArrayOutputStream();

            // Read all available blocks from card
            for (int block = 0; block < BLOCK_COUNT; block++) {
                // Skip block if marked as forbidden
                if (forbiddenBlocks.contains(block)) {
                    System.out.println("Skipping forbidden block " + block);
                    continue;
                }

                // Read block from card into buffer
                authenticate(block);
                buffer.writeBytes(mifareRead((byte) block));
            }

            return buffer.toByteArray();
        }

        @Override
        protected void writeBytes(byte[] data) throws RfidException {
            // Ensure data fits within capacity
            if (data.length > getCapacity()) {
                throw new IllegalArgumentException("Unable to store data with " + data.length + " bytes, maximum capacity is " + getCapacity() + " bytes");
            }

            // Split byte array into list of chunks with block size
            final var chunks = new ArrayDeque<byte[]>();
            int start = 0;
            while (start < data.length) {
                final int end = Math.min(data.length, start + BYTES_PER_BLOCK);
                final var chunk = new byte[BYTES_PER_BLOCK];
                System.arraycopy(data, start, chunk, 0, end - start);
                chunks.add(chunk);
                start += BYTES_PER_BLOCK;
            }

            // Write chunks into available blocks
            int blockAddr = 0;
            while (blockAddr < BLOCK_COUNT && !chunks.isEmpty()) {
                // Write next chunk into block if not forbidden
                if (!forbiddenBlocks.contains(blockAddr)) {
                    final var chunk = chunks.pop();

                    System.out.println("Writing chunk " + ByteHelpers.toString(chunk) + " (" + chunk.length + " bytes) to block " + blockAddr);
                    authenticate(blockAddr);
                    mifareWrite((byte) blockAddr, chunk);
                }

                // Advance to next block
                blockAddr++;
            }

            // Throw an exception if not all chunks were written
            if (!chunks.isEmpty()) {
                throw new RfidException("Could not write all data into card, data will most likely be corrupted");
            }
        }

        private Set<Integer> determineForbiddenBlocks() {
            // Initialize list of forbidden blocks
            final var forbiddenBlocks = new HashSet<Integer>();

            // Avoid first sector which contains manufacturer block
            for (int block = 0; block < BLOCKS_PER_SECTOR; block++) {
                forbiddenBlocks.add(block);
            }

            // Add trailers of other sections as forbidden blocks
            for (int sector = 1; sector < SECTOR_COUNT; sector++) {
                forbiddenBlocks.add((sector * BLOCKS_PER_SECTOR) + (BLOCKS_PER_SECTOR - 1));
            }

            return Collections.unmodifiableSet(forbiddenBlocks);
        }

        private void authenticate(int blockAddr) throws RfidException {
            final int sectorAddr = blockAddr / BLOCKS_PER_SECTOR;
            System.out.println("Authenticating sector " + sectorAddr + " for block " + blockAddr);
            if (lastAuthedSector != sectorAddr) {
                mifareAuth(MifareKey.getDefaultKeyB(), (byte) blockAddr, getTag());
                lastAuthedSector = sectorAddr;
            }
        }
    }
}
