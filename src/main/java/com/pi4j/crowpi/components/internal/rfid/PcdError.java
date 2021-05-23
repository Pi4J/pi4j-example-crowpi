package com.pi4j.crowpi.components.internal.rfid;

/**
 * Error types which can be signalled by the PCD
 */
public enum PcdError {
    PROTOCOL_ERR(0x1, "Protocol error"),
    PARITY_ERR(0x2, "Parity check failed"),
    CRC_ERR(0x4, "CRC checksum mismatch"),
    COLL_ERR(0x8, "Bit-collision detected"),
    BUFFER_OVFL(0x10, "FIFO buffer overflow"),
    TEMP_ERR(0x40, "Temperature error due to overheating"),
    WR_ERR(0x80, "Illegal write error");

    private final byte value;
    private final String description;

    PcdError(int value, String description) {
        this((byte) value, description);
    }

    PcdError(byte value, String description) {
        this.value = value;
        this.description = description;
    }

    public byte getValue() {
        return this.value;
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * Compares the given ERR register (previously read elsewhere) against a list of errors and returns the first match.
     * If none of the specified errors are currently set in the ERR register, null is returned.
     *
     * @param errReg Value of ERR register
     * @param errors One or more errors to check for
     * @return First active error found or null if none
     */
    public static PcdError matchErrReg(byte errReg, PcdError... errors) {
        for (final var error : errors) {
            if (error.isSet(errReg)) {
                return error;
            }
        }
        return null;
    }

    /**
     * Helper method to determine if the  ERR register (previously read elsewhere) has this specific error set.
     *
     * @param errReg Value of ERR register
     * @return True if set, false if unset
     */
    public boolean isSet(byte errReg) {
        return (errReg & getValue()) != 0;
    }
}
