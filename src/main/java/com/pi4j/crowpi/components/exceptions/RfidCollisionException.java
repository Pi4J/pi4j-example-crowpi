package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.MFRC522;

public class RfidCollisionException extends RfidException {
    public RfidCollisionException() {
        super(MFRC522.PcdError.COLL_ERR);
    }

    public RfidCollisionException(String message) {
        super(message);
    }
}
