package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.events.DigitalEventProvider;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Implementation of the CrowPi touch sensor using GPIO with Pi4J
 */
public class TouchSensorComponent extends Component implements DigitalEventProvider<TouchSensorComponent.TouchState> {
    /**
     * Pi4J digital input instance used by this component
     */
    protected final DigitalInput digitalInput;
    /**
     * Default Pin of touch sensor
     */
    protected static final int DEFAULT_PIN = 17;
    /**
     * Debounce of input in microseconds
     */
    protected static final long DEFAULT_DEBOUNCE = 10000;

    /**
     * Creates a new touch sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public TouchSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN, DEFAULT_DEBOUNCE);
    }

    /**
     * Create touch sensor component with custom parameters
     *
     * @param pi4j     Pi4J context
     * @param address  GPIO Pin of Raspberry
     * @param debounce Time in Microseconds to debounce input
     */
    public TouchSensorComponent(Context pi4j, int address, long debounce) {
        this.digitalInput = pi4j.create(buildDigitalInputConfig(pi4j, address, debounce));
    }

    /**
     * Returns the current state of the touch sensor
     *
     * @return Current touch sensor state
     */
    public TouchState getState() {
        return mapDigitalState(digitalInput.state());
    }

    /**
     * Maps a {@link DigitalState} to a well-known {@link TouchState}
     *
     * @param digitalState Pi4J digital state to map
     * @return Mapped touch state
     */
    @Override
    public TouchState mapDigitalState(DigitalState digitalState) {
        switch (digitalState) {
            case HIGH:
                return TouchState.TOUCHED;
            case LOW:
                return TouchState.UNTOUCHED;
            case UNKNOWN:
            default:
                return TouchState.UNKNOWN;
        }
    }

    /**
     * Method to check if touch sensor is currently touched
     *
     * @return True if touch sensor is currently touched
     */
    public boolean isTouched() {
        return getState() == TouchState.TOUCHED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DigitalInput getDigitalInput() {
        return digitalInput;
    }

    /**
     * Configure Digital Input
     *
     * @param pi4j     PI4J Context
     * @param address  GPIO Address of touch sensor
     * @param debounce debounce time in microseconds
     * @return Return Digital Input configuration
     */
    protected DigitalInputConfig buildDigitalInputConfig(Context pi4j, int address, long debounce) {
        return DigitalInput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("TouchSensor")
            .address(address)
            .debounce(debounce)
            .pull(PullResistance.PULL_UP)
            .build();
    }

    /**
     * All available states reported by the touch sensor component.
     */
    public enum TouchState {
        TOUCHED,
        UNTOUCHED,
        UNKNOWN
    }
}
