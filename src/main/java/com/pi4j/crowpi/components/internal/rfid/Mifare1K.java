package com.pi4j.crowpi.components.internal.rfid;

import com.pi4j.crowpi.components.exceptions.RfidException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of MIFARE Classic 1K cards with 16 sectors with 4 blocks (16 bytes) each.
 * First sector with manufacturer block and sector trailers are explicitly hidden from the user.
 */
public final class Mifare1K extends RfidCard {
    /**
     * Number of available sectors on this card
     */
    private static final byte SECTOR_COUNT = 16;
    /**
     * Number of available blocks per sector
     */
    private static final byte BLOCKS_PER_SECTOR = 4;
    /**
     * Number of bytes per block
     */
    private static final byte BYTES_PER_BLOCK = 16;
    /**
     * Total amount of available blocks on this card
     */
    private static final byte BLOCK_COUNT = SECTOR_COUNT * BLOCKS_PER_SECTOR;

    /**
     * Instance of MFRC522 which created this card instance
     */
    private final MFRC522 mfrc522;

    /**
     * Set of integers to be considered as forbidden blocks
     */
    private final Set<Integer> forbiddenBlocks;
    /**
     * Pre-calculated capacity in bytes of this card
     */
    private final int totalCapacity;
    /**
     * Cache for storing the most recently authenticated sector
     */
    private int lastAuthedSector = -1;

    /**
     * Creates a new card instance for the given RFID component and PICC UID
     *
     * @param mfrc522 MFRC522 instance which detected this card
     * @param uid     UID of this card / PICC
     */
    Mifare1K(MFRC522 mfrc522, RfidCardUid uid) {
        super(uid);
        this.mfrc522 = mfrc522;
        this.forbiddenBlocks = determineForbiddenBlocks();
        this.totalCapacity = (BLOCK_COUNT - forbiddenBlocks.size()) * BYTES_PER_BLOCK;
    }

    /**
     * {@inheritDoc}
     */
    public int getCapacity() {
        return totalCapacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] readBytes() throws RfidException {
        final var buffer = new ByteArrayOutputStream();

        // Read all available blocks from card
        for (int blockAddr = 0; blockAddr < BLOCK_COUNT; blockAddr++) {
            // Skip block if marked as forbidden
            if (forbiddenBlocks.contains(blockAddr)) {
                logger.trace("Skipping read of forbidden block #{}", blockAddr);
                continue;
            }

            // Read block from card into buffer
            authenticate(blockAddr);
            buffer.writeBytes(mfrc522.mifareRead((byte) blockAddr));
        }

        return buffer.toByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeBytes(byte[] data) throws RfidException, IllegalArgumentException {
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

                logger.debug("Writing chunk {} to block #{}", ByteHelpers.toString(chunk), blockAddr);
                authenticate(blockAddr);
                mfrc522.mifareWrite((byte) blockAddr, chunk);
            } else {
                logger.trace("Skipping write of forbidden block #{}", blockAddr);
            }

            // Advance to next block
            blockAddr++;
        }

        // Throw an exception if not all chunks were written
        if (!chunks.isEmpty()) {
            throw new RfidException("Could not write all data into card, data will most likely be corrupted");
        }
    }

    /**
     * Starts MIFARE authentication for the sector of the specified block address.
     * To avoid unnecessary repetition for contiguous blocks in the same sector, this method remembers the most recently authed sector.
     * If the current sector happens to be the same one, the authentication gets silently skipped.
     * This method always uses the default key B which all cards use by default.
     *
     * @param blockAddr Address of block whose sector must be authenticated
     * @throws RfidException Authentication failure
     */
    private void authenticate(int blockAddr) throws RfidException {
        final int sectorAddr = blockAddr / BLOCKS_PER_SECTOR;
        if (lastAuthedSector != sectorAddr) {
            logger.debug("Using MIFARE authentication for block #{} in sector #{}", blockAddr, sectorAddr);
            mfrc522.mifareAuth(MifareKey.getDefaultKeyB(), (byte) blockAddr, getUid());
            lastAuthedSector = sectorAddr;
        }
    }

    /**
     * Determine all forbidden blocks for this card and return them as a set.
     * Modifying this method inappropriately may PERMANENTLY brick your card.
     * This specific implementation excludes the whole first sector as well as all sector trailers.
     *
     * @return Addresses of forbidden blocks
     */
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
}
