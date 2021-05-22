package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.internal.rfid.MFRC522;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;

/**
 * Implementation of the CrowPi RFID component using SPI with Pi4J
 */
public class RfidComponent extends MFRC522 {
    /**
     * Default GPIO BCM address of reset pin for RFID component
     */
    protected static final int DEFAULT_RESET_PIN = 25;
    /**
     * Default SPI channel for the RFID component on the CrowPi
     */
    protected static final int DEFAULT_SPI_CHANNEL = 0;
    /**
     * Default SPI baud rate for the RFID component on the CrowPi
     */
    protected static final int DEFAULT_SPI_BAUD_RATE = 1000000;

    /**
     * Creates a new RFID component with the default reset pin, channel and baud rate.
     *
     * @param pi4j Pi4J context
     */
    public RfidComponent(Context pi4j) {
        this(pi4j, DEFAULT_RESET_PIN, DEFAULT_SPI_CHANNEL, DEFAULT_SPI_BAUD_RATE);
    }

    /**
     * Creates a new RFID component with a custom channel, baud rate and no reset pin.
     *
     * @param pi4j       Pi4J context
     * @param spiChannel SPI channel
     * @param spiBaud    SPI baud rate
     */
    public RfidComponent(Context pi4j, int spiChannel, int spiBaud) {
        super(
            null,
            pi4j.create(buildSpiConfig(pi4j, spiChannel, spiBaud))
        );
    }

    /**
     * Creates a new RFID component with a custom reset pin, channel and baud rate.
     *
     * @param pi4j         Pi4J context
     * @param gpioResetPin BCM address of GPIO reset pin
     * @param spiChannel   SPI channel
     * @param spiBaud      SPI baud rate
     */
    public RfidComponent(Context pi4j, int gpioResetPin, int spiChannel, int spiBaud) {
        super(
            pi4j.create(buildResetPinConfig(pi4j, gpioResetPin)),
            pi4j.create(buildSpiConfig(pi4j, spiChannel, spiBaud))
        );
    }

    /**
     * Buidls a new digital output configuration for the GPIO reset pin
     *
     * @param pi4j    Pi4J context
     * @param address BCM address
     * @return Digital output configuration
     */
    private static DigitalOutputConfig buildResetPinConfig(Context pi4j, int address) {
        return DigitalOutput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("RFID Reset Pin")
            .address(address)
            .initial(DigitalState.LOW)
            .shutdown(DigitalState.LOW)
            .build();
    }

    /**
     * Builds a new SPI configuration for the RFID component
     *
     * @param pi4j    Pi4J context
     * @param channel SPI channel
     * @param baud    SPI baud rate
     * @return SPI configuration
     */
    private static SpiConfig buildSpiConfig(Context pi4j, int channel, int baud) {
        return Spi.newConfigBuilder(pi4j)
            .id("SPI" + channel)
            .name("RFID SPI")
            .address(channel)
            .baud(baud)
            .build();
    }
}
