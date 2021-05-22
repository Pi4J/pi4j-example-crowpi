package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.rfid.PcdError;

public class RfidCollisionException extends RfidException {
    public RfidCollisionException() {
        super(PcdError.COLL_ERR);
    }

    public RfidCollisionException(String message) {
        super(message);
    }
}
