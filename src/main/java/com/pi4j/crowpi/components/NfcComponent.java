package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.exceptions.NfcException;
import com.pi4j.crowpi.components.helpers.ByteHelpers;
import com.pi4j.crowpi.components.internal.MFRC522;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;

public class NfcComponent extends MFRC522 {
    protected static final int DEFAULT_POWER_PIN = 25;
    protected static final int DEFAULT_SPI_CHANNEL = 0;
    protected static final int DEFAULT_SPI_BAUD_RATE = 1000000;

    protected MFRC522.Tag tag;

    public NfcComponent(Context pi4j) {
        this(pi4j, DEFAULT_POWER_PIN, DEFAULT_SPI_CHANNEL, DEFAULT_SPI_BAUD_RATE);
    }

    public NfcComponent(Context pi4j, int gpioPowerPin, int spiChannel, int spiBaud) {
        super(
            pi4j.create(buildDigitalOutputConfig(pi4j, gpioPowerPin)),
            pi4j.create(buildSpiConfig(pi4j, spiChannel, spiBaud))
        );
    }

    public String readCardSerial() throws NfcException {
        tag = select();
        System.out.println("Card SAK: " + ByteHelpers.toString(tag.getSak()));
        System.out.println("Card serial: " + tag.getSerial());

        return tag.getSerial();
    }

    public byte[] readCard(byte blockAddr) throws NfcException {
        authenticate(AuthKey.getDefaultKeyB(), blockAddr, tag);
        return mifareRead(blockAddr);
    }

    private static DigitalOutputConfig buildDigitalOutputConfig(Context pi4j, int address) {
        return DigitalOutput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("NFC Power")
            .address(address)
            .initial(DigitalState.LOW)
            .shutdown(DigitalState.LOW)
            .build();
    }

    private static SpiConfig buildSpiConfig(Context pi4j, int channel, int baud) {
        return Spi.newConfigBuilder(pi4j)
            .id("SPI" + channel)
            .name("NFC")
            .address(channel)
            .baud(baud)
            .build();
    }
}
