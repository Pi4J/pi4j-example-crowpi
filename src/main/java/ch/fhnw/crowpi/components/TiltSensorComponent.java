package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.events.DigitalEventProvider;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Implementation of the CrowPi tilt sensor using GPIO with Pi4J
 */
public class TiltSensorComponent implements DigitalEventProvider<TiltSensorComponent.TiltState> {
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
    protected static final long DEFAULT_DEBOUNCE = 10000;

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
     * @param debounce Debounce time in milliseconds
     */
    public TiltSensorComponent(Context pi4j, int address, long debounce) {
        this.digitalInput = pi4j.create(buildDigitalInputConfig(pi4j, address, debounce));
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
     * @return Mapped touch state
     */
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
