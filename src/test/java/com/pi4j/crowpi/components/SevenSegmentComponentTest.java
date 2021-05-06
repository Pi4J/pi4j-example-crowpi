package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.crowpi.components.SevenSegmentComponent.Segment;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;

import static com.pi4j.crowpi.components.SevenSegmentComponent.CHAR_BITSETS;
import static org.junit.jupiter.api.Assertions.*;

class SevenSegmentComponentTest extends ComponentTest {
    protected SevenSegmentComponent segment;
    protected I2C i2c;

    @BeforeEach
    void setUp() {
        this.segment = new SevenSegmentComponent(pi4j);
        this.i2c = segment.getI2C();
    }

    @Test
    void testSetEnabledTrue() {
        // when
        segment.setEnabled(true);

        // then
        assertEquals(0, i2c.readRegisterByte(0b0010_0001)); // System Setup with Oscillator On
        assertEquals(0, i2c.readRegisterByte(0b1000_0001)); // Display Setup with Display On
    }

    @Test
    void testSetEnabledFalse() {
        // when
        segment.setEnabled(false);

        // then
        assertEquals(0, i2c.readRegisterByte(0b0010_0000)); // System Setup with Oscillator Off
        assertEquals(0, i2c.readRegisterByte(0b1000_0000)); // Display Setup with Display Off
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 12, 123, 1234, 12345})
    void testPrintInteger(int i) {
        segment.print(i);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1, 1.2, 1.23, 1.234, 1.2345})
    void testPrintDouble(double d) {
        segment.print(d);
    }

    @Test
    void testPrintTime() {
        // given
        LocalTime time = LocalTime.now();

        // when
        segment.print(time);
        final var fromTime = getAllDigits();
        segment.print(String.format("%02d%02d", time.getHour(), time.getMinute()));
        final var fromString = getAllDigits();

        // then
        assertArrayEquals(fromString, fromTime);
    }

    @Test
    void testPrintString() {
        // when
        segment.print("9.8:76.");

        // then
        assertEquals(CHAR_BITSETS.get('9') | Segment.DECIMAL_POINT.getValue(), segment.getRawDigit(0)); // #1: 9 with decimal point
        assertEquals(CHAR_BITSETS.get('8'), segment.getRawDigit(1)); // #2: 8 without decimal point
        assertEquals(CHAR_BITSETS.get('7'), segment.getRawDigit(2)); // #3: 7 without decimal point
        assertEquals(CHAR_BITSETS.get('6') | Segment.DECIMAL_POINT.getValue(), segment.getRawDigit(3)); // #4: 6 with decimal point
    }

    @Test
    void testSetColon() {
        segment.setColon(true);
        segment.setColon(false);
    }

    @ParameterizedTest
    @ValueSource(bytes = {0b00000000, 0b01111111})
    void testSetDecimalPoint(byte value) {
        // when
        segment.setRawDigit(0, value);
        segment.setDecimalPoint(0, true);

        segment.setRawDigit(1, value);
        segment.setDecimalPoint(1, true);
        segment.setDecimalPoint(1, false);

        // then
        assertEquals(value | Segment.DECIMAL_POINT.getValue(), segment.getRawDigit(0));
        assertEquals(value, segment.getRawDigit(1));
    }

    @ParameterizedTest
    @CsvSource({
        "0,1000_0001",
        "1,1000_0011",
        "2,1000_0101",
        "3,1000_0111"
    })
    void testSetBlinkRate(int rate, String binaryAddress) {
        // when
        segment.setBlinkRate(rate);

        // then
        final var address = parseBinary(binaryAddress);
        assertEquals(0, i2c.readRegisterByte(address)); // Display Setup with Display On and Blink Rate
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 4, 5})
    void testSetBlinkRateBounds(int rate) {
        // when
        Executable t = () -> segment.setBlinkRate(rate);

        // then
        assertThrows(IllegalArgumentException.class, t);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    void testSetBrightness(int brightness) {
        // when
        segment.setBrightness(brightness);

        // then
        final var address = 0b1110_0000 | (brightness & 0xF); // Dimming Set with Brightness as Setting
        assertEquals(0, i2c.readRegisterByte(address));
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 16, 17})
    void testSetBrightnessBounds(int brightness) {
        // when
        Executable t = () -> segment.setBrightness(brightness);

        // then
        assertThrows(IllegalArgumentException.class, t);
    }

    @Test
    void testClearBuffer() {
        // given
        segment.print("----");

        // when
        segment.clear();

        // then
        assertEquals(0, segment.getRawDigit(0));
        assertEquals(0, segment.getRawDigit(1));
        assertEquals(0, segment.getRawDigit(2));
        assertEquals(0, segment.getRawDigit(3));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void testSetDigitIntegerMapping(Integer i) {
        // given
        char c = i.toString().charAt(0);

        // when
        segment.setDigit(0, i);

        // then
        assertEquals(CHAR_BITSETS.get(c), segment.getRawDigit(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 10, 11})
    void testSetDigitIntegerBounds(int i) {
        // when
        Executable t = () -> segment.setDigit(0, i);

        // then
        assertThrows(IllegalArgumentException.class, t);
    }

    @ParameterizedTest
    @ValueSource(chars = {'i', 'z', '@', '#'})
    void testSetCharacterBounds(char c) {
        // when
        Executable t = () -> segment.setDigit(0, c);

        // then
        assertThrows(IllegalArgumentException.class, t);
    }

    @ParameterizedTest
    @CsvSource({
        // Values calculated manually based on testing
        "' ',0x00",
        "-,0x40",

        // Values taken from https://en.wikipedia.org/wiki/Seven-segment_display#Hexadecimal
        // The seven-segment display on the CrowPi uses the "gfedcba" encoding
        "0,0x3F",
        "1,0x06",
        "2,0x5B",
        "3,0x4F",
        "4,0x66",
        "5,0x6D",
        "6,0x7D",
        "7,0x07",
        "8,0x7F",
        "9,0x6F",
        "A,0x77",
        "B,0x7C",
        "C,0x39",
        "D,0x5E",
        "E,0x79",
        "F,0x71",
    })
    void testCharacterMap(char input, byte expected) {
        // when
        segment.setDigit(0, input);

        // then
        assertEquals(expected, segment.getRawDigit(0));
    }

    private byte[] getAllDigits() {
        return new byte[]{
            segment.getRawDigit(0),
            segment.getRawDigit(1),
            segment.getRawDigit(2),
            segment.getRawDigit(3)
        };
    }

    private static int parseBinary(String s) {
        return Integer.parseInt(s.replace("_", ""), 2);
    }
}
