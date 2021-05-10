package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.events.SimpleEventHandler;
import com.pi4j.io.gpio.digital.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;

/**
 * Implementation of the CrowPi button matrix using GPIO with Pi4J
 * <p>
 * The button matrix consists of two separate components:
 * - the selectors, one or more GPIO pins which can be pulled LOW to activate a column
 * - the buttons, one or more GPIO pins which can be used to check a button within the currently active column
 * This means that retrieving the state of all buttons requires looping over all buttons with each selector once pulled LOW.
 * To achieve this, the button matrix component uses an internal poller which gets started by default for polling the buttons.
 * This component requires DIP switches 1-1, 1-2, 1-3, 1-4, 1-5, 1-6, 1-7, 1-8 to be on.
 */
public class ButtonMatrixComponent extends Component {
    /**
     * Default GPIO pins used as selectors for the button matrix.
     * A selector pin can either be a row or a column and gets pulled low to analyze all the button pins.
     * CrowPi Board Pins: 22, 37, 35, 33 (pins down below are BCM)
     */
    protected static final int[] DEFAULT_SELECTOR_PINS = new int[]{25, 26, 19, 13};
    /**
     * Default GPIO pins used as buttons for the button matrix.
     * A button pin does not refer to a single physical button and instead depends on which selector is pulled low.
     * CrowPi Board Pins: 13, 15, 29, 31 (pins down below are BCM)
     */
    protected static final int[] DEFAULT_BUTTON_PINS = new int[]{27, 22, 5, 6};
    /**
     * Default mapping of button number (= array index) to the respective state index.
     * The state array is represented [selector * buttonCount + button] and the following mapping works for the CrowPi.
     */
    protected static final int[] DEFAULT_STATE_MAPPINGS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    /**
     * Default period in milliseconds of button state poller.
     * The poller will be run in a separate thread and executed every X milliseconds.
     */
    protected static final long DEFAULT_POLLER_PERIOD_MS = 25;

    /**
     * Scheduler instance for running the poller thread.
     */
    private final ScheduledExecutorService scheduler;
    /**
     * Active poller thread or null if currently not running.
     */
    private ScheduledFuture<?> poller;

    /**
     * Atomic array of simple event handlers for "onDown" event.
     * Gets triggered whenever a button transitions from unpressed to pressed.
     */
    private final AtomicReferenceArray<SimpleEventHandler> downHandlers;
    /**
     * Atomic array of simple event handlers for "onUp" event.
     * Gets triggered whenever a button transitions from pressed to unpressed.
     */
    private final AtomicReferenceArray<SimpleEventHandler> upHandlers;

    /**
     * State mapping from human-readable button number to actual state index.
     * The index of the array + 1 represents the number, the value the desired state index starting at zero.
     */
    private final int[] stateMappings;
    /**
     * Atomic array of button states which gets repeatedly updated by the poller.
     */
    private final AtomicBoolean[] states;
    /**
     * Array of Pi4J digital outputs for each selector.
     */
    private final DigitalOutput[] selectors;
    /**
     * Array of Pi4J digital inputs for each button.
     */
    private final DigitalInput[] buttons;

    /**
     * Creates a new button matrix component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public ButtonMatrixComponent(Context pi4j) {
        this(pi4j, DEFAULT_SELECTOR_PINS, DEFAULT_BUTTON_PINS, DEFAULT_STATE_MAPPINGS, DEFAULT_POLLER_PERIOD_MS);
    }

    /**
     * Creates a new button matrix component with custom selector/button pins, state mapping and poller period.
     *
     * @param pi4j           Pi4J context
     * @param selectorPins   BCM pins to be used as selectors (digital output)
     * @param buttonPins     BCM pins to be used as buttons (digital input)
     * @param stateMappings  Array of state mappings with same length as total button count
     * @param pollerPeriodMs Period of poller in milliseconds
     */
    public ButtonMatrixComponent(Context pi4j, int[] selectorPins, int[] buttonPins, int[] stateMappings, long pollerPeriodMs) {
        // Initialize selectors and buttons
        this.selectors = buildSelectorDigitalOutputs(pi4j, buttonPins);
        this.buttons = buildButtonDigitalInputs(pi4j, selectorPins);

        // Calculate total number of buttons
        final int buttonCount = this.selectors.length * this.buttons.length;

        // Initialize atomic boolean array for storing states
        this.states = new AtomicBoolean[buttonCount];
        for (int i = 0; i < this.states.length; i++) {
            this.states[i] = new AtomicBoolean(false);
        }

        // Initialize simple event handler arrays
        this.downHandlers = new AtomicReferenceArray<>(buttonCount);
        this.upHandlers = new AtomicReferenceArray<>(buttonCount);

        // Ensure proper bounds for state mappings
        if (stateMappings.length != buttonCount) {
            throw new IllegalArgumentException("State mapping must contain exactly " + stateMappings.length + " entries");
        }
        this.stateMappings = stateMappings;

        // Initialize new scheduler and start the poller
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.startPoller(pollerPeriodMs);
    }

