package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LightSensorComponentTest extends ComponentTest {
    protected LightSensorComponent lightSensor;
    protected I2C i2c;

    @BeforeEach
    public void setUp() {
        this.lightSensor = new LightSensorComponent(pi4j);
        this.i2c = lightSensor.getI2C();
    }

    @Test
    public void testCustomBusConfigurationSet() {
        // given
        LightSensorComponent testObject = new LightSensorComponent(pi4j, 10, 0x5c);

        // when
        i2c = testObject.getI2C();

        // then
        assertEquals(10, i2c.getBus());
        assertEquals(0x5c, i2c.getDevice());
    }

    @Test
    public void testCustomDeviceConfigurationSet() {
        // given
        LightSensorComponent testObject = new LightSensorComponent(pi4j, 1, 0x99);

        // when
        i2c = testObject.getI2C();

        // then
        assertEquals(1, i2c.getBus());
        assertEquals(0x99, i2c.getDevice());
    }

    @Test
    public void testMeasurementToLuxDefaultFactor() {
        // given
        double measuredValue = 123.5;

        // when
        double result = lightSensor.calculateLux(measuredValue);

        // then
        assertEquals(102.92, result, 0.1);
    }
}
