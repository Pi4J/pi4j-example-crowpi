package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.ComponentTest;
import com.pi4j.io.i2c.I2C;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LightSensorComponentTest extends ComponentTest {
    protected LightSensorComponent lightSensor;
    protected I2C i2c;

    @Before
    public void setUp() {
        this.lightSensor = new LightSensorComponent(pi4j);
        this.i2c = lightSensor.getI2C();
    }

    @Test
    public void testCustomBusConfigurationSet () {
        // given
        LightSensorComponent testObject = new LightSensorComponent(pi4j, 10, 0x5c);

        // when
        i2c = testObject.getI2C();

        // then
        Assert.assertEquals(10, i2c.getBus());
        Assert.assertEquals(0x5c, i2c.getDevice());
    }

    @Test
    public void testCustomDeviceConfigurationSet () {
        // given
        LightSensorComponent testObject = new LightSensorComponent(pi4j, 1, 0x99);

        // when
        i2c = testObject.getI2C();

        // then
        Assert.assertEquals(1, i2c.getBus());
        Assert.assertEquals(0x99, i2c.getDevice());
    }

    @Test
    public void testMeasurementToLuxDefaultFactor() {
        // given
        double measuredValue = 123.5;

        // when
        double result = lightSensor.calculateLux(measuredValue);

        // then
        Assert.assertEquals(102.92, result, 0.1);
    }
}
