package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.events.DigitalEventProvider;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Implementation of the CrowPi PIR motion sensor using GPIO with Pi4J
 */
public class PirMotionSensorComponent implements DigitalEventProvider<PirMotionSensorComponent.MotionState> {
    /**
     * Pi4J digital input instance used by this component
     */
    protected final DigitalInput digitalInput;

    /**
     * Default BCM pin of PIR motion sensor for CrowPi
     */
    protected static final int DEFAULT_PIN = 23;

    /**
     * Creates a new PIR motion sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public PirMotionSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN);
    }

    /**
     * Creates a new PIR motion sensor component with custom GPIO address.
     *
     * @param pi4j    Pi4J context
     * @param address GPIO address of PIR motion sensor
     */
    public PirMotionSensorComponent(Context pi4j, int address) {
        this.digitalInput = pi4j.create(buildDigitalInputConfig(pi4j, address));
    }

    /**
     * Returns the current state of the PIR motion sensor.
     *
     * @return Current motion sensor state
     */
    public MotionState getState() {
        return mapDigitalState(digitalInput.state());
    }

    /**
     * Checks if the PIR motion sensor currently detects movement.
     *
     * @return True if movement was detected
     */
    public boolean hasMovement() {
        return getState() == MotionState.MOVEMENT;
    }

    /**
     * Checks if the PIR motion sensor currently detects stillstand.
     *
     * @return True if stillstand was detected
     */
    public boolean hasStillstand() {
        return getState() == MotionState.STILLSTAND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DigitalInput getDigitalInput() {
        return digitalInput;
    }

    /**
     * Maps a {@link DigitalState} to a well-known {@link MotionState}
     *
     * @param digitalState Pi4J digital state to map
     * @return Mapped motion state
     */
    @Override
    public MotionState mapDigitalState(DigitalState digitalState) {
        switch (digitalState) {
            case HIGH:
                return MotionState.MOVEMENT;
            case LOW:
                return MotionState.STILLSTAND;
            case UNKNOWN:
            default:
                return MotionState.UNKNOWN;
        }
    }

    /**
     * Builds a new DigitalInput instance for the PIR motion sensor.
     *
     * @param pi4j    Pi4J context
     * @param address GPIO address of PIR motion sensor
     * @return DigitalInput configuration
     */
    protected DigitalInputConfig buildDigitalInputConfig(Context pi4j, int address) {
        return DigitalInput.newConfigBuilder(pi4j)
            .id("BCM-" + address)
            .name("PIR Motion Sensor")
            .address(address)
            .pull(PullResistance.PULL_DOWN)
            .build();
    }

    /**
     * All available states reported by the PIR motion sensor component.
     */
    public enum MotionState {
        MOVEMENT,
        STILLSTAND,
        UNKNOWN
    }
}
