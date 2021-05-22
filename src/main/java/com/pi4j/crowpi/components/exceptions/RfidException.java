package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.MFRC522;

public class RfidException extends Exception {
    public RfidException(String message) {
        super(message);
    }

    public RfidException(MFRC522.PcdError error) {
        super(error.getDescription());
    }
}
