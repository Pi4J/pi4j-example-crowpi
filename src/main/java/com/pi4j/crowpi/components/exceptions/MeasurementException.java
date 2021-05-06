package com.pi4j.crowpi.components.exceptions;

/**
 * This class provides exceptions when measurements fail
 */
public class MeasurementException extends RuntimeException {

    /**
     * Exception to throw when a measurement fails
     *
     * @param message Exception reason message
     */
    public MeasurementException(String message) {
        super(message);
    }
}
