package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TouchSensorComponentTest extends ComponentTest {
    protected TouchSensorComponent touchSensor;
    protected DigitalInput din;

    @BeforeEach
    public void setUp() {
        touchSensor = new TouchSensorComponent(pi4j);
        din = touchSensor.getDigitalInput();
    }

    @Test
    public void testGetSensorState() {
        // given
        TouchSensorComponent.TouchState resultState;

        // when
        resultState = touchSensor.getState();

        // then
        Assertions.assertEquals(TouchSensorComponent.TouchState.UNTOUCHED, resultState);
    }

    @Test
    public void testIsTouched() {
        // given
        boolean result;

        // when
        result = touchSensor.isTouched();

        // then
        assertEquals(din.isHigh(), result);
    }

    @ParameterizedTest
    @CsvSource({
        "UNKNOWN,UNKNOWN",
        "LOW,UNTOUCHED",
        "HIGH,TOUCHED"
    })
    void testMapDigitalState(DigitalState digitalState, TouchSensorComponent.TouchState expected) {
        // when
        final var actual = touchSensor.mapDigitalState(digitalState);

        // then
        assertEquals(expected, actual);
    }
}
