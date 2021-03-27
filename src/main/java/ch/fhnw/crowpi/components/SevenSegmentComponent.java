package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.internal.HT16K33;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

import java.util.Map;

public class SevenSegmentComponent extends HT16K33 {
    protected static final int DEFAULT_BUS = 0x1;
    protected static final int DEFAULT_DEVICE = 0x70;

    private static final int COLON_POSITION = 4;
    private static final int[] CHAR_POSITIONS = new int[]{0, 2, 6, 8};

    private static final Map<Character, Byte> CHAR_BITSETS = Map.ofEntries(
        Map.entry(' ', withSegments()),
        Map.entry('-', withSegments(Segment.CENTER)),
        Map.entry('0', withSegments(Segment.TOP, Segment.LEFT_TOP, Segment.RIGHT_TOP, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('1', withSegments(Segment.RIGHT_TOP, Segment.RIGHT_BOTTOM)),
        Map.entry('2', withSegments(Segment.TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.BOTTOM)),
        Map.entry('3', withSegments(Segment.TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('4', withSegments(Segment.LEFT_TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.RIGHT_BOTTOM)),
        Map.entry('5', withSegments(Segment.TOP, Segment.LEFT_TOP, Segment.CENTER, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('6', withSegments(Segment.TOP, Segment.LEFT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('7', withSegments(Segment.TOP, Segment.RIGHT_TOP, Segment.RIGHT_BOTTOM)),
        Map.entry('8', withSegments(Segment.LEFT_TOP, Segment.TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('9', withSegments(Segment.TOP, Segment.LEFT_TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('A', withSegments(Segment.TOP, Segment.LEFT_TOP, Segment.RIGHT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM)),
        Map.entry('B', withSegments(Segment.LEFT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('C', withSegments(Segment.TOP, Segment.LEFT_TOP, Segment.LEFT_BOTTOM, Segment.BOTTOM)),
        Map.entry('D', withSegments(Segment.RIGHT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.RIGHT_BOTTOM, Segment.BOTTOM)),
        Map.entry('E', withSegments(Segment.TOP, Segment.LEFT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM, Segment.BOTTOM)),
        Map.entry('F', withSegments(Segment.TOP, Segment.LEFT_TOP, Segment.CENTER, Segment.LEFT_BOTTOM))
    );

    public SevenSegmentComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    public SevenSegmentComponent(Context pi4j, int bus, int device) {
        super(pi4j.create(buildI2CConfig(pi4j, bus, device)));
    }

    public void setColon(boolean enabled) {
        if (enabled) {
            buffer[COLON_POSITION] = 0x02;
        } else {
            buffer[COLON_POSITION] = 0x00;
        }
    }

    public void setDecimal(int index, boolean enabled) {
        byte value = getCharacter(index);
        if (enabled) {
            value |= Segment.DECIMAL_POINT.getValue();
        } else {
            value &= ~Segment.DECIMAL_POINT.getValue();
        }

        setCharacter(index, value);
    }

    public void setDigit(int index, int digit) {
        // Ensure digit is within bounds
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("Digit must be an integer in the range 0-9");
        }

        setCharacter(index, (char) ('0' + digit));
    }

    public void setCharacter(int index, char c) {
        // Ensure bitmask exists for given digit
        final var key = Character.toUpperCase(c);
        if (!CHAR_BITSETS.containsKey(key)) {
            throw new IllegalArgumentException("Character must be valid for seven segment display");
        }

        // Set character by using the corresponding bitmask
        setCharacter(index, CHAR_BITSETS.get(c));
    }

    public void setSegments(int index, Segment... segments) {
        setCharacter(index, withSegments(segments));
    }

    private void setCharacter(int index, byte bitmask) {
        // Ensure index is within bounds
        final var maxIndex = CHAR_POSITIONS.length - 1;
        if (index < 0 || index > maxIndex)
            throw new IllegalArgumentException("Index must be an integer in the range 0-" + maxIndex);

        // Write bitmask into buffer
        final var position = CHAR_POSITIONS[index];
        buffer[position] = bitmask;
    }

    private byte getCharacter(int index) {
        // Ensure index is within bounds
        final var maxIndex = CHAR_POSITIONS.length - 1;
        if (index < 0 || index > maxIndex)
            throw new IllegalArgumentException("Index must be an integer in the range 0-" + maxIndex);

        // Read bitmask from buffer
        final var position = CHAR_POSITIONS[index];
        return buffer[position];
    }

    private static I2CConfig buildI2CConfig(Context pi4j, int bus, int device) {
        return I2C.newConfigBuilder(pi4j)
            .id("I2C-" + device + "@" + bus)
            .name("Segment Display")
            .bus(bus)
            .device(device)
            .build();
    }

    public static byte withSegments(Segment... segments) {
        byte result = 0;
        for (Segment segment : segments) {
            result |= segment.getValue();
        }
        return result;
    }

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
