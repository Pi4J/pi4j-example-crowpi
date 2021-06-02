package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.events.DigitalEventProvider;
import com.pi4j.crowpi.components.events.EventListener;
import com.pi4j.crowpi.components.events.SimpleEventHandler;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Implementation of the CrowPi sound sensor using GPIO with Pi4J
 */
public class SoundSensorComponent extends Component implements DigitalEventProvider<SoundSensorComponent.SoundState> {
    /**
     * Pi4J digital input instance used by this component
     */
    protected final DigitalInput digitalInput;
    /**
     * Default Pin of sound sensor
     */
    protected static final int DEFAULT_PIN = 24;
    /**
     * Debounce of input in microseconds
     */
    protected static final long DEFAULT_DEBOUNCE = 1000;

    /**
     * Handler for simple event when a noise is recognized
     */
    private SimpleEventHandler onNoise;
    /**
     * Handler for simple event when it's silent again
     */
    private SimpleEventHandler onSilence;

    /**
     * Creates a new sound sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public SoundSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN, DEFAULT_DEBOUNCE);
    }

    /**
     * Create sound sensor component with custom parameters
     *
     * @param pi4j     Pi4J context
     * @param address  Custom BCM pin address
     * @param debounce Time to debounce the sound sensor in microseconds
     */
    public SoundSensorComponent(Context pi4j, int address, long debounce) {
        this.digitalInput = pi4j.create(buildDigitalInputConfig(pi4j, address, debounce));
        this.addListener(this::dispatchSimpleEvents);
    }

    /**
     * Sets or disables the handler for the onNoise event.
     * This event gets triggered whenever the sensor registers noise.
     * Only a single event handler can be registered at once.
     *
     * @param handler Event handler to call or null to disable
     */
    public void onNoise(SimpleEventHandler handler) {
        this.onNoise = handler;
    }

    /**
     * Reads the current sensor state
     *
     * @return Returns true if there is a noise currently registered by the sensor
     */
    public boolean isNoisy() {
        return this.getState() == SoundState.NOISE;
    }

    /**
     * Reads the current sensor state
     *
     * @return Returns true if it is silent around the sensor
     */
    public boolean isSilent() {
        return this.getState() == SoundState.SILENT;
    }

    /**
     * Returns the SoundState of the sensor
     *
     * @return Returns the well-known SoundState according to the current noise level
     */
    public SoundState getState() {
        return mapDigitalState(digitalInput.state());
    }

    /**
     * Sets or disables the handler for the onSilence event.
     * This event gets triggered whenever the sensor is no longer under noise
     * Only a single event handler can be registered at once.
     *
     * @param handler Event handler to call or null to disable
     */
    public void onSilence(SimpleEventHandler handler) {
        this.onSilence = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DigitalInput getDigitalInput() {
        return digitalInput;
    }

    /**
     * Maps a {@link DigitalState} to a well-known {@link SoundState}
     *
     * @param digitalState Pi4J digital state to map
     * @return Mapped sound state
     */
    @Override
    public SoundState mapDigitalState(DigitalState digitalState) {
        switch (digitalState) {
            case LOW:
                return SoundState.NOISE;
            case HIGH:
                return SoundState.SILENT;
            case UNKNOWN:
            default:
                return SoundState.UNKNOWN;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchSimpleEvents(SoundState state) {
        switch (state) {
            case NOISE:
                triggerSimpleEvent(onNoise);
                break;
            case SILENT:
                triggerSimpleEvent(onSilence);
                break;
        }
    }

    /**
     * Configure Digital Input
     *
     * @param pi4j    PI4J Context
     * @param address GPIO Address of sound sensor
     * @return Return Digital Input configuration
     */
    protected DigitalInputConfig buildDigitalInputConfig(Context pi4j, int address, long debounce) {
        return DigitalInput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("SoundSensor")
            .address(address)
            .pull(PullResistance.PULL_UP)
            .debounce(debounce)
            .build();
    }

    /**
     * All available states reported by the sound sensor component.
     */
    public enum SoundState {
        NOISE,
        SILENT,
        UNKNOWN
    }
}
