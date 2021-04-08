package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.definitions.Direction;
import ch.fhnw.crowpi.components.internal.MAX7219;
import com.pi4j.context.Context;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Implementation of the CrowPi LED matrix using SPI with Pi4J
 */
public class LedMatrixComponent extends MAX7219 {
    /**
     * Default SPI channel for the LED matrix on the CrowPi
     */
    protected static final int DEFAULT_CHANNEL = 1;
    /**
     * Default SPI baud rate for the LED matrix on the CrowPi
     */
    protected static final int DEFAULT_BAUD_RATE = 8000000;
    /**
     * Default delay between scroll operations in milliseconds
     */
    protected static final long DEFAULT_SCROLL_DELAY = 50;
    /**
     * Default direction for scroll operations
     */
    protected static final Direction DEFAULT_SCROLL_DIRECTION = Direction.LEFT;

    /**
     * Creates a new LED matrix component with the default channel and baud rate.
     *
     * @param pi4j Pi4J context
     */
    public LedMatrixComponent(Context pi4j) {
        this(pi4j, DEFAULT_CHANNEL, DEFAULT_BAUD_RATE);
    }

    /**
     * Creates a new LED matrix component with a custom channel and baud rate.
     *
     * @param pi4j    Pi4J context
     * @param channel SPI channel
     * @param baud    SPI baud rate
     */
    public LedMatrixComponent(Context pi4j, int channel, int baud) {
        super(pi4j.create(buildSpiConfig(pi4j, channel, baud)));
    }

    /**
     * Scrolls the display towards the given direction and leaves the now empty row/column empty.
     *
     * @param direction Desired scroll direction
     */
    public void scroll(Direction direction) {
        scroll(direction, ScrollMode.NORMAL, null, 0);
    }

    /**
     * Rotates the display towards the given direction and wraps around the affected row/column.
     * E.g. if {@link Direction#LEFT} is used, the column which falls out on the left will be the new rightmost column.
     *
     * @param direction Desired scroll direction
     */
    public void rotate(Direction direction) {
        scroll(direction, ScrollMode.ROTATE, null, 0);
    }

    /**
     * Scrolls the display towards the given direction and fills the empty row/column based on scroll mode.
     * This helper method calls the appropriate internal functions and MUST not be exposed as it contains internal logic.
     * The scrolling operating will be immediately visible on the display.
     *
     * @param direction  Desired scroll direction
     * @param scrollMode Desired scroll mode
     * @param newBuffer  Only if {@link ScrollMode#REPLACE}: New buffer for replacement values
     * @param newOffset  Only if {@link ScrollMode#REPLACE}: Desired row/column offset for new buffer
     */
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

    /**
     * Scrolls the display upwards and fills the now empty row based on scroll mode.
     * This works by copying the buffer array with an offset using {@link System#arraycopy(Object, int, Object, int, int)}.
     *
     * @param scrollMode Desired scroll mode
     * @param newBuffer  Only if {@link ScrollMode#REPLACE}: New buffer for replacement values
     * @param newOffset  Only if {@link ScrollMode#REPLACE}: Desired row offset for new buffer
     */
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

    /**
     * Scrolls the display downwards and fills the now empty row based on scroll mode.
     * This works by copying the buffer array with an offset using {@link System#arraycopy(Object, int, Object, int, int)}.
     *
     * @param scrollMode Desired scroll mode
     * @param newBuffer  Only if {@link ScrollMode#REPLACE}: New buffer for replacement values
     * @param newOffset  Only if {@link ScrollMode#REPLACE}: Desired row offset for new buffer
     */
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

    /**
     * Scrolls the display to the left and fills the now empty column based on scroll mode.
     * This works by shifting each row to the left and combining the new column value with a binary OR.
     *
     * @param scrollMode Desired scroll mode
     * @param newBuffer  Only if {@link ScrollMode#REPLACE}: New buffer for replacement values
     * @param newOffset  Only if {@link ScrollMode#REPLACE}: Desired column offset for new buffer
     */
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

    /**
     * Scrolls the display to the right and fills the now empty column based on scroll mode.
     * This works by shifting each row to the right and combining the new column value with a binary OR.
     *
     * @param scrollMode Desired scroll mode
     * @param newBuffer  Only if {@link ScrollMode#REPLACE}: New buffer for replacement values
     * @param newOffset  Only if {@link ScrollMode#REPLACE}: Desired column offset for new buffer
     */
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

    /**
     * Prints the given string to the LED matrix by scrolling each character in from left to right with the default scroll delay.
     * This method is blocking until the string has been fully printed and will both start and end with an empty display.
     *
     * @param string String to be displayed
     */
    public void print(String string) {
        print(string, DEFAULT_SCROLL_DIRECTION);
    }

    /**
     * Prints the given string to the LED matrix by scrolling each character in towards the given direction with the default scroll delay.
     * This method is blocking until the string has been fully printed and will both start and end with an empty display.
     *
     * @param string          String to be displayed
     * @param scrollDirection Direction towards character should be scrolled in
     */
    public void print(String string, Direction scrollDirection) {
        print(string, scrollDirection, DEFAULT_SCROLL_DELAY);
    }

