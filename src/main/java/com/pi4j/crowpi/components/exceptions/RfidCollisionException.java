package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.rfid.PcdError;

/**
 * Collision exception for the RFID component based on {@link RfidException}.
 * Happens when multiple PICCs are in the proximity of the PCD.
 */
public class RfidCollisionException extends RfidException {
    public RfidCollisionException() {
        super(PcdError.COLL_ERR);
    }

    public RfidCollisionException(String message) {
        super(message);
    }
}
