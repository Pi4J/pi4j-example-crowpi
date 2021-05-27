package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class SoundSensorComponentTest extends ComponentTest {
    protected SoundSensorComponent soundSensor;
    protected MockDigitalInput din;

    @BeforeEach
    public void setUp() {
        soundSensor = new SoundSensorComponent(pi4j);
        din = toMock(soundSensor.getDigitalInput());
    }

    @Test
    public void testGetSensorState() {
        // given
        din.mockState(DigitalState.HIGH);

        // when
        final var resultState = soundSensor.getState();

        // then
        Assertions.assertEquals(SoundSensorComponent.SoundState.SILENT, resultState);
    }

    @Test
    public void testNoise() {
        // given
        din.mockState(DigitalState.LOW);

        // when
        final boolean isSilent = soundSensor.isSilent();
        final boolean isNoisy = soundSensor.isNoisy();

        // then
        assertFalse(isSilent);
        assertTrue(isNoisy);
    }

    @Test
    public void testSilence() {
        // given
        din.mockState(DigitalState.HIGH);

        // when
        final boolean isSilent = soundSensor.isSilent();
        final boolean isNoisy = soundSensor.isNoisy();

        // then
        assertTrue(isSilent);
        assertFalse(isNoisy);
    }

    @ParameterizedTest
    @CsvSource({
        "UNKNOWN,UNKNOWN",
        "HIGH,SILENT",
        "LOW,NOISE"
    })
    void testMapDigitalState(DigitalState digitalState, SoundSensorComponent.SoundState expected) {
        // when
        final var actual = soundSensor.mapDigitalState(digitalState);

        // then
        assertEquals(expected, actual);
    }
}
