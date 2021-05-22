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
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * Default polling period in milliseconds for detecting new cards
     */
    protected static final long DEFAULT_POLLER_PERIOD_MS = 100;

    /**
     * Atomic reference to event handler for card detection
     */
    private final AtomicReference<EventHandler<RfidCard>> cardDetectedHandler;

    private final AtomicBoolean pollOnlyNewCards;

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
        this.pollOnlyNewCards = new AtomicBoolean(true);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Sets or disables the handler for any new card which gets in the proximity of the PCD.
     * Previously written cards are automatically being ignored due to being in the HALT state.
     *
     * @param handler Event handler to call when new card is approached
     */
    public synchronized void onCardDetected(EventHandler<RfidCard> handler) {
        this.cardDetectedHandler.set(handler);
        if (handler != null) {
            startPoller(DEFAULT_POLLER_PERIOD_MS);
        } else {
            stopPoller();
        }
    }

    /**
     * Blocks current thread until a new card has been detected, then runs the handler and continues.
     *
     * @see #waitForCard(EventHandler, boolean)
     */
    public void waitForNewCard(EventHandler<RfidCard> handler) {
        waitForCard(handler, true);
    }

    /**
     * Blocks current thread until any card has been detected, then runs the handler and continues.
     *
     * @see #waitForCard(EventHandler, boolean)
     */
    public void waitForAnyCard(EventHandler<RfidCard> handler) {
        waitForCard(handler, false);
    }

    /**
     * Acts like {@link #onCardDetected(EventHandler)}, but only executes the handler once and waits for the event to happen.
     * This can be used to simplify synchronous programming without having to deal with asynchronous event logic.
     *
     * @param handler Event handler to call when new card is approached
     * @param onlyNew Specifies if only new cards (true) or any card (false) should be detected
     */
    private void waitForCard(EventHandler<RfidCard> handler, boolean onlyNew) {
        final var done = new AtomicBoolean(false);

        // Register new event handler which triggers exactly once
        onCardDetected(card -> {
            try {
                handler.handle(card);
            } finally {
                onCardDetected(null);
                this.pollOnlyNewCards.set(true);
                done.set(true);
            }
        });

        // If only new has been disabled, switch poller mode to detect any card
        // This will wakeup any existing cards, forcing them to be re-detected
        if (!onlyNew) {
            this.pollOnlyNewCards.set(false);
        }

        // Wait until event handler has been called once
        while (!done.get()) {
            sleep(10);
        }
    }

    /**
     * (Re-)starts the poller with the desired time period in milliseconds.
     * If the poller is already running, it will be cancelled and rescheduled with the given time.
     * The first poll happens immediately in a separate thread and does not get delayed.
     *
     * @param pollerPeriodMs Polling period in milliseconds
     */
    protected synchronized void startPoller(long pollerPeriodMs) {
        if (this.poller != null) {
            this.poller.cancel(true);
        }
        this.poller = scheduler.scheduleAtFixedRate(new Poller(), 0, pollerPeriodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the poller immediately, therefore causing RFID cards to be no longer detected.
     * If the poller is already stopped, this method will silently return and do nothing.
     */
    protected synchronized void stopPoller() {
        if (this.poller != null) {
            this.poller.cancel(true);
            this.poller = null;
        }
    }

    /**
     * Returns the internal scheduled future for the poller thread or null if currently stopped.
     *
     * @return Active poller instance or null
     */
    protected ScheduledFuture<?> getPoller() {
        return this.poller;
    }

    /**
     * Returns the current reset pin for the RFID component.
     *
     * @return Reset pin as digital output
     */
    protected DigitalOutput getResetPin() {
        return this.resetPin;
    }

    /**
     * Returns the current SPI instance for the LED matrix.
     *
     * @return SPI instance
     */
    protected Spi getSpi() {
        return this.spi;
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

    /**
     * Poller class which implements {@link Runnable} to be used with {@link ScheduledExecutorService} for repeated execution.
     * This poller consecutively calls {@link MFRC522#isNewCardPresent()} to check for any new idle PICCs within the proximity of the PCD.
     * If any new card is found, it will be read and passed to the registered handler before being put into a HALT state.
     */
    private final class Poller implements Runnable {
        @Override
        public void run() {
            // Retrieve handler and store to avoid race conditions
            final var handler = cardDetectedHandler.get();

            // Abort if handler is not registered to avoid putting PICCs into HALT state too early
            if (handler == null) {
                return;
            }

            // Abort if no eligible card is within proximity of PCD
            // We can either only check for new cards (REQA) or any card (WUPA)
            if (pollOnlyNewCards.get()) {
                if (!isNewCardPresent()) {
                    return;
                }
            } else {
                if (!isAnyCardPresent()) {
                    return;
                }
            }

            // Trigger onCardDetected handler for this new PICC
            try {
                // Initialize new card and pass instance to handler
                final var card = initializeCard();
                handler.handle(card);
            } catch (RfidUnsupportedCardException e) {
                // While this card is unsupported, this is not an abnormal exception
                // We therefore handle this situation separately from the generic RfidException to avoid resetting the MFRC522
                logger.warn("Ignoring unsupported RFID card type: {}", e.getCardType());
            } catch (RfidException e) {
                // Reset the MFRC522 for any abnormal exceptions
                // This is required to ensure further operation is possible
                logger.warn("Resetting RFID component due to abnormal exception: {}", e);
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