    /**
     * Prints the given string to the LED matrix by scrolling each character in towards the given direction with a custom scroll delay.
     * This method is blocking until the string has been fully printed and will both start and end with an empty display.
     *
     * @param string          String to be displayed
     * @param scrollDirection Direction towards character should be scrolled in
     * @param scrollDelay     Delay in milliseconds between scroll operations
     */
    public void print(String string, Direction scrollDirection, long scrollDelay) {
        print(Symbol.SPACE);
        for (int i = 1; i < string.length(); i++) {
            transition(lookupSymbol(string.charAt(i)), scrollDirection, scrollDelay);
        }
        transition(Symbol.SPACE, scrollDirection, scrollDelay);
    }

    /**
     * Prints the given symbol on the LED matrix, which will be immediately displayed.
     *
     * @param symbol Symbol to display
     */
    public void print(Symbol symbol) {
        System.arraycopy(symbol.getRows(), 0, buffer, 0, HEIGHT);
        refresh();
    }

    /**
     * Transitions the current LED matrix display to the given symbol by gradually scrolling the symbol in.
     * This works by scrolling each column in one-by-one towards the default scroll direction with the default scroll delay.
     *
     * @param symbol New symbol to display
     */
    public void transition(Symbol symbol) {
        transition(symbol, DEFAULT_SCROLL_DIRECTION, DEFAULT_SCROLL_DELAY);
    }

    /**
     * Transitions the current LED matrix display to the given symbol by gradually scrolling the symbol in with the default scroll delay.
     * This works by scrolling each column in one-by-one towards the given scroll direction with the default scroll delay.
     *
     * @param symbol          New symbol to display
     * @param scrollDirection Desired scrolling direction, e.g. {@link Direction#LEFT} means the new symbol scrolls in from right towards left
     */
    public void transition(Symbol symbol, Direction scrollDirection) {
        transition(symbol, scrollDirection, DEFAULT_SCROLL_DELAY);
    }

