package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.RfidComponent;
import com.pi4j.crowpi.components.exceptions.RfidException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Mifare1K extends RfidCard {
    private static final byte SECTOR_COUNT = 16;
    private static final byte BLOCKS_PER_SECTOR = 4;
    private static final byte BYTES_PER_BLOCK = 16;
    private static final byte BLOCK_COUNT = SECTOR_COUNT * BLOCKS_PER_SECTOR;

    private final RfidComponent rfidComponent;
    private final Set<Integer> forbiddenBlocks;
    private final int totalCapacity;
    private int lastAuthedSector = -1;

    public Mifare1K(RfidComponent rfidComponent, RfidUid uid) {
        super(uid);
        this.rfidComponent = rfidComponent;
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
            buffer.writeBytes(rfidComponent.mifareRead((byte) block));
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
                rfidComponent.mifareWrite((byte) blockAddr, chunk);
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
            rfidComponent.mifareAuth(MifareKey.getDefaultKeyB(), (byte) blockAddr, getUid());
            lastAuthedSector = sectorAddr;
        }
    }
}
