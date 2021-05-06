package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RelayComponentTest extends ComponentTest {
    protected RelayComponent relay;
    protected DigitalOutput digitalOutput;

    @BeforeEach
    public void setUp() {
        this.relay = new RelayComponent(pi4j);
        this.digitalOutput = relay.getDigitalOutput();
    }

    @Test
    public void testCustomBusConfigurationSet() {
        // given
        RelayComponent testObject = new RelayComponent(pi4j, 99);

        // when
        digitalOutput = testObject.getDigitalOutput();

        // then
        assertEquals(99, digitalOutput.getAddress());
    }

    @Test
    public void testSetRelayOn() {
        // when
        relay.setStateOn();

        // then
        assertEquals(DigitalState.LOW, digitalOutput.state());
    }

    @Test
    public void testSetRelayOff() {
        // when
        relay.setStateOff();

        // then
        assertEquals(DigitalState.HIGH, digitalOutput.state());
    }

    @Test
    public void testToggleRelay() {
        // given
        relay.setStateOff();

        // when
        var result = relay.toggleState();

        // then
        Assertions.assertTrue(result);
        assertEquals(DigitalState.LOW, digitalOutput.state());
    }

    @Test
    public void testSetRelayState() {
        // when
        relay.setState(true);

        // then
        assertEquals(DigitalState.LOW, digitalOutput.state());

        // when
        relay.setState(false);

        // then
        assertEquals(DigitalState.HIGH, digitalOutput.state());
    }
}