    /**
     * Transitions the current LED matrix display to the given symbol by gradually scrolling the symbol in.
     * This works by scrolling each column in one-by-one towards the given scroll direction with the specified scroll delay.
     *
     * @param symbol          New symbol to display
     * @param scrollDirection Desired scrolling direction, e.g. {@link Direction#LEFT} means the new symbol scrolls in from right towards left
     * @param scrollDelay     Delay in milliseconds between each scrolled column
     */
    public void transition(Symbol symbol, Direction scrollDirection, long scrollDelay) {
        for (int i = 0; i < WIDTH; i++) {
            scroll(scrollDirection, ScrollMode.REPLACE, symbol.getRows(), i);
            try {
                Thread.sleep(scrollDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Returns a {@link Symbol} which is associated with the given ASCII character.
     * Throws an {@link IllegalArgumentException} if no symbol associated with this character was found.
     *
     * @param c Character to lookup
     * @return Symbol associated to character
     */
    protected Symbol lookupSymbol(char c) {
        final var symbol = Symbol.getByChar(c);
        if (symbol == null) {
            throw new IllegalArgumentException("Character is not supported by LED matrix");
        }

        return symbol;
    }

    /**
     * Initializes a blank image with the same size as the LED matrix and calls the given consumer with a {@link Graphics2D} instance.
     * This allows to easily draw on the screen using regular drawing commands like {@link Graphics2D#drawLine(int, int, int, int)}.
     * The drawn image will be immediately displayed on the LED matrix.
     *
     * @param drawer Lambda function which draws on new image
     */
    public void draw(Consumer<Graphics2D> drawer) {
        // Create new 1-bit buffered image with same size as LED matrix
        final var image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        final var graphics = image.createGraphics();

        // Call consumer and pass graphics context for drawing
        drawer.accept(graphics);

        // Draw image on LED matrix
        draw(image);
    }

    /**
     * Displays a specific area of the given {@link BufferedImage} on the LED matrix by enabling LEDs for non-black colors.
     * The area will start at the given X/Y position and has the same width and height as the LED matrix.
     * You MUST ensure that the full width/height is still within bounds or a {@link java.awt.image.RasterFormatException} will be thrown.
     * The drawn image will be immediately displayed on the LED matrix.
     *
     * @param image Image to partially display on the LED matrix
     * @param x     X coordinate where visible area should start
     * @param y     Y coordinate where visible area should start
     */
    public void draw(BufferedImage image, int x, int y) {
        draw(image.getSubimage(x, y, WIDTH, HEIGHT));
    }

    /**
     * Displays the given {@link BufferedImage} on the LED matrix by enabling LEDs for non-black colors.
     * The passed image MUST have the same size as the LED matrix and of type {@link BufferedImage#TYPE_BYTE_BINARY}.
     * Use the overloaded method {@link #draw(BufferedImage, int, int)} to only display a specific area of a bigger image.
     * The drawn image will be immediately displayed on the LED matrix.
     *
     * @param image Image to display on the LED matrix
     */
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

    /**
     * Helper method for extracting a single bit from a byte value.
     * The result will be returned as an integer to guarantee that further bit operations are handled correctly.
     *
     * @param value Byte value to read
     * @param bit   Bit which should be extracted in range 0-7
     * @return Extracted bit (0 or 1) as integer
     */
    private static int getBitFromByte(byte value, int bit) {
        return (((value & 0xFF) >> bit) & 0x1);
    }

    /**
     * Builds a new SPI instance for the LED matrix
     *
     * @param pi4j    Pi4J context
     * @param channel SPI channel
     * @param baud    SPI baud rate
     * @return SPI instance
     */
    private static SpiConfig buildSpiConfig(Context pi4j, int channel, int baud) {
        return Spi.newConfigBuilder(pi4j)
            .id("SPI-")
            .name("LED Matrix")
            .address(channel)
            .baud(baud)
            .build();
    }

    /**
     * Specifies which mode should be used while scrolling the LED matrix.
     */
    protected enum ScrollMode {
        /**
         * Normally scroll the LED matrix in one direction, causing one row or column to be empty.
         */
        NORMAL,
        /**
         * Scroll the LED matrix in one direction and wrap the row or column around to the other side.
         */
        ROTATE,
        /**
         * Scroll the LED matrix in one direction and replace the now empty row or column with values from a new buffer.
         * This can be used to gradually transition from one buffer to another via scrolling.
         */
        REPLACE
    }

    /**
     * Mapping of various symbols to their respective 8x8 encoding.
     * Each symbol can be linked to an optional ASCII code which can be looked up using {@link #getByChar(char)}.
     * <p>
     * All ASCII printable characters are based on the "IBM BIOS 8x8" font.
     * All icons have been manually created for this component library and can not be referenced using ASCII characters.
     */
    public enum Symbol {
        // Icons
        HEART((byte) 0x66, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7E, (byte) 0x3C, (byte) 0x18),
        CROSS((byte) 0x81, (byte) 0x42, (byte) 0x24, (byte) 0x18, (byte) 0x18, (byte) 0x24, (byte) 0x42, (byte) 0x81),
        SMILEY_HAPPY((byte) 0x3C, (byte) 0x42, (byte) 0xA5, (byte) 0x81, (byte) 0xA5, (byte) 0x99, (byte) 0x42, (byte) 0x3C),
        SMILEY_SAD((byte) 0x3C, (byte) 0x42, (byte) 0xA5, (byte) 0x81, (byte) 0x99, (byte) 0xA5, (byte) 0x42, (byte) 0x3C),
        SMILEY_SHOCKED((byte) 0x3C, (byte) 0x42, (byte) 0xA5, (byte) 0x99, (byte) 0xA5, (byte) 0xA5, (byte) 0x5A, (byte) 0x3C),
        SMILEY_NEUTRAL((byte) 0x3C, (byte) 0x42, (byte) 0xA5, (byte) 0x81, (byte) 0xBD, (byte) 0xBD, (byte) 0x42, (byte) 0x3C),
        ARROW_UP((byte) 0x18, (byte) 0x3C, (byte) 0x7E, (byte) 0xFF, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18),
        ARROW_DOWN((byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0xFF, (byte) 0x7E, (byte) 0x3C, (byte) 0x18),
        ARROW_LEFT((byte) 0x10, (byte) 0x30, (byte) 0x70, (byte) 0xFF, (byte) 0xFF, (byte) 0x70, (byte) 0x30, (byte) 0x10),
        ARROW_RIGHT((byte) 0x08, (byte) 0x0C, (byte) 0x0E, (byte) 0xFF, (byte) 0xFF, (byte) 0x0E, (byte) 0x0C, (byte) 0x08),

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

        /**
         * ASCII character to which this symbol belongs to or -1 if no ASCII mapping is available
         */
        private final int ascii;

        /**
         * Byte array with 8 items to represent 8x8 LED matrix
         */
        private final byte[] rows;

        /**
         * Creates a new symbol without any ASCII character association
         *
         * @param rows Byte array with 8 items for 8x8 LED matrix
         */
        Symbol(byte... rows) {
            this(null, rows);
        }

        /**
         * Creates a new symbol associated to a specific ASCII character
         *
         * @param ascii ASCII character to be associated with
         * @param rows  Byte array with 8 items for 8x8 LED matrix
         */
        Symbol(Character ascii, byte... rows) {
            if (rows.length != 8) {
                throw new IllegalArgumentException("Rows must contain exactly 8 items for 8x8 LED matrix");
            }

            this.ascii = ascii != null ? ascii : -1;
            this.rows = rows;
        }

        /**
         * Returns the associated ASCII code for this symbol or -1 if not applicable.
         *
         * @return ASCII code of symbol or -1
         */
        public int getAscii() {
            return this.ascii;
        }

        /**
         * Returns the associated byte array to be used for displaying on the 8x8 LED matrix.
         *
         * @return Byte array with 8 items
         */
        public byte[] getRows() {
            return this.rows;
        }

        /**
         * Attempts to find a symbol associated to the given character and returns it.
         * Returns null if no symbol exists for the given character.
         *
         * @param c ASCII character to lookup
         * @return Symbol if found or null
         */
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