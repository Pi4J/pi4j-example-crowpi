package com.pi4j.crowpi.components.internal;

import com.pi4j.crowpi.components.Component;
import com.pi4j.io.i2c.I2C;

import java.util.Arrays;

/**
 * Implementation of HT16K33 LED driver chip used for segment displays.
 * Uses I2C via Pi4J for controlling the chip programmatically.
 */
public class HT16K33 extends Component {
    /**
     * Internal size of the buffer which gets flushed to the display.
     */
    private static final int BUFFER_SIZE = 16;

    // HT16K33: Display Data Command
    private static final int CMD_DISPLAY_DATA = 0b0000;

    // HT16K33: Dimming Set Command
    private static final int CMD_DIMMING_SET = 0b1110;

    // HT16K33: System Setup Command
    private static final int CMD_SYSTEM_SETUP = 0b0010;
    private static final int SET_OSCILLATOR_ON = 0b0001;
    private static final int SET_OSCILLATOR_OFF = 0b0000;

    // HT16K33: Display Setup Command
    private static final int CMD_DISPLAY_SETUP = 0b1000;
    private static final int SET_DISPLAY_ON = 0b0001;
    private static final int SET_DISPLAY_OFF = 0b0000;

    /**
     * Internal buffer where all digits get stored before being flushed to the display.
     */
    protected final byte[] buffer = new byte[BUFFER_SIZE];
    protected final I2C i2c;

    /**
     * Creates a new HT16K33 instance using the given I2C instance from Pi4J.
     *
     * @param i2c I2C instance
     */
    public HT16K33(I2C i2c) {
        this.i2c = i2c;
    }

    /**
     * Clears the internal buffer without refreshing the display.
     * This means that the current contents of the displays are still being shown until {@link #refresh()} is called.
     */
    public void clear() {
        Arrays.fill(buffer, (byte) 0);
    }

    /**
     * Flushes the internal buffer to the chip, causing it to be displayed.
     * The contents of the buffer will be preserved by this command.
     */
    public void refresh() {
        execute(CMD_DISPLAY_DATA, 0);
        i2c.writeRegister(0, buffer, BUFFER_SIZE);
    }

    /**
     * Specifies if the seven-segment display should be enabled or disabled.
     * This will activate/deactivate the internal system oscillator and turn the LEDs on/off.
     *
     * @param enabled Display state
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            execute(CMD_SYSTEM_SETUP, SET_OSCILLATOR_ON);
            execute(CMD_DISPLAY_SETUP, SET_DISPLAY_ON);
        } else {
            execute(CMD_SYSTEM_SETUP, SET_OSCILLATOR_OFF);
            execute(CMD_DISPLAY_SETUP, SET_DISPLAY_OFF);
        }
    }

    /**
     * Changes the desired blink rate for the seven-segment display.
     * This method expects an integer value within the range 0-3, with 0 being equal to no blinking and 3 being the fastest choice.
     * The whole display is affected by this command which gets immediately applied.
     *
     * @param rate Desired blink rate from 0-3
     */
    public void setBlinkRate(int rate) {
        if (rate < 0 || rate > 3)
            throw new IllegalArgumentException("Blink rate must be an integer in the range 0-3");
        execute(CMD_DISPLAY_SETUP, SET_DISPLAY_ON | (rate << 1));
    }

    /**
     * Changes the desired brightness for the seven-segment display.
     * This method expects an integer value within the range 0-15, with 0 being the dimmest and 15 the brightest possible value.
     * The whole display is affected by this command which gets immediately applied.
     *
     * @param brightness Desired brightness from 0-15
     */
    public void setBrightness(int brightness) {
        if (brightness < 0 || brightness > 15)
            throw new IllegalArgumentException("Brightness must be an integer in the range 0-15");
        execute(CMD_DIMMING_SET, brightness);
    }

    /**
     * Helper method for sending a command to the HT16K33 chip. Communication with the chip happens by sending a NULL value to a given
     * address. The address itself consists of a byte split in two, with the upper half/nibble being the command and the lower half/nibble
     * being the desired data / setting for the command.
     *
     * @param command Command to be executed
     * @param setting Optional setting / data for the selected command
     */
    private void execute(int command, int setting) {
        if (command < 0 || command > 0xF)
            throw new IllegalArgumentException("Command must be nibble");
        if (setting < 0 || setting > 0xF)
            throw new IllegalArgumentException("Setting must be nibble");

        final var address = (command << 4) | setting;
        i2c.writeRegister(address, 0);
    }
}
