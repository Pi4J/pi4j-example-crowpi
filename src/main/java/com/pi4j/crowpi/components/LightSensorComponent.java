package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

/**
 * Implementation of the CrowPi light sensor using I2C with Pi4J
 */
public class LightSensorComponent extends Component {
    private final I2C i2c;

    /**
     * Those default address are to use this class with default CrowPi setup
     */
    private static final int DEFAULT_BUS = 0x1;
    private static final int DEFAULT_DEVICE = 0x5c;
    /**
     * Define the factor which is used to calculate lux from the measurement value. BH1750 = 1.2
     */
    private static final double MEASUREMENT_TO_LUX_FACTOR = 1.2;

    // Start measurement at 1lx resolution. Time typically 120ms
    private static final int ONE_TIME_HIGH_RES_MODE_1 = 0x20;
    // Start measurement at 0.5lx resolution. Time typically 120ms
    private static final int ONE_TIME_HIGH_RES_MODE_2 = 0x21;
    // Start measurement at 1lx resolution. Time typically 120ms
    private static final int ONE_TIME_LOW_RES_MODE = 0x23;

    /**
     * Creates a new light sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public LightSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new light sensor component with custom bus, device address
     *
     * @param pi4j   Pi4J context
     * @param bus    Custom I2C bus address
     * @param device Custom device address on I2C
     */
    public LightSensorComponent(Context pi4j, int bus, int device) {
        this.i2c = pi4j.create(buildI2CConfig(pi4j, bus, device));
    }

    /**
     * Read current light intensity in lux with custom resolution settings
     *
     * @param resolution Defines resolution of measurements: 0 = 4lx Resolution / 1 = 1lx Resolution / 2 = 0.5lx Resolution
     * @return Light intensity in lux
     */
    public double readLight(int resolution) {
        if (resolution > 2 || resolution < 0) {
            throw new IllegalArgumentException("Invalid Resolution Selected");
        }

        int resolutionRegisterValue = 0;
        switch (resolution) {
            case 0:
                resolutionRegisterValue = ONE_TIME_LOW_RES_MODE;
                break;
            case 1:
                resolutionRegisterValue = ONE_TIME_HIGH_RES_MODE_1;
                break;
            case 2:
                resolutionRegisterValue = ONE_TIME_HIGH_RES_MODE_2;
                break;
        }

        return calculateLux(i2c.readRegisterWord(resolutionRegisterValue));
    }

    /**
     * Measure current light intensity with default resolution (1lx)
     *
     * @return Light intensity in lux
     */
    public double readLight() {
        return calculateLux(i2c.readRegisterWord(ONE_TIME_HIGH_RES_MODE_1));
    }

    /**
     * Calculates Lux values from measurement values
     *
     * @param measurementValue Measurement value from light sensor
     * @return Calculated value in lux
     */
    protected double calculateLux(double measurementValue) {
        return measurementValue / MEASUREMENT_TO_LUX_FACTOR;
    }

    /**
     * Returns the created PWM instance for the buzzer
     *
     * @return I2C instance
     */
    protected I2C getI2C() {
        return this.i2c;
    }

    /**
     * Build a I2C Configuration to use the light Sensor
     *
     * @param pi4j   PI4J Context
     * @param bus    I2C Bus address
     * @param device I2C Device address
     * @return I2C configuration
     */
    private static I2CConfig buildI2CConfig(Context pi4j, int bus, int device) {
        return I2C.newConfigBuilder(pi4j)
            .id("I2C-" + device + "@" + bus)
            .name("Light Sensor")
            .bus(bus)
            .device(device)
            .build();
    }
}
