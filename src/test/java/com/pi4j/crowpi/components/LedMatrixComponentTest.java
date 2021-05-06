package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.crowpi.components.LedMatrixComponent.Symbol;
import com.pi4j.crowpi.components.definitions.Direction;
import com.pi4j.io.spi.Spi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import static com.pi4j.crowpi.components.internal.MAX7219.HEIGHT;
import static com.pi4j.crowpi.components.internal.MAX7219.WIDTH;
import static org.junit.jupiter.api.Assertions.*;

class LedMatrixComponentTest extends ComponentTest {
    protected LedMatrixComponent matrix;
    protected Spi spi;

    @BeforeEach
    void setUp() {
        this.matrix = new LedMatrixComponent(pi4j);
        this.spi = matrix.getSpi();
    }

    @Test
    void testSetEnabledTrue() {
        // when
        matrix.setEnabled(true);

        // then
        assertArrayEquals(new byte[]{0x0C, 0x01}, spi.readNBytes(2)); // Normal Operation
        assertArrayEquals(new byte[]{0x09, 0x00}, spi.readNBytes(2)); // No decoding for digits 0-7
        assertArrayEquals(new byte[]{0x0B, 0x07}, spi.readNBytes(2)); // Enable all 8 digits
    }

    @Test
    void testSetEnabledFalse() {
        // when
        matrix.setEnabled(false);

        // then
        assertArrayEquals(new byte[]{0x0C, 0x00}, spi.readNBytes(2)); // Shutdown Mode
    }

    @Test
    void testSetTestModeTrue() {
        // when
        matrix.setTestMode(true);

        // then
        assertArrayEquals(new byte[]{0x0F, 0x01}, spi.readNBytes(2)); // Test Mode Enabled
    }

