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
        Map.entry(' ', (byte) 0x00),
        Map.entry('-', (byte) 0x40),
        Map.entry('0', (byte) 0x3F),
        Map.entry('1', (byte) 0x06),
        Map.entry('2', (byte) 0x5B),
        Map.entry('3', (byte) 0x4F),
        Map.entry('4', (byte) 0x66),
        Map.entry('5', (byte) 0x6D),
        Map.entry('6', (byte) 0x7D),
        Map.entry('7', (byte) 0x07),
        Map.entry('8', (byte) 0x7F),
        Map.entry('9', (byte) 0x6F),
        Map.entry('A', (byte) 0x77),
        Map.entry('B', (byte) 0x7C),
        Map.entry('C', (byte) 0x39),
        Map.entry('D', (byte) 0x5E),
        Map.entry('E', (byte) 0x79),
        Map.entry('F', (byte) 0x71)
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

    private void setCharacter(int index, byte bitmask) {
        // Ensure position is within bounds
        final var maxIndex = CHAR_POSITIONS.length - 1;
        if (index < 0 || index > maxIndex)
            throw new IllegalArgumentException("Index must be an integer in the range 0-" + maxIndex);

        // Write bitmask into buffer
        final var position = CHAR_POSITIONS[index];
        buffer[position] = bitmask;
    }

    private static I2CConfig buildI2CConfig(Context pi4j, int bus, int device) {
        return I2C.newConfigBuilder(pi4j)
            .id("I2C-" + device + "@" + bus)
            .name("Segment Display")
            .bus(bus)
            .device(device)
            .build();
    }
}
