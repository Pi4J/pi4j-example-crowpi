package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VibrationMotorTest extends ComponentTest {
    protected VibrationMotorComponent vibrationMotor;
    protected DigitalOutput digitalOutput;

    @BeforeEach
    public void setUp() {
        this.vibrationMotor = new VibrationMotorComponent(pi4j);
        this.digitalOutput = vibrationMotor.getDigitalOutput();
    }

    @Test
    public void testCustomBusConfigurationSet() {
        // given
        VibrationMotorComponent testObject = new VibrationMotorComponent(pi4j, 99);

        // when
        digitalOutput = testObject.getDigitalOutput();

        // then
        assertEquals(99, digitalOutput.getAddress());
    }

    @Test
    public void testVibrationMotorOn() {
        // when
        vibrationMotor.on();

        // then
        assertEquals(DigitalState.HIGH, digitalOutput.state());
    }

    @Test
    public void testVibrationMotorOff() {
        // when
        vibrationMotor.off();

        // then
        assertEquals(DigitalState.LOW, digitalOutput.state());
    }

    @Test
    public void testVibrationMotorToggle() {
        // given
        vibrationMotor.off();

        // when
        var result = vibrationMotor.toggle();

        // then
        Assertions.assertTrue(result);
        assertEquals(DigitalState.HIGH, digitalOutput.state());
    }

    @Test
    public void testVibrationMotorSetState() {
        // when
        vibrationMotor.setState(true);

        // then
        assertEquals(DigitalState.HIGH, digitalOutput.state());

        // when
        vibrationMotor.setState(false);

        // then
        assertEquals(DigitalState.LOW, digitalOutput.state());
    }

    @Test
    public void testVibrationMotorPulse() {
        // when
        vibrationMotor.pulse(500);

        // then
        assertEquals(DigitalState.LOW, digitalOutput.state());
    }
}
