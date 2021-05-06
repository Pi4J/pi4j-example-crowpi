package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.crowpi.components.TiltSensorComponent.TiltState;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TiltSensorComponentTest extends ComponentTest {
    protected TiltSensorComponent tiltSensor;
    protected DigitalInput digitalInput;

    @BeforeEach
    void setUp() {
        this.tiltSensor = new TiltSensorComponent(pi4j);
        this.digitalInput = tiltSensor.getDigitalInput();
    }

    @ParameterizedTest
    @CsvSource({
        "UNKNOWN,UNKNOWN",
        "LOW,RIGHT",
        "HIGH,LEFT"
    })
    void testMapDigitalState(DigitalState digitalState, TiltState expected) {
        // when
        final var actual = tiltSensor.mapDigitalState(digitalState);

        // then
        assertEquals(expected, actual);
    }
}
