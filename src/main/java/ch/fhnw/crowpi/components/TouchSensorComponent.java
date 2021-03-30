package ch.fhnw.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

import java.util.function.Consumer;

public class TouchSensorComponent {
    protected final DigitalInput din;
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
        this.din = pi4j.create(buildDigitalInputConfig(pi4j, address, debounce));
    }

    /**
     * Method to check if touch sensor is currently touched
     *
     * @return true if touch sensor is currently touched
     */
    public boolean isTouched() {
        return din.state().isHigh();
    }

    /**
     * Read current state of touch sensor
     *
     * @return Returns DigitalState of touch sensor (invalid, high, low)
     */
    public DigitalState getState() {
        return din.state();
    }

    /**
     * Create event listener on touch sensor
     *
     * @param onTouched provide an consumer with runs when an event is fired
     * @return returns created listener object. This object is needed to remove the listener afterwards
     */
    public Object addListener(Consumer<DigitalState> onTouched) {
        DigitalStateChangeListener digitalStateChangeListener = createStateChangeListener(onTouched);
        din.addListener(digitalStateChangeListener);

        return digitalStateChangeListener;
    }

    /**
     * Remove a before created event listener.
     *
     * @param stateChangeListenerObject Needs the listener object which is returned when creating a listener
     */
    public void removeListener(Object stateChangeListenerObject) {
        din.removeListener((DigitalStateChangeListener) stateChangeListenerObject);
    }

    /**
     * Encapsulate pi4j event listener objects
     *
     * @param consumer action with is executed when event is fired
     * @return listener which can be attached to digital input
     */
    protected DigitalStateChangeListener createStateChangeListener(Consumer<DigitalState> consumer) {
        return event -> consumer.accept(event.state());
    }

    /**
     * Get current Digital Input instance
     *
     * @return return digital input instance
     */
    protected DigitalInput getDigitalInput() {
        return din;
    }

    /**
     * Configure Digital Input
     *
     * @param pi4j PI4J Context
     * @param address GPIO Address of touch sensor
     * @param debounce debounce time in microseconds
     * @return return Digital Input configuration
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
}
