package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.internal.HT16K33;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

import java.time.LocalTime;
import java.util.Map;

/**
 * Implementation of the CrowPi seven-segment display using I2C with Pi4J
 */
public class SevenSegmentComponent extends HT16K33 {
    /**
     * Default I2C bus address for the seven-segment display on the CrowPi
     */
    protected static final int DEFAULT_BUS = 0x1;
    /**
     * Default I2C device address for the seven-segment display on the CrowPi
     */
    protected static final int DEFAULT_DEVICE = 0x70;

    /**
     * Internal buffer index for the colon of the seven-segment display
     */
    private static final int COLON_INDEX = 4;
    /**
     * Internal buffer indices for the digits of the seven-segment display
     */
    private static final int[] DIGIT_INDICES = new int[]{0, 2, 6, 8};

    /**
     * Mapping of characters to their respective byte representation.
     * Each byte is a bitset where each bit specifies if a specific segment should be enabled (1) or disabled (0).
     */
    protected static final Map<Character, Byte> CHAR_BITSETS = Map.ofEntries(
        Map.entry(' ', fromSegments()),
        Map.entry('-', fromSegments(Segment.CENTER)),
        Map.entry('0', fromSegments(Segment.TOP, Segment.LEFT_TOP, Segment.RIGHT_TOP, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('1', fromSegments(Segment.RIGHT_TOP, Segment.RIGHT_BOTTOM)),
        Map.entry('2', fromSegments(Segment.TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.BOTTOM)),
        Map.entry('3', fromSegments(Segment.TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('4', fromSegments(Segment.LEFT_TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.RIGHT_BOTTOM)),
        Map.entry('5', fromSegments(Segment.TOP, Segment.LEFT_TOP, Segment.CENTER, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('6', fromSegments(Segment.TOP, Segment.LEFT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('7', fromSegments(Segment.TOP, Segment.RIGHT_TOP, Segment.RIGHT_BOTTOM)),
        Map.entry('8', fromSegments(Segment.LEFT_TOP, Segment.TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('9', fromSegments(Segment.TOP, Segment.LEFT_TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('A', fromSegments(Segment.TOP, Segment.LEFT_TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM)),
        Map.entry('B', fromSegments(Segment.LEFT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('C', fromSegments(Segment.TOP, Segment.LEFT_TOP, Segment.LEFT_BOTTOM, Segment.BOTTOM)),
        Map.entry('D', fromSegments(Segment.RIGHT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('E', fromSegments(Segment.TOP, Segment.LEFT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.BOTTOM)),
        Map.entry('F', fromSegments(Segment.TOP, Segment.LEFT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM))
    );

    /**
     * Creates a new seven-segment display component with the default bus and device address.
     *
     * @param pi4j Pi4J context
     */
    public SevenSegmentComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new seven-segment display component with a custom bus and device address.
     *
     * @param pi4j   Pi4J context
     * @param bus    Bus address
     * @param device Device address
     */
    public SevenSegmentComponent(Context pi4j, int bus, int device) {
        super(pi4j.create(buildI2CConfig(pi4j, bus, device)));
    }

    /**
     * Prints the given integer value to the seven-segment display.
     * This works by converting the integer into a string and passing it to {@link #print(String)}.
     * Please note that due to the limitations of the display only the first four digits will be displayed.
     * This will clear the buffer and automatically call {@link #refresh()} afterwards to immediately display the number.
     *
     * @param i Integer to display
     */
    public void print(int i) {
        print(String.valueOf(i));
    }

    /**
     * Prints the given double value to the seven-segment display, automatically setting the decimal point if applicable.
     * This works by converting the double into a string and passing it to {@link #print(String)}.
     * Please note that due to the limitations of the display only the first four digits will be displayed.
     * This will clear the buffer and automatically call {@link #refresh()} afterwards to immediately display the number.
     *
     * @param d Double to display
     */
    public void print(double d) {
        print(String.valueOf(d));
    }

    /**
     * Prints the hours and minutes of the given {@link LocalTime} instance to the seven-segment display.
     * The time will be displayed in 24 hours format as HH:MM with the colon being active for every odd second.
     * This will clear the buffer and automatically call {@link #refresh()} afterwards to immediately display the time.
     *
     * @param time Time to display
     */
    public void print(LocalTime time) {
        clear();
        setColon(time.getSecond() % 2 == 1);
        setDigit(0, time.getHour() / 10);
        setDigit(1, time.getHour() % 10);
        setDigit(2, time.getMinute() / 10);
        setDigit(3, time.getMinute() % 10);
        refresh();
    }

    /**
     * Prints the first four letters (or less if applicable) of the given string to the display. Additional letters are ignored.
     * Adding a dot after a digit or putting a colon after two digits causes the respective symbols to be displayed too.
     * This will clear the buffer and automatically call {@link #refresh()} afterwards to immediately display the text.
     * <p>
     * Example: "1.2:34." will print the string "1234" to the display, with decimal point #1 and #4 as well as the colon symbol being set
     *
     * @param s String which should be printed
     */
    public void print(String s) {
        clear();

        int idx = 0, pos = 0;
        while (idx < s.length() && pos < 4) {
            // Set digit to character at current index and advance
            setDigit(pos, s.charAt(idx++));

            // Exit early if we reached the end
            if (idx >= s.length()) {
                break;
            }

            // Set decimal point if next character is dot
            if (s.charAt(idx) == '.') {
                setDecimalPoint(pos, true);
                idx++;
            }

            // Exit early if we reached the end
            if (idx >= s.length()) {
                break;
            }

            // Advance to next digit
            pos++;

            // Set colon if next character is a colon after two digits
            if (pos == 2 && s.charAt(idx) == ':') {
                setColon(true);
                idx++;
            }
        }

        refresh();
    }

    /**
     * Enables or disables the colon symbol of the seven-segment display.
     * This will only affect the internal buffer and does not get displayed until {@link #refresh()} gets called.
     *
     * @param enabled Specify if colon should be enabled or disabled.
     */
    public void setColon(boolean enabled) {
        if (enabled) {
            buffer[COLON_INDEX] = 0x02;
        } else {
            buffer[COLON_INDEX] = 0x00;
        }
    }

    /**
     * Enables or disables the decimal point at the given digit position.
     * Please note that the decimal point is just an additional belonging to the same digit.
     * This means that overriding the specific digit (e.g. using {@link #setRawDigit(int, byte)} will reset the decimal point.
     * This will only affect the internal buffer and does not get displayed until {@link #refresh()} gets called.
     *
     * @param position Desired position of digit from 0-3.
     * @param enabled  Specify if decimal point should be enabled or disabled.
     */
    public void setDecimalPoint(int position, boolean enabled) {
        // Get current value of given digit and set the decimal point accordingly
        byte value = getRawDigit(position);
        if (enabled) {
            value |= Segment.DECIMAL_POINT.getValue();
        } else {
            value &= ~Segment.DECIMAL_POINT.getValue();
        }

        // Write updated value into digit
        setRawDigit(position, value);
    }

    /**
     * Sets the digit at the specified position to the given integer.
     * The integer must not contain more than one number, in other words it has to be in the range 0-9.
     * This will only affect the internal buffer and does not get displayed until {@link #refresh()} gets called.
     *
     * @param position Desired position of digit from 0-3.
     * @param i        Single digit number which should be displayed.
     */
    public void setDigit(int position, int i) {
        // Ensure integer consists of a single number in decimal representation
        if (i < 0 || i > 9) {
            throw new IllegalArgumentException("Digit must be an integer in the range 0-9");
        }

        // Calculate ASCII character by adding number to ASCII value of '0'
        // This works as the ASCII table contains the numbers 0-9 in ascending order
        setDigit(position, (char) ('0' + i));
    }

    /**
     * Sets the digit at the specified position to the given character.
     * The character must be representable by the seven-segment display, so only a subset of chars is supported.
     * This will only affect the internal buffer and does not get displayed until {@link #refresh()} gets called.
     *
     * @param position Desired position of digit from 0-3.
     * @param c        Character which should be displayed.
     */
    public void setDigit(int position, char c) {
        // Lookup byte value for given character
        final var value = CHAR_BITSETS.get(Character.toUpperCase(c));
        if (value == null) {
            throw new IllegalArgumentException("Character is not supported by seven-segment display");
        }

        setRawDigit(position, value);
    }

    /**
     * Sets the digit at the specified position to match the given segments.
     * This will only affect the internal buffer and does not get displayed until {@link #refresh()} gets called.
     *
     * @param position Desired position of digit from 0-3.
     * @param segments Segments which should be displayed.
     */
    public void setDigit(int position, Segment... segments) {
        setRawDigit(position, fromSegments(segments));
    }

    /**
     * Sets the raw digit at the specified position. This method will take a byte value which gets processed by the underlying chip.
     * The byte represents a bitset where each bit belongs to a specific segment and decides if its enabled (1) or disabled (0).
     * Valid values can be crafted using the {@link #fromSegments(Segment...)} method.
     * This will only affect the internal buffer and does not get displayed until {@link #refresh()} gets called.
     *
     * @param position Desired position of digit from 0-3.
     * @param value    Raw byte value to be displayed.
     */
    protected void setRawDigit(int position, byte value) {
        buffer[resolveDigitIndex(position)] = value;
    }

    /**
     * Gets the raw digit at the specified position. This method will return the internal byte value of the underlying chip.
     * The byte represents a bitset where each bit belongs to a specific segment and decides if its enabled (1) or disabled (0).
     *
     * @param position Desired position of digit from 0-3.
     * @return Raw byte value at specified position.
     */
    protected byte getRawDigit(int position) {
        return buffer[resolveDigitIndex(position)];
    }

    /**
     * Helper method for converting the human-readable position of a digit (e.g. second digit) to the actual buffer index.
     * This will throw an {@link IndexOutOfBoundsException} when the given position is outside of the known indices.
     *
     * @param position Human-readable position of digit starting at zero
     * @return Actual index of digit in the internal buffer
     */
    private int resolveDigitIndex(int position) {
        // Ensure position is within bounds
        final var maxPosition = DIGIT_INDICES.length - 1;
        if (position < 0 || position > maxPosition) {
            throw new IndexOutOfBoundsException("Digit position is outside of range 0-" + maxPosition);
        }

        // Lookup actual index based on position
        return DIGIT_INDICES[position];
    }

    /**
     * Returns the created I2C instance for the seven-segment display
     *
     * @return I2C instance
     */
    protected I2C getI2C() {
        return this.i2c;
    }

    /**
     * Helper method for creating a raw digit value (byte) from 0-n segments.
     * This can be used together with the {@link Segment} enumeration to create and display your own digits.
     * All segments passed to this method will be flagged as active and enabled when passed to {@link #setRawDigit(int, byte)}
     *
     * @param segments Segments which should be enabled to together
     * @return Raw digit value as byte
     */
    protected static byte fromSegments(Segment... segments) {
        byte result = 0;
        for (Segment segment : segments) {
            result |= segment.getValue();
        }
        return result;
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
            .name("Segment Display")
            .bus(bus)
            .device(device)
            .build();
    }

    /**
     * Mapping of segments to their respective bit according to the HT16K33.
     * The chip uses the gfedcba encoding as can be found on Wikipedia: https://en.wikipedia.org/wiki/Seven-segment_display#Hexadecimal
     */
    public enum Segment {
        TOP(0),
        RIGHT_TOP(1),
        RIGHT_BOTTOM(2),
        BOTTOM(3),
        LEFT_BOTTOM(4),
        LEFT_TOP(5),
        CENTER(6),
        DECIMAL_POINT(7);

        private final byte value;

        Segment(int bit) {
            this.value = (byte) (1 << bit);
        }

        byte getValue() {
            return this.value;
        }
    }
}
