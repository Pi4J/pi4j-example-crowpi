package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.definitions.Direction;
import ch.fhnw.crowpi.components.internal.MAX7219;
import com.pi4j.context.Context;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class LedMatrixComponent extends MAX7219 {
    protected static final int DEFAULT_CHANNEL = 1;
    protected static final int DEFAULT_BAUD_RATE = 8000000;

    public LedMatrixComponent(Context pi4j) {
        this(pi4j, DEFAULT_CHANNEL, DEFAULT_BAUD_RATE);
    }

    public LedMatrixComponent(Context pi4j, int channel, int baud) {
        super(pi4j.create(buildSpiConfig(pi4j, channel, baud)));
    }

    private static SpiConfig buildSpiConfig(Context pi4j, int channel, int baud) {
        return Spi.newConfigBuilder(pi4j)
            .id("SPI-")
            .name("LED Matrix")
            .address(channel)
            .baud(baud)
            .build();
    }

    public void scroll(Direction direction) {
        scroll(direction, ScrollMode.NORMAL, null, 0);
    }

    public void rotate(Direction direction) {
        scroll(direction, ScrollMode.ROTATE, null, 0);
    }

    protected void scroll(Direction direction, ScrollMode scrollMode, byte[] newBuffer, int newOffset) {
        // Call internal scroll function based on direction
        // This has been split up into separate methods to keep this method tidy
        switch (direction) {
            case UP:
                scrollUp(scrollMode, newBuffer, newOffset);
                break;
            case DOWN:
                scrollDown(scrollMode, newBuffer, newOffset);
                break;
            case LEFT:
                scrollLeft(scrollMode, newBuffer, newOffset);
                break;
            case RIGHT:
                scrollRight(scrollMode, newBuffer, newOffset);
                break;
        }

        // Immediately draw the updated buffer to the LED matrix
        refresh();
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    private void scrollUp(ScrollMode scrollMode, byte[] newBuffer, int newOffset) {
        // Preserve first row and scroll buffer upwards
        final var firstRow = buffer[0];
        System.arraycopy(buffer, 1, buffer, 0, HEIGHT - 1);

        // Determine target value for last row based on scroll mode
        final byte lastRow;
        if (scrollMode == ScrollMode.ROTATE) {
            lastRow = firstRow;
        } else if (scrollMode == ScrollMode.REPLACE) {
            lastRow = newBuffer[newOffset];
        } else {
            lastRow = 0;
        }

        // Set last row to determined value
        buffer[HEIGHT - 1] = lastRow;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    private void scrollDown(ScrollMode scrollMode, byte[] newBuffer, int newOffset) {
        // Preserve last row and scroll buffer downwards
        final var lastRow = buffer[HEIGHT - 1];
        System.arraycopy(buffer, 0, buffer, 1, HEIGHT - 1);

        // Determine target value for first row based on scroll mode
        final byte firstRow;
        if (scrollMode == ScrollMode.ROTATE) {
            firstRow = lastRow;
        } else if (scrollMode == ScrollMode.REPLACE) {
            firstRow = newBuffer[HEIGHT - 1 - newOffset];
        } else {
            firstRow = 0;
        }

        // Set first row to determined value
        buffer[0] = firstRow;
    }

    private void scrollLeft(ScrollMode scrollMode, byte[] newBuffer, int newOffset) {
        // Scroll each row individually to the left
        for (int row = 0; row < HEIGHT; row++) {
            // Convert byte to integer to ensure proper bit operations
            final int tmp = buffer[row] & 0xFF;

            if (scrollMode == ScrollMode.ROTATE) {
                // Shift left by one (therefore moving the columns) and wrap the value around
                buffer[row] = (byte) ((tmp << 1) | (tmp >>> 7));
            } else if (scrollMode == ScrollMode.REPLACE) {
                // Determine target value for last column based on new buffer
                final int lastColumn = getBitFromByte(newBuffer[row], WIDTH - 1 - newOffset);
                // Combine shifted row (without last column) with last column
                buffer[row] = (byte) ((tmp << 1) | lastColumn);
            } else {
                // Shift left by one, causing the last column to be empty
                buffer[row] = (byte) (tmp << 1);
            }
        }
    }

    private void scrollRight(ScrollMode scrollMode, byte[] newBuffer, int newOffset) {
        // Scroll each row individually to the right
        for (int row = 0; row < HEIGHT; row++) {
            // Convert byte to integer to ensure proper bit operations
            final int tmp = buffer[row] & 0xFF;

            if (scrollMode == ScrollMode.ROTATE) {
                // Shift right by one (therefore moving the columns) and wrap the value around
                buffer[row] = (byte) ((tmp >>> 1) | (tmp << 7));
            } else if (scrollMode == ScrollMode.REPLACE) {
                // Determine target value for first column based on new buffer
                final int firstColumn = getBitFromByte(newBuffer[row], newOffset) << 7;
                // Combine shifted row (without first column) with first column
                buffer[row] = (byte) ((tmp >>> 1) | firstColumn);
            } else {
                // Shift right by one, causing the first column to be empty
                buffer[row] = (byte) (tmp >>> 1);
            }
        }
    }

    protected void transition(Direction direction, byte[] newBuffer) {
        for (int i = 0; i < 8; i++) {
            scroll(direction, ScrollMode.REPLACE, newBuffer, i);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    public void print(String s) {
        // Add spaces around string to fully transition from empty to string to empty
        s = " " + s + " ";

        print(s.charAt(0));
        for (int i = 1; i < s.length(); i++) {
            final var symbol = Symbol.getByChar(s.charAt(i));
            if (symbol != null) {
                transition(Direction.LEFT, symbol.getRows());
            }
        }
    }

    public void print(char c) {
        final var symbol = Symbol.getByChar(c);
        if (symbol != null) {
            System.arraycopy(symbol.getRows(), 0, buffer, 0, HEIGHT);
            refresh();
        }
    }

    public void draw(Consumer<Graphics2D> drawer) {
        // Create new 1-bit buffered image with same size as LED matrix
        final var image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        final var graphics = image.createGraphics();

        // Call consumer and pass graphics context for drawing
        drawer.accept(graphics);

        // Draw image on LED matrix
        draw(image);
    }

    public void draw(BufferedImage image, int x, int y) {
        draw(image.getSubimage(x, y, WIDTH, HEIGHT));
    }

    public void draw(BufferedImage image) {
        // Ensure image has correct type
        if (image.getType() != BufferedImage.TYPE_BYTE_BINARY) {
            throw new IllegalArgumentException("Image must be of type BYTE_BINARY");
        }

        // Ensure image has correct size
        if (image.getWidth() != WIDTH || image.getHeight() != HEIGHT) {
            throw new IllegalArgumentException("Image must be exactly " + WIDTH + "x" + HEIGHT + " pixels");
        }

        // Copy image into buffer pixel-by-pixel by looping over Y and X coordinates
        // While retrieving the data buffer (which happens to have the same format) would be slightly quicker, it breaks with sub-images
        // As a slight optimization we first clear the buffer and then only call setPixel() when true
        clear();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (image.getRGB(x, y) != Color.BLACK.getRGB()) {
                    setPixel(x, y, true);
                }
            }
        }

        // Immediately draw the updated buffer to the LED matrix
        refresh();
    }

    private static int getBitFromByte(byte value, int bit) {
        return (((value & 0xFF) >> bit) & 0x1);
    }

    protected enum ScrollMode {
        NORMAL,
        ROTATE,
        REPLACE
    }

    public enum Symbol {
        // ASCII: Uppercase Letters
        A('A', (byte) 0x30, (byte) 0x78, (byte) 0xCC, (byte) 0xCC, (byte) 0xFC, (byte) 0xCC, (byte) 0xCC, (byte) 0x00),
        B('B', (byte) 0xFC, (byte) 0x66, (byte) 0x66, (byte) 0x7C, (byte) 0x66, (byte) 0x66, (byte) 0xFC, (byte) 0x00),
        C('C', (byte) 0x3C, (byte) 0x66, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0x66, (byte) 0x3C, (byte) 0x00),
        D('D', (byte) 0xF8, (byte) 0x6C, (byte) 0x66, (byte) 0x66, (byte) 0x66, (byte) 0x6C, (byte) 0xF8, (byte) 0x00),
        E('E', (byte) 0xFE, (byte) 0x62, (byte) 0x68, (byte) 0x78, (byte) 0x68, (byte) 0x62, (byte) 0xFE, (byte) 0x00),
        F('F', (byte) 0xFE, (byte) 0x62, (byte) 0x68, (byte) 0x78, (byte) 0x68, (byte) 0x60, (byte) 0xF0, (byte) 0x00),
        G('G', (byte) 0x3C, (byte) 0x66, (byte) 0xC0, (byte) 0xC0, (byte) 0xCE, (byte) 0x66, (byte) 0x3E, (byte) 0x00),
        H('H', (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xFC, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0x00),
        I('I', (byte) 0x78, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x78, (byte) 0x00),
        J('J', (byte) 0x1E, (byte) 0x0C, (byte) 0x0C, (byte) 0x0C, (byte) 0xCC, (byte) 0xCC, (byte) 0x78, (byte) 0x00),
        K('K', (byte) 0xE6, (byte) 0x66, (byte) 0x6C, (byte) 0x78, (byte) 0x6C, (byte) 0x66, (byte) 0xE6, (byte) 0x00),
        L('L', (byte) 0xF0, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x62, (byte) 0x66, (byte) 0xFE, (byte) 0x00),
        M('M', (byte) 0xC6, (byte) 0xEE, (byte) 0xFE, (byte) 0xFE, (byte) 0xD6, (byte) 0xC6, (byte) 0xC6, (byte) 0x00),
        N('N', (byte) 0xC6, (byte) 0xE6, (byte) 0xF6, (byte) 0xDE, (byte) 0xCE, (byte) 0xC6, (byte) 0xC6, (byte) 0x00),
        O('O', (byte) 0x38, (byte) 0x6C, (byte) 0xC6, (byte) 0xC6, (byte) 0xC6, (byte) 0x6C, (byte) 0x38, (byte) 0x00),
        P('P', (byte) 0xFC, (byte) 0x66, (byte) 0x66, (byte) 0x7C, (byte) 0x60, (byte) 0x60, (byte) 0xF0, (byte) 0x00),
        Q('Q', (byte) 0x78, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xDC, (byte) 0x78, (byte) 0x1C, (byte) 0x00),
        R('R', (byte) 0xFC, (byte) 0x66, (byte) 0x66, (byte) 0x7C, (byte) 0x6C, (byte) 0x66, (byte) 0xE6, (byte) 0x00),
        S('S', (byte) 0x78, (byte) 0xCC, (byte) 0xE0, (byte) 0x70, (byte) 0x1C, (byte) 0xCC, (byte) 0x78, (byte) 0x00),
        T('T', (byte) 0xFC, (byte) 0xB4, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x78, (byte) 0x00),
        U('U', (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xFC, (byte) 0x00),
        V('V', (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0x78, (byte) 0x30, (byte) 0x00),
        W('W', (byte) 0xC6, (byte) 0xC6, (byte) 0xC6, (byte) 0xD6, (byte) 0xFE, (byte) 0xEE, (byte) 0xC6, (byte) 0x00),
        X('X', (byte) 0xC6, (byte) 0xC6, (byte) 0x6C, (byte) 0x38, (byte) 0x38, (byte) 0x6C, (byte) 0xC6, (byte) 0x00),
        Y('Y', (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0x78, (byte) 0x30, (byte) 0x30, (byte) 0x78, (byte) 0x00),
        Z('Z', (byte) 0xFE, (byte) 0xC6, (byte) 0x8C, (byte) 0x18, (byte) 0x32, (byte) 0x66, (byte) 0xFE, (byte) 0x00),

        // ASCII: Lowercase Letters
        a('a', (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x0C, (byte) 0x7C, (byte) 0xCC, (byte) 0x76, (byte) 0x00),
        b('b', (byte) 0xE0, (byte) 0x60, (byte) 0x60, (byte) 0x7C, (byte) 0x66, (byte) 0x66, (byte) 0xDC, (byte) 0x00),
        c('c', (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0xCC, (byte) 0xC0, (byte) 0xCC, (byte) 0x78, (byte) 0x00),
        d('d', (byte) 0x1C, (byte) 0x0C, (byte) 0x0C, (byte) 0x7C, (byte) 0xCC, (byte) 0xCC, (byte) 0x76, (byte) 0x00),
        e('e', (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0xCC, (byte) 0xFC, (byte) 0xC0, (byte) 0x78, (byte) 0x00),
        f('f', (byte) 0x38, (byte) 0x6C, (byte) 0x60, (byte) 0xF0, (byte) 0x60, (byte) 0x60, (byte) 0xF0, (byte) 0x00),
        g('g', (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xCC, (byte) 0xCC, (byte) 0x7C, (byte) 0x0C, (byte) 0xF8),
        h('h', (byte) 0xE0, (byte) 0x60, (byte) 0x6C, (byte) 0x76, (byte) 0x66, (byte) 0x66, (byte) 0xE6, (byte) 0x00),
        i('i', (byte) 0x30, (byte) 0x00, (byte) 0x70, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x78, (byte) 0x00),
        j('j', (byte) 0x0C, (byte) 0x00, (byte) 0x0C, (byte) 0x0C, (byte) 0x0C, (byte) 0xCC, (byte) 0xCC, (byte) 0x78),
        k('k', (byte) 0xE0, (byte) 0x60, (byte) 0x66, (byte) 0x6C, (byte) 0x78, (byte) 0x6C, (byte) 0xE6, (byte) 0x00),
        l('l', (byte) 0x70, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x78, (byte) 0x00),
        m('m', (byte) 0x00, (byte) 0x00, (byte) 0xCC, (byte) 0xFE, (byte) 0xFE, (byte) 0xD6, (byte) 0xC6, (byte) 0x00),
        n('n', (byte) 0x00, (byte) 0x00, (byte) 0xF8, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0x00),
        o('o', (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0x78, (byte) 0x00),
        p('p', (byte) 0x00, (byte) 0x00, (byte) 0xDC, (byte) 0x66, (byte) 0x66, (byte) 0x7C, (byte) 0x60, (byte) 0xF0),
        q('q', (byte) 0x00, (byte) 0x00, (byte) 0x76, (byte) 0xCC, (byte) 0xCC, (byte) 0x7C, (byte) 0x0C, (byte) 0x1E),
        r('r', (byte) 0x00, (byte) 0x00, (byte) 0xDC, (byte) 0x76, (byte) 0x66, (byte) 0x60, (byte) 0xF0, (byte) 0x00),
        s('s', (byte) 0x00, (byte) 0x00, (byte) 0x7C, (byte) 0xC0, (byte) 0x78, (byte) 0x0C, (byte) 0xF8, (byte) 0x00),
        t('t', (byte) 0x10, (byte) 0x30, (byte) 0x7C, (byte) 0x30, (byte) 0x30, (byte) 0x34, (byte) 0x18, (byte) 0x00),
        u('u', (byte) 0x00, (byte) 0x00, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0x76, (byte) 0x00),
        v('v', (byte) 0x00, (byte) 0x00, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0x78, (byte) 0x30, (byte) 0x00),
        w('w', (byte) 0x00, (byte) 0x00, (byte) 0xC6, (byte) 0xD6, (byte) 0xFE, (byte) 0xFE, (byte) 0x6C, (byte) 0x00),
        x('x', (byte) 0x00, (byte) 0x00, (byte) 0xC6, (byte) 0x6C, (byte) 0x38, (byte) 0x6C, (byte) 0xC6, (byte) 0x00),
        y('y', (byte) 0x00, (byte) 0x00, (byte) 0xCC, (byte) 0xCC, (byte) 0xCC, (byte) 0x7C, (byte) 0x0C, (byte) 0xF8),
        z('z', (byte) 0x00, (byte) 0x00, (byte) 0xFC, (byte) 0x98, (byte) 0x30, (byte) 0x64, (byte) 0xFC, (byte) 0x00),

        // ASCII: Numbers
        ZERO('0', (byte) 0x7C, (byte) 0xC6, (byte) 0xCE, (byte) 0xDE, (byte) 0xF6, (byte) 0xE6, (byte) 0x7C, (byte) 0x00),
        ONE('1', (byte) 0x30, (byte) 0x70, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0xFC, (byte) 0x00),
        TWO('2', (byte) 0x78, (byte) 0xCC, (byte) 0x0C, (byte) 0x38, (byte) 0x60, (byte) 0xC4, (byte) 0xFC, (byte) 0x00),
        THREE('3', (byte) 0x78, (byte) 0xCC, (byte) 0x0C, (byte) 0x38, (byte) 0x0C, (byte) 0xCC, (byte) 0x78, (byte) 0x00),
        FOUR('4', (byte) 0x1C, (byte) 0x3C, (byte) 0x6C, (byte) 0xCC, (byte) 0xFE, (byte) 0x0C, (byte) 0x1E, (byte) 0x00),
        FIVE('5', (byte) 0xFC, (byte) 0xC0, (byte) 0xF8, (byte) 0x0C, (byte) 0x0C, (byte) 0xCC, (byte) 0x78, (byte) 0x00),
        SIX('6', (byte) 0x38, (byte) 0x60, (byte) 0xC0, (byte) 0xF8, (byte) 0xCC, (byte) 0xCC, (byte) 0x78, (byte) 0x00),
        SEVEN('7', (byte) 0xFC, (byte) 0xCC, (byte) 0x0C, (byte) 0x18, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x00),
        EIGHT('8', (byte) 0x78, (byte) 0xCC, (byte) 0xCC, (byte) 0x78, (byte) 0xCC, (byte) 0xCC, (byte) 0x78, (byte) 0x00),
        NINE('9', (byte) 0x78, (byte) 0xCC, (byte) 0xCC, (byte) 0x7C, (byte) 0x0C, (byte) 0x18, (byte) 0x70, (byte) 0x00),

        // ASCII: Miscellaneous
        SPACE(' ', (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00),
        EXCLAMATION_MARK('!', (byte) 0x30, (byte) 0x78, (byte) 0x78, (byte) 0x30, (byte) 0x30, (byte) 0x00, (byte) 0x30, (byte) 0x00),
        DOUBLE_QUOTE('"', (byte) 0x6C, (byte) 0x6C, (byte) 0x6C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00),
        NUMBER_SIGN('#', (byte) 0x6C, (byte) 0x6C, (byte) 0xFE, (byte) 0x6C, (byte) 0xFE, (byte) 0x6C, (byte) 0x6C, (byte) 0x00),
        DOLLAR('$', (byte) 0x30, (byte) 0x7C, (byte) 0xC0, (byte) 0x78, (byte) 0x0C, (byte) 0xF8, (byte) 0x30, (byte) 0x00),
        PERCENT('%', (byte) 0x00, (byte) 0xC6, (byte) 0xCC, (byte) 0x18, (byte) 0x30, (byte) 0x66, (byte) 0xC6, (byte) 0x00),
        AMPERSAND('&', (byte) 0x38, (byte) 0x6C, (byte) 0x38, (byte) 0x76, (byte) 0xDC, (byte) 0xCC, (byte) 0x76, (byte) 0x00),
        QUOTE_SINGLE('\'', (byte) 0x60, (byte) 0x60, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00),
        PARENTHESIS_LEFT('(', (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x00),
        PARENTHESIS_RIGHT(')', (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x00),
        ASTERISK('*', (byte) 0x00, (byte) 0x66, (byte) 0x3C, (byte) 0xFF, (byte) 0x3C, (byte) 0x66, (byte) 0x00, (byte) 0x00),
        PLUS('+', (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0xFC, (byte) 0x30, (byte) 0x30, (byte) 0x00, (byte) 0x00),
        COMMA(',', (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x60),
        HYPHEN('-', (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFC, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00),
        PERIOD('.', (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x00),
        SLASH('/', (byte) 0x06, (byte) 0x0C, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xC0, (byte) 0x80, (byte) 0x00),
        COLON(':', (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x00),
        SEMICOLON(';', (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x00, (byte) 0x30, (byte) 0x30, (byte) 0x60, (byte) 0x00),
        LESS('<', (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xC0, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x00),
        EQUAL('=', (byte) 0x00, (byte) 0x00, (byte) 0xFC, (byte) 0x00, (byte) 0x00, (byte) 0xFC, (byte) 0x00, (byte) 0x00),
        GREATER('>', (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x0C, (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x00),
        QUESTION('?', (byte) 0x78, (byte) 0xCC, (byte) 0x0C, (byte) 0x18, (byte) 0x30, (byte) 0x00, (byte) 0x30, (byte) 0x00),
        AT('@', (byte) 0x7C, (byte) 0xC6, (byte) 0xDE, (byte) 0xDE, (byte) 0xDE, (byte) 0xC0, (byte) 0x78, (byte) 0x00),
        BRACKET_LEFT('[', (byte) 0x78, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x78, (byte) 0x00),
        BACKSLASH('\\', (byte) 0xC0, (byte) 0x60, (byte) 0x30, (byte) 0x18, (byte) 0x0C, (byte) 0x06, (byte) 0x02, (byte) 0x00),
        BRACKET_RIGHT(']', (byte) 0x78, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x78, (byte) 0x00),
        CARET('^', (byte) 0x10, (byte) 0x38, (byte) 0x6C, (byte) 0xC6, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00),
        UNDERSCORE('_', (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF),
        GRAVE('`', (byte) 0x30, (byte) 0x30, (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00),
        BRACE_LEFT('{', (byte) 0x1C, (byte) 0x30, (byte) 0x30, (byte) 0xE0, (byte) 0x30, (byte) 0x30, (byte) 0x1C, (byte) 0x00),
        BAR('|', (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x00),
        BRACE_RIGHT('}', (byte) 0xE0, (byte) 0x30, (byte) 0x30, (byte) 0x1C, (byte) 0x30, (byte) 0x30, (byte) 0xE0, (byte) 0x00),
        TILDE('~', (byte) 0x76, (byte) 0xDC, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00);

        private final int ascii;
        private final byte[] rows;

        Symbol(Character ascii, byte... rows) {
            this.ascii = ascii != null ? ascii : -1;
            this.rows = rows;
        }

        public int getAscii() {
            return this.ascii;
        }

        public byte[] getRows() {
            return this.rows;
        }

        public static Symbol getByChar(char c) {
            for (Symbol symbol : Symbol.values()) {
                if (symbol.getAscii() == c) {
                    return symbol;
                }
            }
            return null;
        }
    }
}
