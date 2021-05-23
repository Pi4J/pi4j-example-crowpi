package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.events.DigitalEventProvider;
import com.pi4j.crowpi.components.events.FlappingEventProvider;
import com.pi4j.crowpi.components.events.SimpleEventHandler;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Implementation of the CrowPi tilt sensor using GPIO with Pi4J
 */
public class TiltSensorComponent extends Component implements DigitalEventProvider<TiltSensorComponent.TiltState> {
    /**
     * Pi4J digital input instance used by this component
     */
    private final DigitalInput digitalInput;

    /**
     * Default BCM pin of tilt sensor for CrowPi
     */
    protected static final int DEFAULT_PIN = 22;
    /**
     * Default debounce time in microseconds
     */
    protected static final long DEFAULT_DEBOUNCE = 25000;
    /**
     * Default threshold for considering consecutive state changes as shaking.
     */
    protected static final int DEFAULT_SHAKE_THRESHOLD = 3;

    /**
     * Handler for simple event when sensor is tilted left
     */
    private SimpleEventHandler onTiltLeft;
    /**
     * Handler for simple event when sensor is tilted right
     */
    private SimpleEventHandler onTiltRight;
    /**
     * Provider for the shake event based on repeated state flapping
     */
    private final FlappingEventProvider<TiltState> shakeEventProvider;

    /**
     * Creates a new tilt sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public TiltSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN, DEFAULT_DEBOUNCE);
    }

    /**
     * Creates a new tilt sensor component with custom GPIO address and debounce time.
     *
     * @param pi4j     Pi4J context
     * @param address  GPIO address of tilt sensor
     * @param debounce Debounce time in microseconds
     */
    public TiltSensorComponent(Context pi4j, int address, long debounce) {
        this.digitalInput = pi4j.create(buildDigitalInputConfig(pi4j, address, debounce));
        this.shakeEventProvider = new FlappingEventProvider<>(TiltState.LEFT, TiltState.RIGHT);
        this.addListener(this.shakeEventProvider);
        this.addListener(this::dispatchSimpleEvents);
    }

    /**
     * Returns the current state of the tilt sensor.
     *
     * @return Current tilt sensor state
     */
    public TiltState getState() {
        return mapDigitalState(digitalInput.state());
    }

    /**
     * Maps a {@link DigitalState} to a well-known {@link TiltState}
     *
     * @param digitalState Pi4J digital state to map
     * @return Mapped tilt state
     */
    @Override
    public TiltState mapDigitalState(DigitalState digitalState) {
        switch (digitalState) {
            case HIGH:
                return TiltState.LEFT;
            case LOW:
                return TiltState.RIGHT;
            case UNKNOWN:
            default:
                return TiltState.UNKNOWN;
        }
    }

    /**
     * Checks if the tilt sensor is currently tilted left
     *
     * @return True if tilted left
     */
    public boolean hasLeftTilt() {
        return getState() == TiltState.LEFT;
    }

    /**
     * Checks if the tilt sensor is currently tilted right
     *
     * @return True if tilted right
     */
    public boolean hasRightTilt() {
        return getState() == TiltState.RIGHT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchSimpleEvents(TiltState value) {
        switch (value) {
            case LEFT:
                triggerSimpleEvent(onTiltLeft);
                break;
            case RIGHT:
                triggerSimpleEvent(onTiltRight);
                break;
        }
    }

    /**
     * Sets or disables the handler for the onTiltLeft event.
     * This event gets triggered whenever the sensor is tilted left.
     * Only a single event handler can be registered at once.
     *
     * @param handler Event handler to call or null to disable
     */
    public void onTiltLeft(SimpleEventHandler handler) {
        this.onTiltLeft = handler;
    }

    /**
     * Sets or disables the handler for the onTiltRight event.
     * This event gets triggered whenever the sensor is tilted right.
     * Only a single event handler can be registered at once.
     *
     * @param handler Event handler to call or null to disable
     */
    public void onTiltRight(SimpleEventHandler handler) {
        this.onTiltRight = handler;
    }

    /**
     * Sets or disables the handler for the onShake event.
     * This event gets triggered whenever the sensor alternates between left/right in a short time.
     * Using this method will use the {@link #DEFAULT_SHAKE_THRESHOLD} as the desired shake threshold.
     * Only a single event handler can be registered at once.
     *
     * @param handler Event handler to call or null to disable
     */
    public void onShake(SimpleEventHandler handler) {
        onShake(DEFAULT_SHAKE_THRESHOLD, handler);
    }

    /**
     * Sets or disables the handler for the onShake event.
     * This event gets triggered whenever the sensor alternates between left/right in a short time.
     * Only a single event handler can be registered at once.
     *
     * @param threshold Threshold when to consider consecutive state changes as shaking.
     *                  As an example, passing 3 would mean that repeatedly alternating between left/right/left (or vice-versa)
     *                  would be considered as shaking.
     * @param handler   Event handler to call or null to disable
     */
    public void onShake(int threshold, SimpleEventHandler handler) {
        this.shakeEventProvider.setOptions(threshold, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DigitalInput getDigitalInput() {
        return this.digitalInput;
    }

    /**
     * Builds a new DigitalInput instance for the tilt sensor.
     *
     * @param pi4j     Pi4J context
     * @param address  GPIO address of tilt sensor
     * @param debounce Debounce time in microseconds
     * @return DigitalInput configuration
     */
    protected DigitalInputConfig buildDigitalInputConfig(Context pi4j, int address, long debounce) {
        return DigitalInput.newConfigBuilder(pi4j)
            .id("BCM-" + address)
            .name("Tilt Sensor")
            .address(address)
            .debounce(debounce)
            .pull(PullResistance.PULL_DOWN)
            .build();
    }

    /**
     * All available states reported by the tilt sensor component.
     */
    public enum TiltState {
        LEFT,
        RIGHT,
        UNKNOWN
    }
}
