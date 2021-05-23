package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalOutput;
import com.pi4j.plugin.mock.provider.spi.MockSpi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RfidComponentTest extends ComponentTest {
    protected RfidComponent rfid;
    protected MockSpi spi;
    protected MockDigitalOutput resetPin;

    @BeforeEach
    void setUp() {
        this.rfid = new RfidComponent(pi4j);
        this.spi = toMock(rfid.getSpi());
        this.resetPin = toMock(rfid.getResetPin());
    }

    @Test
    void testInitialization() {
        // then
        assertTrue(resetPin.isHigh());
    }

    @Test
    void testPollerAutoStart() {
        // when
        rfid.onCardDetected(card -> {
        });

        // then
        assertFalse(rfid.getPoller().isDone());
    }

    @Test
    void testPollerAutoRestart() {
        // given
        rfid.onCardDetected(card -> {
        });
        final var oldPoller = rfid.getPoller();

        // when
        rfid.onCardDetected(card -> {
        });
        final var newPoller = rfid.getPoller();

        // then
        assertTrue(oldPoller.isDone());
        assertFalse(newPoller.isDone());
    }

    @Test
    void testPollerAutoStop() {
        // given
        rfid.onCardDetected(card -> {
        });
        final var oldPoller = rfid.getPoller();

        // when
        rfid.onCardDetected(null);

        // then
        assertNull(rfid.getPoller());
        assertTrue(oldPoller.isDone());
    }
}