    @Test
    void testSetTestModeFalse() {
        // when
        matrix.setTestMode(false);

        // then
        assertArrayEquals(new byte[]{0x0F, 0x00}, spi.readNBytes(2)); // Test Mode Disabled
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
    void testSetBrightness(int brightness) {
        // when
        matrix.setBrightness(brightness);

        // then
        assertArrayEquals(new byte[]{0x0A, (byte) brightness}, spi.readNBytes(2)); // Intensity
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 16, 17})
    void testSetBrightnessBounds(int brightness) {
        // when
        final Executable t = () -> matrix.setBrightness(brightness);

        // then
        assertThrows(IllegalArgumentException.class, t);
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 8, 9})
    void testSetPixelBounds(int xy) {
        // when
        final Executable t1 = () -> matrix.setPixel(xy, 0, true);
        final Executable t2 = () -> matrix.setPixel(xy, 0, true);

        // then
        assertThrows(IllegalArgumentException.class, t1);
        assertThrows(IllegalArgumentException.class, t2);
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 8, 9})
    void testGetPixelBounds(int xy) {
        // when
        final Executable t1 = () -> matrix.getPixel(xy, 0);
        final Executable t2 = () -> matrix.getPixel(xy, 0);

        // then
        assertThrows(IllegalArgumentException.class, t1);
        assertThrows(IllegalArgumentException.class, t2);
    }

    @Test
    void testClearBuffer() {
        // given
        setPixelBuffer(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF);

        // when
        matrix.clear();

        // then
        assertPixelBuffer(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
    }

    @Test
    void testRefresh() {
        // given
        setPixelBuffer(0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18);

        // when
        matrix.refresh();

        // then
        assertArrayEquals(new byte[]{0x01, 0x11}, spi.readNBytes(2));
        assertArrayEquals(new byte[]{0x02, 0x12}, spi.readNBytes(2));
        assertArrayEquals(new byte[]{0x03, 0x13}, spi.readNBytes(2));
        assertArrayEquals(new byte[]{0x04, 0x14}, spi.readNBytes(2));
        assertArrayEquals(new byte[]{0x05, 0x15}, spi.readNBytes(2));
        assertArrayEquals(new byte[]{0x06, 0x16}, spi.readNBytes(2));
        assertArrayEquals(new byte[]{0x07, 0x17}, spi.readNBytes(2));
        assertArrayEquals(new byte[]{0x08, 0x18}, spi.readNBytes(2));
    }

    @Test
    void testScrollUp() {
        // given
        setPixelBuffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);

        // when
        matrix.scroll(Direction.UP);

        // then
        assertPixelBuffer(0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x00);
    }

    @Test
    void testRotateUp() {
        // given
        setPixelBuffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);

        // when
        matrix.rotate(Direction.UP);

        // then
        assertPixelBuffer(0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01);
    }

    @Test
    void testReplaceUp() {
        // given
        setPixelBuffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);
        final byte[] newBuffer = new byte[]{0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80};

        // when
        matrix.scroll(Direction.UP, LedMatrixComponent.ScrollMode.REPLACE, newBuffer, 1);

        // then last row should be 0x02 as taken from new buffer with offset 1
        assertPixelBuffer(0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x02);
    }

    @Test
    void testScrollDown() {
        // given
        setPixelBuffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);

        // when
        matrix.scroll(Direction.DOWN);

        // then
        assertPixelBuffer(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07);
    }

    @Test
    void testRotateDown() {
        // given
        setPixelBuffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);

        // when
        matrix.rotate(Direction.DOWN);

        // then
        assertPixelBuffer(0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07);
    }

    @Test
    void testReplaceDown() {
        // given
        setPixelBuffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);
        final byte[] newBuffer = new byte[]{0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80};

        // when
        matrix.scroll(Direction.DOWN, LedMatrixComponent.ScrollMode.REPLACE, newBuffer, 1);

        // then first row should be 0x40 as taken from new buffer with offset 1 (counting from end / inverted)
        assertPixelBuffer(0x40, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07);
    }

    @Test
    void testScrollLeft() {
        // given
        setPixelBuffer(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88);

        // when
        matrix.scroll(Direction.LEFT);

        // then
        assertPixelBuffer(0x02, 0x04, 0x06, 0x08, 0x0A, 0x0C, 0x0E, 0x10);
    }

    @Test
    void testRotateLeft() {
        // given
        setPixelBuffer(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88);

        // when
        matrix.rotate(Direction.LEFT);

        // then
        assertPixelBuffer(0x03, 0x05, 0x07, 0x09, 0x0B, 0x0D, 0x0F, 0x11);
    }

    @Test
    void testReplaceLeft() {
        // given
        setPixelBuffer(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88);
        final byte[] newBuffer = new byte[]{0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80};

        // when
        matrix.scroll(Direction.LEFT, LedMatrixComponent.ScrollMode.REPLACE, newBuffer, 1);

        // then second-last row should be 0x0F due to the second column at this row being set in the new buffer (0x40)
        assertPixelBuffer(0x02, 0x04, 0x06, 0x08, 0x0A, 0x0C, 0x0F, 0x10);
    }

    @Test
    void testScrollRight() {
        // given
        setPixelBuffer(0x03, 0x05, 0x07, 0x09, 0x0B, 0x0D, 0x0F, 0x11);

        // when
        matrix.scroll(Direction.RIGHT);

        // then
        assertPixelBuffer(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);
    }

    @Test
    void testRotateRight() {
        // given
        setPixelBuffer(0x03, 0x05, 0x07, 0x09, 0x0B, 0x0D, 0x0F, 0x11);

        // when
        matrix.rotate(Direction.RIGHT);

        // then
        assertPixelBuffer(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88);
    }

    @Test
    void testReplaceRight() {
        // given
        setPixelBuffer(0x03, 0x05, 0x07, 0x09, 0x0B, 0x0D, 0x0F, 0x11);
        final byte[] newBuffer = new byte[]{0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80};

        // when
        matrix.scroll(Direction.RIGHT, LedMatrixComponent.ScrollMode.REPLACE, newBuffer, 1);

        // then second row should be 0x82 due to the second column at this row being set in the new buffer (0x02)
        assertPixelBuffer(0x01, 0x82, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);
    }

    @Test
    void testAsciiSupport() {
        // test support for all printable ascii characters
        for (char c = 32; c < 127; c++) {
            // given
            final char finalC = c;

            // when
            Executable t = () -> matrix.lookupSymbol(finalC);

            // then
            assertDoesNotThrow(t);
        }
    }

    @Test
    void testStringParsing() {
        // given
        final String s1 = "{HEART}";
        final String s2 = "A{CROSS}B";
        final String s3 = "A{I1}{I2";

        // when
        final List<Symbol> actual1 = matrix.convertToSymbols(s1);
        final List<Symbol> actual2 = matrix.convertToSymbols(s2);
        final List<Symbol> actual3 = matrix.convertToSymbols(s3);

        // then
        assertArrayEquals(new Symbol[]{Symbol.HEART}, actual1.toArray());
        assertArrayEquals(new Symbol[]{Symbol.A, Symbol.CROSS, Symbol.B}, actual2.toArray());
        assertArrayEquals(new Symbol[]{
            Symbol.A, Symbol.BRACE_LEFT, Symbol.I, Symbol.ONE, Symbol.BRACE_RIGHT, Symbol.BRACE_LEFT, Symbol.I, Symbol.TWO
        }, actual3.toArray());
    }

    @Test
    void testPrintCharacter() {
        // when
        matrix.print(" ", Direction.LEFT, 0);

        // then
        assertPixelBuffer(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
    }

    @Test
    void testPrintString() {
        // when
        Executable t = () -> matrix.print("ABC", Direction.LEFT, 0);

        // then
        assertDoesNotThrow(t);
    }

    @Test
    void testDrawLambda() {
        // given
        final Consumer<Graphics2D> f = (graphics) -> {
            graphics.drawLine(0, 0, WIDTH, HEIGHT);
        };

        // when
        matrix.draw(f);

        // then
        assertPixelBuffer(0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1);
    }

    @Test
    void testDrawImage() {
        // given
        final BufferedImage i = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        i.createGraphics().drawLine(0, 0, WIDTH, HEIGHT);

        // when
        matrix.draw(i);

        // then
        assertPixelBuffer(0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1);
    }

    @Test
    void testDrawPartialImage() {
        // given
        final BufferedImage image = new BufferedImage(WIDTH * 3, HEIGHT * 3, BufferedImage.TYPE_BYTE_BINARY);
        image.createGraphics().drawLine(WIDTH, HEIGHT, WIDTH * 2, HEIGHT * 2);

        // when
        matrix.draw(image, WIDTH, HEIGHT);

        // then
        assertPixelBuffer(0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1);
    }

    @Test
    void testDrawIllegalImage() {
        // given
        final BufferedImage i1 = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        final BufferedImage i2 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        // when
        final Executable t1 = () -> matrix.draw(i1);
        final Executable t2 = () -> matrix.draw(i2);

        // then
        assertThrows(IllegalArgumentException.class, t1);
        assertThrows(IllegalArgumentException.class, t2);
    }

    private void setPixelBuffer(int... rows) {
        assertEquals(HEIGHT, rows.length);

        for (int y = 0; y < HEIGHT; y++) {
            final int tmp = rows[y] & 0xFF;
            matrix.setPixel(0, y, ((tmp >>> 7) & 0x1) != 0);
            matrix.setPixel(1, y, ((tmp >>> 6) & 0x1) != 0);
            matrix.setPixel(2, y, ((tmp >>> 5) & 0x1) != 0);
            matrix.setPixel(3, y, ((tmp >>> 4) & 0x1) != 0);
            matrix.setPixel(4, y, ((tmp >>> 3) & 0x1) != 0);
            matrix.setPixel(5, y, ((tmp >>> 2) & 0x1) != 0);
            matrix.setPixel(6, y, ((tmp >>> 1) & 0x1) != 0);
            matrix.setPixel(7, y, ((tmp) & 0x1) != 0);
        }
    }

    private void assertPixelBuffer(int... rows) {
        assertEquals(HEIGHT, rows.length);

        for (int y = 0; y < HEIGHT; y++) {
            final byte expected = (byte) rows[y];
            final byte actual = getRowValue(y);

            assertEquals(
                expected, getRowValue(y),
                String.format("Expected value [0x%02x] of row %d does not match actual value [0x%02x]", expected, y, actual)
            );
        }
    }

    private byte getRowValue(int row) {
        byte result = 0;
        for (int x = 0; x < 8; x++) {
            if (matrix.getPixel(x, row)) {
                result |= (1 << (7 - x));
            }
        }
        return result;
    }
}
