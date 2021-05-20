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
 * Implementation of the CrowPi touch sensor using GPIO with Pi4J
 */
public class SoundSensorComponent extends Component implements DigitalEventProvider<SoundSensorComponent.SoundState> {
    /**
     * Pi4J digital input instance used by this component
     */
    protected final DigitalInput digitalInput;
    /**
     * Default Pin of touch sensor
     */
    protected static final int DEFAULT_PIN = 24;

    /**
     * Handler for simple event when a noise is recognized
     */
    private SimpleEventHandler onNoise;
    /**
     * Handler for simple event when it's silent again
     */
    private SimpleEventHandler onSilence;

    /**
     * Creates a new touch sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public SoundSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN);
    }

    /**
     * Create touch sensor component with custom parameters
     *
     * @param pi4j    Pi4J context
     * @param address GPIO Pin of Raspberry
     */
    public SoundSensorComponent(Context pi4j, int address) {
        this.digitalInput = pi4j.create(buildDigitalInputConfig(pi4j, address));
        this.addListener(this::dispatchSimpleEvents);
    }

    /**
     * Sets or disables the handler for the onTouch event.
     * This event gets triggered whenever the sensor is touched.
     * Only a single event handler can be registered at once.
     *
     * @param handler Event handler to call or null to disable
     */
    public void onNoise(SimpleEventHandler handler) {
        this.onNoise = handler;
    }

    /**
     * Sets or disables the handler for the onRelease event.
     * This event gets triggered whenever the sensor is no longer touched.
     * Only a single event handler can be registered at once.
     *
     * @param handler Event handler to call or null to disable
     */
    public void onSilence(SimpleEventHandler handler) {
        this.onSilence = handler;
    }

    @Override
    public DigitalInput getDigitalInput() {
        return digitalInput;
    }

    @Override
    public SoundState mapDigitalState(DigitalState digitalState) {
        switch (digitalState) {
            case HIGH:
                return SoundState.NOISE;
            case LOW:
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
    public void dispatchSimpleEvents(EventListener listener, SoundState state) {
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
     * @param address GPIO Address of touch sensor
     * @return Return Digital Input configuration
     */
    protected DigitalInputConfig buildDigitalInputConfig(Context pi4j, int address) {
        return DigitalInput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("SoundSensor")
            .address(address)
            .pull(PullResistance.PULL_UP)
            .build();
    }

    /**
     * All available states reported by the touch sensor component.
     */
    public enum SoundState {
        NOISE,
        SILENT,
        UNKNOWN
    }
}