    /**
     * (Re-)starts the poller with the desired time period in milliseconds.
     * If the poller is already running, it will be cancelled and rescheduled with the given time.
     * The first poll happens immediately in a separate thread and does not get delayed.
     *
     * @param pollerPeriodMs Polling period in milliseconds
     */
    public void startPoller(long pollerPeriodMs) {
        if (this.poller != null) {
            this.poller.cancel(true);
        }
        this.poller = scheduler.scheduleAtFixedRate(new Poller(), 0, pollerPeriodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the poller immediately, therefore causing the button states to be no longer refreshed.
     * If the poller is already stopped, this method will silently return and do nothing.
     */
    public void stopPoller() {
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
     * Idle-waits until a button is pressed and released and then returns the button number.
     * If more than one button is pressed, the first one based on its state index is taken.
     * This loop will wait 10 milliseconds between each check and waits indefinitely.
     *
     * @return Number of pressed button or -1 if failed
     */
    public int readBlocking() {
        return readBlocking(0);
    }

    /**
     * Idle-waits until a button is pressed and released and then returns the button number.
     * If more than one button is pressed, the first one based on its state index is taken.
     * No result will be returned until the button gets released, so it may also timeout while waiting for the button to be released.
     * This loop will wait 10 milliseconds between each check and times out after the given time.
     *
     * @param timeoutMs Timeout in milliseconds or 0 for infinite
     * @return Number of pressed button or -1 if failed (e.g. timeout)
     */
    public int readBlocking(long timeoutMs) {
        // Prepare new thread executor for polling with timeout
        final var executor = Executors.newSingleThreadExecutor();

        // Submit new task which idle-waits for a button press to be registered
        final var future = executor.submit(() -> {
            int resultNumber = -1;

            // Wait for at least one button press and register the number of the first one
            while (resultNumber == -1 && !Thread.interrupted()) {
                // Loop through all buttons to check if one was pressed
                // If so, exit and return its number
                for (int number = 1; number <= stateMappings.length; number++) {
                    final var index = resolveIndexFromNumber(number);
                    if (states[index].get()) {
                        resultNumber = number;
                        break;
                    }
                }

                // Wait for 10 milliseconds before checking again
                if (resultNumber == -1) {
                    sleep(10);
                }
            }

            // Wait for the detected button to be released
            while (resultNumber != -1 && isDown(resultNumber) && !Thread.interrupted()) {
                sleep(10);
            }

            return resultNumber;
        });

        // Wait for executor to finish, either by completion or due to timeout
        try {
            if (timeoutMs > 0) {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } else {
                return future.get();
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Returns a list of all buttons which were pressed during the last poll cycle.
     * This can be used for building an idle-wait loop until one or more keys are pressed.
     *
     * @return Array of currently pressed buttons
     */
    public int[] getPressedButtons() {
        return IntStream.range(1, stateMappings.length)
            .filter(number -> states[resolveIndexFromNumber(number)].get())
            .toArray();
    }

    /**
     * Returns the state of the button with the given number (starting at 1) during the last poll cycle.
     * Please note that this value will only update while the poller is running.
     *
     * @param number Button number to check, starting at 1
     * @return Current button state
     */
    public ButtonComponent.ButtonState getState(int number) {
        if (states[resolveIndexFromNumber(number)].get()) {
            return ButtonComponent.ButtonState.DOWN;
        } else {
            return ButtonComponent.ButtonState.UP;
        }
    }

    /**
     * Checks if the button with the given number (starting at 1) was pressed during the last poll cycle.
     * Please note that this value will only update while the poller is running.
     *
     * @param number Button number to check
     * @return True if button is pressed
     */
    public boolean isDown(int number) {
        return getState(number) == ButtonComponent.ButtonState.DOWN;
    }

    /**
     * Checks if the button with the given number (starting at 1) was NOT pressed during the last poll cycle.
     * Please note that this value will only update while the poller is running.
     *
     * @param number Button number to check, starting at 1
     * @return True if button is not pressed
     */
    public boolean isUp(int number) {
        return getState(number) == ButtonComponent.ButtonState.UP;
    }

    /**
     * Sets or disables the handler for the onDown of the given button.
     * This event gets triggered whenever the given button is pressed.
     * Only a single event handler per button can be registered at once.
     *
     * @param number  Button number to check, starting at 1
     * @param handler Event handler to call or null to disable
     */
    public void onDown(int number, SimpleEventHandler handler) {
        downHandlers.set(resolveIndexFromNumber(number), handler);
    }

    /**
     * Sets or disables the handler for the onUp of the given button.
     * This event gets triggered whenever the given button is released.
     * Only a single event handler per button can be registered at once.
     *
     * @param number  Button number to check, starting at 1
     * @param handler Event handler to call or null to disable
     */
    public void onUp(int number, SimpleEventHandler handler) {
        upHandlers.set(resolveIndexFromNumber(number), handler);
    }

    /**
     * Helper method to resolve the internal state index of a button based on its number.
     * This method will lookup the number within the state mapping passed during initialization.
     *
     * @param number Button number to resolve, between 1 and number of buttons inclusively
     * @return Internal state index
     */
    private int resolveIndexFromNumber(int number) {
        if (number < 1 || number > stateMappings.length) {
            throw new IndexOutOfBoundsException("Number for button must be between 1 and " + stateMappings.length);
        }

        final var index = stateMappings[number - 1];
        if (index < 0 || index >= states.length) {
            throw new IndexOutOfBoundsException("State index for button must be between 0 and " + states.length);
        }

        return index;
    }

    /**
     * Build array of Pi4J digital outputs for selector pins.
     * Uses the {@link #buildDigitalOutputConfig(Context, int)} method internally.
     *
     * @param pi4j         Pi4J context
     * @param selectorPins BCM pins to be used as selectors
     * @return Array of Pi4J digital outputs for selector pins
     */
    private static DigitalOutput[] buildSelectorDigitalOutputs(Context pi4j, int[] selectorPins) {
        final var selectors = new DigitalOutput[selectorPins.length];
        for (int i = 0; i < selectorPins.length; i++) {
            selectors[i] = pi4j.create(buildDigitalOutputConfig(pi4j, selectorPins[i]));
        }
        return selectors;
    }

    /**
     * Builds a new DigitalOutput configuration for a button matrix selector.
     *
     * @param pi4j    Pi4J context
     * @param address BCM pin of selector
     * @return DigitalOutputConfig configuration
     */
    protected static DigitalOutputConfig buildDigitalOutputConfig(Context pi4j, int address) {
        return DigitalOutput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("Button Matrix Selector #" + address)
            .address(address)
            .build();
    }

    /**
     * Build array of Pi4J digital inputs for button pins.
     * Uses the {@link #buildDigitalInputConfig(Context, int)} method internally.
     *
     * @param pi4j       Pi4J context
     * @param buttonPins BCM pins to be used as buttons
     * @return Array of Pi4J digital inputs for button pins
     */
    private static DigitalInput[] buildButtonDigitalInputs(Context pi4j, int[] buttonPins) {
        final var buttons = new DigitalInput[buttonPins.length];
        for (int i = 0; i < buttonPins.length; i++) {
            buttons[i] = pi4j.create(buildDigitalInputConfig(pi4j, buttonPins[i]));
        }
        return buttons;
    }

    /**
     * Builds a new DigitalInput configuration for a button matrix button.
     *
     * @param pi4j    Pi4J context
     * @param address BCM pin of button
     * @return DigitalInput configuration
     */
    protected static DigitalInputConfig buildDigitalInputConfig(Context pi4j, int address) {
        return DigitalInput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("Button Matrix Button #" + address)
            .address(address)
            .pull(PullResistance.PULL_UP)
            .build();
    }

    /**
     * Poller class which implements {@link Runnable} to be used with {@link ScheduledExecutorService} for repeated execution.
     * This poller consecutively checks all buttons and updates the internal {@link #states} array.
     * Additionally, simple event handlers will be triggered during state transitions.
     */
    private final class Poller implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < selectors.length; i++) {
                // Pull current selector LOW to analyze state of buttons
                selectors[i].low();

                // Store new state of each button and trigger event if needed
                for (int j = 0; j < buttons.length; j++) {
                    // Calculate state index and swap with new state
                    final var index = i * buttons.length + j;
                    final var newState = buttons[j].state() == DigitalState.LOW;
                    final var oldState = states[index].getAndSet(newState);

                    // If the state did not change, continue with next button
                    if (oldState == newState) {
                        continue;
                    }

                    // Otherwise trigger the appropriate event handler
                    if (newState) {
                        triggerSimpleEvent(downHandlers.get(index));
                    } else {
                        triggerSimpleEvent(upHandlers.get(index));
                    }
                }

                // Pull current selector HIGH before moving to next selector
                selectors[i].high();
            }
        }
    }
}
