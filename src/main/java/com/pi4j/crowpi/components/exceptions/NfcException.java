package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.MFRC522;

public class NfcException extends Exception {
    public NfcException(String message) {
        super(message);
    }

    public NfcException(MFRC522.PcdError error) {
        super(error.getDescription());
    }
}
