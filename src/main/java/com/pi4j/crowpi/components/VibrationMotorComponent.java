package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of the CrowPi vibration motor using GPIO with Pi4J
 */
public class VibrationMotorComponent extends Component {
    /**
     * Pi4J digital output instance used by this component
     */
    protected final DigitalOutput digitalOutput;
    /**
     * If no pin is specified by the user, the default BCM pin is used.
     */
    protected static final int DEFAULT_PIN = 27;

    /**
     * Creates a new vibration motor component using the default pin.
     *
     * @param pi4j Pi4J context
     */
    public VibrationMotorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN);
    }

    /**
     * Creates a new vibration motor component with a custom BCM pin.
     *
     * @param pi4j Pi4J context
     * @param pin  Custom BCM pin address
     */
    public VibrationMotorComponent(Context pi4j, int pin) {
        this.digitalOutput = pi4j.create(buildDigitalOutputConfig(pi4j, pin));
    }

    /**
     * Set the vibration motor on or off depending on the boolean argument.
     *
     * @param on Sets the relay to on (true) or off (false)
     */
    public void setState(boolean on) {
        digitalOutput.setState(on);
    }

    /**
     * Sets the vibration motor to on.
     */
    public void on() {
        digitalOutput.on();
    }

    /**
     * Sets the vibration motor to off
     */
    public void off() {
        digitalOutput.off();
    }

    /**
     * Toggle the vibration motor state depending on its current state.
     *
     * @return Return true or false according to the new state of the vibration motor.
     */
    public boolean toggle() {
        digitalOutput.toggle();
        return digitalOutput.isOn();
    }

    /**
     * Enables the vibration motor a specified time in milliseconds
     *
     * @param interval How long the vibration motor is enabled in millis
     */
    public void pulse(int interval) {
        digitalOutput.pulse(interval, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the instance of the digital output
     *
     * @return DigitalOutput instance of the vibration motor
     */
    protected DigitalOutput getDigitalOutput() {
        return digitalOutput;
    }

    /**
     * Configure Digital Input
     *
     * @param pi4j    PI4J Context
     * @param address GPIO Address of vibration motor
     * @return Return Digital Input configuration
     */
    protected DigitalOutputConfig buildDigitalOutputConfig(Context pi4j, int address) {
        return DigitalOutput.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("Vibration Motor")
            .address(address)
            .shutdown(DigitalState.LOW)
            .build();
    }
}
