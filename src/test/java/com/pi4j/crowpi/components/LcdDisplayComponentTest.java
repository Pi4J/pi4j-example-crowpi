package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.crowpi.components.internal.MCP23008;
import com.pi4j.io.i2c.I2C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LcdDisplayComponentTest extends ComponentTest {
    protected LcdDisplayComponent lcd;
    protected MCP23008 mcp;
    protected I2C i2c;

    @BeforeEach
    void setUp() {
        this.lcd = new LcdDisplayComponent(pi4j);
        this.mcp = lcd.getMcp();
        this.i2c = mcp.getI2C();

        lcd.initialize();
        i2c.readRegister(9, new byte[250]);
    }

    @Test
    void testWriteTooLongText() {
        // when
        String text = "to long text to write the display";

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            lcd.writeText(text);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            lcd.writeLine(text, 1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            lcd.writeLine(text, 2);
        });
    }

    @Test
    void testWriteTextWithAllowedLength() {
        // when
        String text = "This is ok";

        // then
        assertDoesNotThrow(() -> {
            lcd.writeLine(text, 1);
            lcd.writeLine(text, 2);
            lcd.writeText(text);
        });

    }

    @Test
    void testClearInvalidLine() {
        // then
        assertThrows(IllegalArgumentException.class, () -> {
            lcd.clearLine(3);
        });
    }

    @Test
    void testSetBacklight() {
        // given
        lcd.setDisplayBacklight(true);

        // then
        assertEquals(mcp.getGpioState(), i2c.readRegister(9));
    }

    @Test
    void testMoveCursorRight() {
        // given
        lcd.moveCursorRight();
        var buffer = new byte[9];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(9, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -120, -120, -116, -120, -96, -96, -92, -96}, buffer);
    }

    @Test
    void testMoveCursorLeft() {
        // given
        lcd.moveCursorLeft();
        var buffer = new byte[9];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(9, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -120, -120, -116, -120, -128, -128, -124, -128}, buffer);
    }

    @Test
    void testMoveDisplayRight() {
        // given
        lcd.moveDisplayRight();
        var buffer = new byte[9];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(9, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -120, -120, -116, -120, -32, -32, -28, -32}, buffer);
    }

    @Test
    void testMoveDisplayLeft() {
        // given
        lcd.moveDisplayRight();
        var buffer = new byte[9];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(9, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -120, -120, -116, -120, -32, -32, -28, -32}, buffer);
    }

    @Test
    void testInitialize() {
        // given
        lcd.initialize();
        var buffer = new byte[64];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(64, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -104, -104, -100, -104, -104, -104, -100, -104, -104, -104, -104, -100, -104,
            -112, -112, -108, -112, -112, -128, -128, -124, -128, -32, -32, -28, -32, -32, -112, -112, -108, -112, -64,
            -64, -60, -64, -64, -128, -128, -124, -128, -80, -80, -76, -80, -80, -128, -128, -124, -128, -120, -120, -116,
            -120, -120, -128, -128, -124, -128, -112, -112, -108, -112, -112}, buffer);
    }

    @Test
    void testWriteCharacter() {
        // given
        lcd.writeCharacter('X');
        var buffer = new byte[9];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(9, numberOfBytes);
        assertArrayEquals(new byte[]{-110, -86, -86, -82, -86, -62, -62, -58, -62}, buffer);
    }

    @Test
    void testWriteALine() {
        // given
        lcd.writeLine("abc", 2);
        var buffer = new byte[198];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(198, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -32, -32, -28, -32, -128, -128, -124, -128, -126, -110, -110, -106, -110, -126, -126,
            -122, -126, -126, -110, -110, -106, -110, -126, -126, -122, -126, -126, -110, -110, -106, -110, -126, -126, -122, -126,
            -126, -110, -110, -106, -110, -126, -126, -122, -126, -126, -110, -110, -106, -110, -126, -126, -122, -126, -126, -110,
            -110, -106, -110, -126, -126, -122, -126, -126, -110, -110, -106, -110, -126, -126, -122, -126, -126, -110, -110, -106,
            -110, -126, -126, -122, -126, -126, -110, -110, -106, -110, -126, -126, -122, -126, -126, -110, -110, -106, -110, -126,
            -126, -122, -126, -126, -110, -110, -106, -110, -126, -126, -122, -126, -126, -110, -110, -106, -110, -126, -126, -122,
            -126, -126, -110, -110, -106, -110, -126, -126, -122, -126, -126, -110, -110, -106, -110, -126, -126, -122, -126, -126,
            -110, -110, -106, -110, -126, -126, -122, -126, -126, -110, -110, -106, -110, -126, -126, -122, -126, -128, -128, -128,
            -124, -128, -112, -112, -108, -112, -112, -32, -32, -28, -32, -128, -128, -124, -128, -126, -78, -78, -74, -78, -118,
            -118, -114, -118, -118, -78, -78, -74, -78, -110, -110, -106, -110, -110, -78, -78, -74, -78, -102, -102,
            -98, -102}, buffer);
    }

    @Test
    public void testWriteText() {
        // given
        lcd.writeText("ABC");
        var buffer = new byte[54];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(54, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -128, -128, -124, -128, -120, -120, -116, -120, -120, -128, -128, -124, -128,
            -112, -112, -108, -112, -112, -64, -64, -60, -64, -128, -128, -124, -128, -126, -94, -94, -90, -94, -118, -118,
            -114, -118, -118, -94, -94, -90, -94, -110, -110, -106, -110, -110, -94, -94, -90, -94, -102, -102, -98, -102}, buffer);
    }

    @Test
    public void testSetCursorToPosition() {
        // given
        lcd.setCursorToPosition(9, 1);
        var buffer = new byte[9];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(9, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -64, -64, -60, -64, -56, -56, -52, -56}, buffer);
    }

    @Test
    public void testClearDisplay() {
        // given
        lcd.clearDisplay();
        var buffer = new byte[18];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(18, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -128, -128, -124, -128, -120, -120, -116, -120, -120, -128, -128,
            -124, -128, -112, -112, -108, -112}, buffer);
    }

    @Test
    public void testCreateCharacter() {
        // given
        lcd.createCharacter(1, new byte[]{
            0b00000,
            0b00000,
            0b00000,
            0b00001,
            0b00011,
            0b00111,
            0b01111,
            0b11111
        });
        var buffer = new byte[81];

        // when
        var numberOfBytes = i2c.readRegister(9, buffer);

        // then
        assertEquals(mcp.getGpioState(), buffer[numberOfBytes - 1]);
        assertEquals(81, numberOfBytes);
        assertArrayEquals(new byte[]{-112, -96, -96, -92, -96, -64, -64, -60, -64, -62, -126, -126, -122, -126,
            -126, -126, -122, -126, -126, -126, -126, -122, -126, -126, -126, -122, -126, -126, -126, -126, -122,
            -126, -126, -126, -122, -126, -126, -126, -126, -122, -126, -118, -118, -114, -118, -118, -126, -126,
            -122, -126, -102, -102, -98, -102, -102, -126, -126, -122, -126, -70, -70, -66, -70, -70, -126, -126,
            -122, -126, -6, -6, -2, -6, -6, -118, -118, -114, -118, -6, -6, -2, -6}, buffer);

    }
}
