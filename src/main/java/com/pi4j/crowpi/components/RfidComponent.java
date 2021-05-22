package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.events.EventHandler;
import com.pi4j.crowpi.components.exceptions.RfidException;
import com.pi4j.crowpi.components.exceptions.RfidUnsupportedCardException;
import com.pi4j.crowpi.components.internal.rfid.MFRC522;
import com.pi4j.crowpi.components.internal.rfid.RfidCard;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    protected static final long DEFAULT_POLLER_PERIOD_MS = 100;

    private final AtomicReference<EventHandler<RfidCard>> cardDetectedHandler;

    /**
     * Scheduler instance for running the poller thread.
     */
    private final ScheduledExecutorService scheduler;
    /**
     * Active poller thread or null if currently not running.
     */
    private ScheduledFuture<?> poller;

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
        this(pi4j, null, spiChannel, spiBaud);
    }

    /**
     * Creates a new RFID component with a custom reset pin, channel and baud rate.
     *
     * @param pi4j         Pi4J context
     * @param gpioResetPin BCM address of GPIO reset pin
     * @param spiChannel   SPI channel
     * @param spiBaud      SPI baud rate
     */
    public RfidComponent(Context pi4j, Integer gpioResetPin, int spiChannel, int spiBaud) {
        super(
            pi4j.create(buildResetPinConfig(pi4j, gpioResetPin)),
            pi4j.create(buildSpiConfig(pi4j, spiChannel, spiBaud))
        );

        this.cardDetectedHandler = new AtomicReference<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public synchronized void onCardDetected(EventHandler<RfidCard> handler) {
        this.cardDetectedHandler.set(handler);
        if (handler != null) {
            startPoller(DEFAULT_POLLER_PERIOD_MS);
        } else {
            stopPoller();
        }
    }

    protected void startPoller(long pollerPeriodMs) {
        if (this.poller != null) {
            this.poller.cancel(true);
        }
        this.poller = scheduler.scheduleAtFixedRate(new Poller(), 0, pollerPeriodMs, TimeUnit.MILLISECONDS);
    }

    protected void stopPoller() {
        if (this.poller != null) {
            this.poller.cancel(true);
            this.poller = null;
        }
    }

    /**
     * Builds a new digital output configuration for the GPIO reset pin
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

    private final class Poller implements Runnable {
        @Override
        public void run() {
            // Retrieve handler and store to avoid race conditions
            final var handler = cardDetectedHandler.get();

            // Abort if handler is not registered to avoid putting PICCs into HALT state too early
            if (handler == null) {
                return;
            }

            // Abort if no new card is within proximity of PCD
            if (!isNewCardPresent()) {
                return;
            }

            // Trigger onCardDetected handler for this new PICC
            try {
                // Initialize new card and pass instance to handler
                final var card = initializeCard();
                handler.handle(card);
            } catch (RfidUnsupportedCardException e) {
                // While this card is unsupported, this is not an abnormal exception
                // We therefore handle this situation separately from the generic RfidException to avoid resetting the MFRC522
                System.out.println("Unsupported RFID card: " + e.getCardType());
            } catch (RfidException e) {
                // Reset the MFRC522 for any abnormal exceptions
                // This is required to ensure further operation is possible
                System.out.println("Abnormal RFID exception: " + e.getMessage());
                reset();
            } finally {
                // Always attempt to uninitialize the current card
                // This will put the card into a HALT state and avoid further detection
                try {
                    uninitializeCard();
                } catch (RfidException ignored) {
                }
            }
        }
    }
}
