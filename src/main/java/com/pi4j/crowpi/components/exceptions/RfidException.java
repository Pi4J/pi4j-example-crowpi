package com.pi4j.crowpi.components.exceptions;

import com.pi4j.crowpi.components.internal.rfid.PcdError;

public class RfidException extends Exception {
    public RfidException(String message) {
        super(message);
    }

    public RfidException(String message, Throwable previous) {
        super(message, previous);
    }

    public RfidException(PcdError error) {
        super(error.getDescription());
    }
}
