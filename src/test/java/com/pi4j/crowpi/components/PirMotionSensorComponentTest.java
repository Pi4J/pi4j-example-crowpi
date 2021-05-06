package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PirMotionSensorComponentTest extends ComponentTest {
    protected PirMotionSensorComponent motionSensor;
    protected DigitalInput digitalInput;

    @BeforeEach
    void setUp() {
        this.motionSensor = new PirMotionSensorComponent(pi4j);
        this.digitalInput = motionSensor.getDigitalInput();
    }

    @ParameterizedTest
    @CsvSource({
        "UNKNOWN,UNKNOWN",
        "LOW,STILLSTAND",
        "HIGH,MOVEMENT"
    })
    void testMapDigitalState(DigitalState digitalState, PirMotionSensorComponent.MotionState expected) {
        // when
        final var actual = motionSensor.mapDigitalState(digitalState);

        // then
        assertEquals(expected, actual);
    }
}
