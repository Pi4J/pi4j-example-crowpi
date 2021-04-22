package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.internal.MCP23008;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.io.pwm.Pwm;

/**
 * Implementation of the CrowPi Relay
 */
public class RelayComponent extends Component {
    /**
     * Pi4J digital output instance used by this component
     */
    protected final DigitalOutput digitalOutput;
    /**
     * If no pin is specified by the user, the default BCM pin 18 is used.
     */
    protected static final int DEFAULT_PIN = 21;

    /**
     * Creates a new buzzer component using the default pin.
     *
     * @param pi4j Pi4J context
     */
    public RelayComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN);
    }

    /**
     * Creates a new buzzer component with a custom BCM pin.
     *
     * @param pi4j    Pi4J context
     * @param address Custom BCM pin address
     */
    public RelayComponent(Context pi4j, int address) {
        this.digitalOutput = pi4j.create(buildDigitalOutputConfig(pi4j, address));
    }

    /**
     * Set the Relay on or off depending on the boolean argument.
     *
     * @param on Sets the relay to on (true) or off (false)
     */
    public void setState(boolean on) {
        digitalOutput.setState(!on);
    }

    /**
     * Sets the relay to on.
     */
    public void setStateOn() {
        digitalOutput.off();
    }

    /**
     * Sets the relay to off
     */
    public void setStateOff() {
        digitalOutput.on();
    }

    /**
     * Toggle the relay state depending on its current state.
     *
     * @return Return true or false according to the new state of the relay.
     */
    public boolean toggleState() {
        digitalOutput.toggle();
        return digitalOutput.isOff();
    }

    /**
     * Configure Digital Input
     *
     * @param pi4j    PI4J Context
     * @param address GPIO Address of touch sensor
     * @return Return Digital Input configuration
     */
    protected DigitalOutputConfig buildDigitalOutputConfig(Context pi4j, int address) {
        return DigitalOutput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("Relay")
            .address(address)
            .build();
    }
}
