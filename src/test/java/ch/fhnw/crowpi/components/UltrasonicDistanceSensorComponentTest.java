package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.ComponentTest;
import ch.fhnw.crowpi.components.exceptions.MeasurementException;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UltrasonicDistanceSensorComponentTest extends ComponentTest {
    protected UltrasonicDistanceSensorComponent distanceSensor;
    protected DigitalInput echo;
    protected DigitalOutput trigger;

    @BeforeEach
    void setUp() {
        this.distanceSensor = new UltrasonicDistanceSensorComponent(pi4j);
        this.echo = distanceSensor.getDigitalInputEcho();
        this.trigger = distanceSensor.getDigitalOutputTrigger();
    }

    @ParameterizedTest
    @CsvSource({
            "-20.0, 9.58",
            "-10.0, 9.76",
            "00.0, 9.94",
            "10.0, 10.13",
            "20.0, 10.30",
            "30.0, 10.49",
            "40.0, 10.67"
    })
    void testDistanceCalculationDifferentTemperatures(double temperature, double expected) {
        // given
        double pulseLength = 0.6;

        // when
        var result = distanceSensor.calculateDistance(pulseLength, temperature);

        // then
        assertEquals(expected, result, 0.01);
    }

    @ParameterizedTest
    @CsvSource({
            "0.1, 1.72",
            "0.5, 8.59",
            "1.0, 17.18",
            "3.0, 51.53",
            "5.0, 85.88",
            "10.0, 171.75",
            "15, 257.63"
    })
    void testDistanceCalculationDifferentPulses(double pulseLength, double expected) {
        // given
        double temperature = 20.0;

        // when
        var result = distanceSensor.calculateDistance(pulseLength, temperature);

        // then
        assertEquals(expected, result, 0.01);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-25, 42})
    void testDistanceCalculationInvalidTemperature(double temperature) {
        // given
        double pulseLength = 0.6;

        // when
        assertThrows(IllegalArgumentException.class, () -> {
            distanceSensor.calculateDistance(pulseLength, temperature);
        });
    }

    @Test
    void testInvalidMeasurementCalculation() {
        // given
        double temperature = 20.0;
        double pulseLength = 200.0;

        // when + then
        assertThrows(MeasurementException.class, () -> {
            distanceSensor.calculateDistance(pulseLength, temperature);
        });
    }
}