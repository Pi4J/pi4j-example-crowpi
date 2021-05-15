package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.MFRC522;

public class NfcCollisionException extends NfcException {
    public NfcCollisionException() {
        super(MFRC522.PcdError.COLL_ERR);
    }

    public NfcCollisionException(String message) {
        super(message);
    }
}
