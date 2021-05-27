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

public class SoundSensorComponentTest extends ComponentTest {
    protected SoundSensorComponent soundSensor;
    protected DigitalInput din;

    @BeforeEach
    public void setUp() {
        soundSensor = new SoundSensorComponent(pi4j);
        din = soundSensor.getDigitalInput();
    }

    @Test
    public void testGetSensorState() {
        // when
        final var resultState = soundSensor.getState();

        // then
        Assertions.assertEquals(SoundSensorComponent.SoundState.SILENT, resultState);
    }

    @Test
    public void testIsNoise() {
        // given
        boolean result;

        // when
        result = soundSensor.isSilent();

        // then
        assertEquals(din.isHigh(), result);
    }

    @ParameterizedTest
    @CsvSource({
        "UNKNOWN,UNKNOWN",
        "LOW,SILENT",
        "HIGH,NOISE"
    })
    void testMapDigitalState(DigitalState digitalState, SoundSensorComponent.SoundState expected) {
        // when
        final var actual = soundSensor.mapDigitalState(digitalState);

        // then
        assertEquals(expected, actual);
    }
}
