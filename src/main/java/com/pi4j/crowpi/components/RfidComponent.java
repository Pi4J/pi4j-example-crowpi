package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.exceptions.RfidException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;
import com.pi4j.crowpi.components.internal.rfid.MFRC522;
import com.pi4j.crowpi.components.internal.rfid.Mifare1K;
import com.pi4j.crowpi.components.internal.rfid.RfidCard;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;

public class RfidComponent extends MFRC522 {
    protected static final int DEFAULT_POWER_PIN = 25;
    protected static final int DEFAULT_SPI_CHANNEL = 0;
    protected static final int DEFAULT_SPI_BAUD_RATE = 1000000;

    public RfidComponent(Context pi4j) {
        this(pi4j, DEFAULT_POWER_PIN, DEFAULT_SPI_CHANNEL, DEFAULT_SPI_BAUD_RATE);
    }

    public RfidComponent(Context pi4j, int gpioPowerPin, int spiChannel, int spiBaud) {
        super(
            pi4j.create(buildDigitalOutputConfig(pi4j, gpioPowerPin)),
            pi4j.create(buildSpiConfig(pi4j, spiChannel, spiBaud))
        );
    }

    public RfidCard initializeCard() throws RfidException {
        final var tag = select();

        if (tag.getSak() == 0x08) {
            return new Mifare1K(this, tag);
        } else {
            throw new RfidException("Unsupported SAK " + ByteHelpers.toString(tag.getSak()) + ", only MIFARE Classic 1K is supported");
        }
    }

    private static DigitalOutputConfig buildDigitalOutputConfig(Context pi4j, int address) {
        return DigitalOutput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("RFID Reset Pin")
            .address(address)
            .initial(DigitalState.LOW)
            .shutdown(DigitalState.LOW)
            .build();
    }

    private static SpiConfig buildSpiConfig(Context pi4j, int channel, int baud) {
        return Spi.newConfigBuilder(pi4j)
            .id("SPI" + channel)
            .name("RFID SPI")
            .address(channel)
            .baud(baud)
            .build();
    }

}
