package com.pi4j.crowpi.components.internal;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.Component;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

/**
 * Class to provide IO interfacing with MCP23008
 */
public class MCP23008 extends Component {
    /**
     * Instance of I2C Bus
     */
    private final I2C i2c;
    /**
     * Those default address are to use this class with default CrowPi setup
     */
    private static final int DEFAULT_BUS = 0x1;
    private static final int DEFAULT_DEVICE = 0x21;
    /**
     * IODIR register controls the direction of the GPIO on the port expander
     */
    private static final byte IODIR_REGISTER_ADDRESS = 0x00;
    /**
     * GPIO register is used to read the pins input
     */
    private static final byte GPIO_REGISTER_ADDRESS = 0x09; //
    /**
     * Current IO States of the MCP23008
     */
    protected byte gpioState = 0x00;

    /**
     * Creates a new MCP using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public MCP23008(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new MCP with custom bus, device address
     *
     * @param pi4j   Pi4J context
     * @param bus    Custom I2C bus address
     * @param device Custom device address on I2C
     */
    public MCP23008(Context pi4j, int bus, int device) {
        this.i2c = pi4j.create(buildI2CConfig(pi4j, bus, device));
    }

    /**
     * Configure the IO's of the MCP23008. Every bit of the input byte represents one of the IO PIN.
     * 0 = Output, 1 = Input
     *
     * @param ioConfiguration Configure the Pin's of the MCP as Input (1) or Output(0)
     */
    public void initializeIo(byte ioConfiguration) {
        i2c.writeRegister(IODIR_REGISTER_ADDRESS, ioConfiguration);
    }

    /**
     * Pulse a IO Pin
     *
     * @param bit        Number of the Pin
     * @param pulseWidth Time to Pulse the Output in Millis
     */
    public void pulsePin(int bit, int pulseWidth) {
        checkPinNumber(bit);
        setAndWritePin(bit, false);
        sleep(1);
        setAndWritePin(bit, true);
        sleep(pulseWidth);
        setAndWritePin(bit, false);
    }

    /**
     * Set the state of a pin to high or low. Pin needs manually write out to the hardware after setting
     *
     * @param bit   Number of the Pin
     * @param state Set the Pin to this state
     */
    public void setPin(int bit, boolean state) {
        checkPinNumber(bit);

        if (state) {
            gpioState |= (1 << bit);
        } else {
            gpioState &= ~(1 << bit);
        }
    }

    /**
     * Set a Pin an directly write it out to the hardware
     *
     * @param bit   Number of the Pin
     * @param state State which the Pin is set and written to
     */
    public void setAndWritePin(int bit, boolean state) {
        checkPinNumber(bit);
        setPin(bit, state);
        writePins();
    }

    /**
     * Write the Buffer out to the Pins
     */
    public void writePins() {
        i2c.writeRegister(GPIO_REGISTER_ADDRESS, gpioState);
    }

    /**
     * Checks a bit is in Range of 0-7. So it is an IO Pin
     *
     * @param bit Number to check
     */
    protected void checkPinNumber(int bit) {
        if (bit > 7 || bit < 0) {
            throw new IllegalArgumentException("Invalid Pin Number");
        }
    }

    /**
     * Returns the created i2c instance for the mcp
     *
     * @return I2C instance
     */
    public I2C getI2C() {
        return this.i2c;
    }

    /**
     * Returns the created gpio state byte for the mcp
     *
     * @return I2C instance
     */
    public byte getGpioState() {
        return this.gpioState;
    }

    /**
     * Builds a new I2C instance for the MCP23008
     *
     * @param pi4j   Pi4J context
     * @param bus    Bus address
     * @param device Device address
     * @return I2C instance
     */
    private static I2CConfig buildI2CConfig(Context pi4j, int bus, int device) {
        return I2C.newConfigBuilder(pi4j)
            .id("I2C-" + device + "@" + bus)
            .name("MCP23008")
            .bus(bus)
            .device(device)
            .build();
    }
}
