package com.pi4j.crowpi.components.internal;

import com.pi4j.crowpi.components.Component;
import com.pi4j.io.spi.Spi;

import java.util.Arrays;

/**
 * Implementation of MAX7219 driver chip used for 8x8 LED matrix displays.
 * Uses SPI via Pi4J for controlling the chip programmatically.
 */
public class MAX7219 extends Component {
    // MAX7219: Internal Commands
    private static final byte CMD_SET_FIRST_ROW = 0x01;
    private static final byte CMD_DECODE_MODE = 0x09;
    private static final byte CMD_INTENSITY = 0x0A;
    private static final byte CMD_SCAN_LIMIT = 0x0B;
    private static final byte CMD_SHUTDOWN = 0x0C;
    private static final byte CMD_DISPLAY_TEST = 0x0F;

    /**
     * Width of MAX7219 LED matrix
     */
    public static final int WIDTH = 8;

    /**
     * Height of MAX7219 LED matrix
     */
    public static final int HEIGHT = 8;

    /**
     * Internal buffer to store the 8x8 matrix
     * A byte[] array is used as each of the 8 bits is used to represent a column
     */
    protected final byte[] buffer = new byte[HEIGHT];

    /**
     * Pi4J SPI instance
     */
    protected final Spi spi;

    /**
     * Creates a new MAX7219 instance using the given SPI instance from Pi4J.
     *
     * @param spi SPI instance
     */
    public MAX7219(Spi spi) {
        this.spi = spi;
    }

    /**
     * Clears the internal buffer without refreshing the display.
     * This means that the current contents of the displays are still being shown until {@link #refresh()} is called.
     */
    public void clear() {
        Arrays.fill(buffer, (byte) 0);
    }

    /**
     * Flushes the internal buffer for all rows to the chip, causing it to be displayed.
     * The contents of the buffer will be preserved by this command.
     */
    public void refresh() {
        for (int row = 0; row < HEIGHT; row++) {
            refreshRow(row);
        }
    }

    /**
     * Flushes the internal buffer for a single row to the chip, causing it to be displayed.
     * The contents of the buffer will be preserved by this command.
     *
     * @param row Row to be flushed
     */
    protected void refreshRow(int row) {
        if (row < 0 || row >= HEIGHT) {
            throw new IllegalArgumentException("Row must be an integer in the range 0-" + HEIGHT);
        }

        execute((byte) (CMD_SET_FIRST_ROW + row), buffer[row]);
    }

    /**
     * Specifies if the LED matrix should be enabled or disabled.
     * This will also setup the proper decoding mode and scan limit when enabling the chip.
     *
     * @param enabled LED matrix state (true = ON, false = OFF)
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            execute(CMD_SHUTDOWN, (byte) 0x01);
            execute(CMD_DECODE_MODE, (byte) 0x00);
            execute(CMD_SCAN_LIMIT, (byte) 0x07);
        } else {
            execute(CMD_SHUTDOWN, (byte) 0x00);
        }
    }

    /**
     * Enables or disables the testing mode of the LED matrix.
     * When enabled, all other options (including {@link #setEnabled(boolean)} are ignored and all LEDs are turned on.
     * To actually control the chip, the test mode MUST be disabled.
     *
     * @param enabled Test mode state (true = ON, false = OFF)
     */
    public void setTestMode(boolean enabled) {
        execute(CMD_DISPLAY_TEST, (byte) (enabled ? 0x01 : 0x00));
    }

    /**
     * Changes the desired brightness for the LED matrix.
     * This method expects an integer value within the range 0-15, with 0 being the dimmest and 15 the brightest possible value.
     * The whole display is affected by this command which gets immediately applied.
     *
     * @param brightness Desired brightness from 0-15
     */
    public void setBrightness(int brightness) {
        if (brightness < 0 || brightness > 15) {
            throw new IllegalArgumentException("Brightness must be an integer in the range 0-15");
        }
        execute(CMD_INTENSITY, (byte) brightness);
    }

    /**
     * Enables or disables the pixel at the given X/Y position within the internal buffer.
     * This change will not be visible until {@link #refresh()} or {@link #refreshRow(int)} gets called.
     *
     * @param x       X position to change
     * @param y       Y position to change
     * @param enabled Desired pixel state (true = ON, false = OFF)
     */
    public void setPixel(int x, int y, boolean enabled) {
        // Ensure coordinates are within boundaries
        checkPixelBounds(x, y);

        // Generate bitmask and set/unset specific bit
        final byte mask = (byte) (1 << (WIDTH - 1 - x));
        if (enabled) {
            buffer[y] |= mask;
        } else {
            buffer[y] &= ~mask;
        }
    }

    /**
     * Retrieves the pixel at the given X/Y position within the internal buffer.
     *
     * @param x X position to change
     * @param y Y position to change
     * @return Current state of specified pixel (true = ON, false = OFF)
     */
    public boolean getPixel(int x, int y) {
        // Ensure coordinates are within boundaries
        checkPixelBounds(x, y);

        // Generate bitmask and retrieve specific bit
        final byte mask = (byte) (1 << (WIDTH - 1 - x));
        return (buffer[y] & mask) != 0;
    }

    /**
     * Ensures the given X and Y coordinates are within the boundaries of this LED matrix.
     * An {@link IllegalArgumentException} will be thrown if outside.
     *
     * @param x X coordinate to check
     * @param y Y coordinate to check
     */
    private void checkPixelBounds(int x, int y) {
        if (x < 0 || x >= WIDTH) {
            throw new IllegalArgumentException("X must be an integer in the range 0-" + WIDTH);
        }
        if (y < 0 || y >= WIDTH) {
            throw new IllegalArgumentException("Y must be an integer in the range 0-" + HEIGHT);
        }
    }

    /**
     * Helper method for sending a command to the MAX7219 chip with data. Communication happens over SPI by simply sending two pieces of
     * data, more specifically the desired command as a byte value, followed by the data as another byte value.
     *
     * @param command Command to be executed
     * @param data    Data for the given command
     */
    private void execute(byte command, byte data) {
        spi.write(command, data);
    }
}
