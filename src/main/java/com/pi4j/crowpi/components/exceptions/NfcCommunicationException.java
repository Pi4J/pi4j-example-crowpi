package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.MFRC522;

public class NfcCommunicationException extends NfcException {
    public NfcCommunicationException(String message) {
        super(message);
    }

    public NfcCommunicationException(MFRC522.PcdError error) {
        this(error.getDescription());
    }
}
