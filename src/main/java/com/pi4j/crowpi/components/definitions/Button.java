package com.pi4j.crowpi.components.definitions;

/**
 * Mapping of CrowPi buttons to the respective BCM pins
 */
public enum Button {
    /**
     * UP button with board pin 37
     */
    UP(26),
    /**
     * DOWN button with board pin 33
     */
    DOWN(13),
    /**
     * LEFT button with board pin 22
     */
    LEFT(25),
    /**
     * RIGHT button with board pin 35
     */
    RIGHT(19);

    private final int bcmPin;
    private final boolean inverted;

    Button(int bcmPin) {
        this.bcmPin = bcmPin;
        this.inverted = true;
    }

    public int getBcmPin() {
        return this.bcmPin;
    }

    public boolean getInverted() {
        return this.inverted;
    }
}
