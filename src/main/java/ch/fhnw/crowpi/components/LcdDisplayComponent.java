package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.events.DigitalEventProvider;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;


public class LcdDisplayComponent {
    private final I2C i2c;

    /**
     * Those default address are to use this class with default CrowPi setup
     */
    private static final int DEFAULT_BUS = 0x1;
    private static final int DEFAULT_DEVICE = 0x21;

    /**
     * Creates a new light sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public LcdDisplayComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new light sensor component with custom bus, device address
     *
     * @param pi4j   Pi4J context
     * @param bus    Custom I2C bus address
     * @param device Custom device address on I2C
     */
    public LcdDisplayComponent(Context pi4j, int bus, int device) {
        this.i2c = pi4j.create(buildI2CConfig(pi4j, bus, device));
    }

    /**
     * Builds a new I2C instance for the seven-segment display
     *
     * @param pi4j   Pi4J context
     * @param bus    Bus address
     * @param device Device address
     * @return I2C instance
     */
    private static I2CConfig buildI2CConfig(Context pi4j, int bus, int device) {
        return I2C.newConfigBuilder(pi4j)
            .id("I2C-" + device + "@" + bus)
            .name("LCD Display")
            .bus(bus)
            .device(device)
            .build();
    }
}
