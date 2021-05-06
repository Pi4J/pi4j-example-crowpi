package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.internal.MCP23008;

/**
 * This class provides a simple usage of a LCD Display with Pi4J and the CrowPi.
 * There are different ways possible to use this functionalities from pretty simple to a bit more basic and advanced. *
 */
public class LcdDisplayComponent extends Component {
    /**
     * IO Component used to Display
     */
    private final MCP23008 mcp;
    /**
     * Those default address are to use this class with default CrowPi setup
     */
    private static final int DEFAULT_BUS = 0x1;
    private static final int DEFAULT_DEVICE = 0x21;

    /**
     * With this Byte cursor visibility is controlled
     */
    private byte displayControl;

    /**
     * Creates a new LCD Display component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public LcdDisplayComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new LCD Display component with custom bus, device address
     *
     * @param pi4j   Pi4J context
     * @param bus    Custom I2C bus address
     * @param device Custom device address on I2C
     */
    public LcdDisplayComponent(Context pi4j, int bus, int device) {
        this.mcp = new MCP23008(pi4j, bus, device);
        this.mcp.initializeIo(MCP_IO_CONFIG);
    }

    /**
     * Initializes the LCD Display
     */
    public void initialize() {
        // Initialize display
        write((byte) 0b001_10011);
        write((byte) 0b001_10010);

        // Initialize display settings
        this.displayControl = (byte) (LCD_DISPLAY_ON | LCD_CURSOR_OFF | LCD_BLINK_OFF);
        byte displayFunction = (byte) (LCD_4BIT_MODE | LCD_1LINE | LCD_2LINE | LCD_5x8DOTS);
        byte displayMode = (byte) (LCD_ENTRY_LEFT | LCD_ENTRY_SHIFT_DECREMENT);

        // Write Display settings
        executeCommand(LCD_DISPLAY_CONTROL, displayControl);
        write((byte) (LCD_FUNCTION_SET | displayFunction));
        write((byte) (LCD_ENTRY_MODE_SET | displayMode));

        // Clear display
        clearDisplay();

        // Enable backlight
        setDisplayBacklight(true);
    }

    /**
     * Writes a character to the current cursor position
     *
     * @param c Character which is written to the LCD Display
     */
    public void writeCharacter(char c) {
        write(Symbol.getByChar(c), true);
    }

    /**
     * Write a character to a specified place on the display.
     *
     * @param c     Character to write
     * @param digit Digit number on the line
     * @param line  Line number on the display
     */
    public void writeCharacter(char c, int digit, int line) {
        setCursorToPosition(digit, line);
        write(Symbol.getByChar(c), true);
    }

    /**
     * Write a Line of Text on the LCD Display
     *
     * @param text Text to display
     * @param line Select Line of Display
     */
    public void writeLine(String text, int line) {
        if (text.length() > 16) {
            throw new IllegalArgumentException("Too long text. Only 16 characters possible");
        }

        clearLine(line);
        moveCursorHome();
        setCursorToLine(line);

        for (int i = 0; i < text.length(); i++) {
            write(Symbol.getByChar(text.charAt(i)), true);
        }
    }

    /**
     * Write a text upto 32 characters to the display
     *
     * @param text Text to write to the display
     */
    public void writeText(String text) {
        if ((text.length() > 32 && !text.contains("\n")) || (text.length() > 33 && text.contains("\n"))) {
            throw new IllegalArgumentException("Too long text. Only 32 characters plus one linebreak allowed");
        }

        // Clean and prepare to write some text
        var currentLine = 1;
        clearDisplay();
        setCursorToLine(1);

        // Iterate through characters and write them to the display
        for (int i = 0; i < text.length(); i++) {
            // line break in text found
            if (text.charAt(i) == '\n') {
                currentLine = 2;
                setCursorToLine(2);
                continue;
            }

            // Write character to display
            write(Symbol.getByChar(text.charAt(i)), true);

            // Was last character on first line? switch to second
            if (i == 15 && currentLine == 1) {
                setCursorToLine(2);
                if (text.charAt(i + 1) == ' ') {
                    i++;
                }
                currentLine = 2;
            }
        }
    }

    /**
     * Returns the Cursor to Home Position (First line, first character)
     */
    public void moveCursorHome() {
        write(LCD_RETURN_HOME);
        sleep(3);
    }

    /**
     * Moves the cursor 1 character right
     */
    public void moveCursorRight() {
        executeCommand(LCD_CURSOR_SHIFT, (byte) (LCD_CURSOR_MOVE | LCD_MOVE_RIGHT));
        sleep(1);
    }

    /**
     * Moves the cursor 1 character left
     */
    public void moveCursorLeft() {
        executeCommand(LCD_CURSOR_SHIFT, (byte) (LCD_CURSOR_MOVE | LCD_MOVE_LEFT));
        sleep(1);
    }

    /**
     * Sets the cursor to a target destination
     *
     * @param digit Selects the character of the line
     * @param line  Selects the line of the display
     */
    public void setCursorToPosition(int digit, int line) {
        if (line > 2 || line < 1) {
            throw new IllegalArgumentException("Line out of range. Display has only 2x16 Characters!");
        }

        if (digit < 0 || digit > 15) {
            throw new IllegalArgumentException("Digit out of range. Display has only 2x16 Characters!");
        }

        digit &= 0xFF;
        line &= 0xFF;

        executeCommand(LCD_SET_DDRAM_ADDR, (byte) (digit + LCD_ROW_OFFSETS[line - 1]));
    }

    /**
     * Set the cursor to line 1 or 2
     *
     * @param line Sets the cursor to this line. Only Range 1-2 allowed.
     */
    public void setCursorToLine(int line) {
        if (line > 2 || line < 1) {
            throw new IllegalArgumentException("CrowPi Display has only 2 Rows!");
        }

        executeCommand(LCD_SET_DDRAM_ADDR, LCD_ROW_OFFSETS[line - 1]);
    }

    /**
     * Sets the display cursor to hidden or showing
     *
     * @param show Set the state of the cursor
     */
    public void setCursorVisibility(boolean show) {
        if (show) {
            this.displayControl |= LCD_CURSOR_ON;
        } else {
            this.displayControl &= ~LCD_CURSOR_ON;
        }

        executeCommand(LCD_DISPLAY_CONTROL, this.displayControl);
    }

    /**
     * Set the cursor to blinking or static
     *
     * @param blink Blink = true means the cursor will change to blinking mode. False let's the cursor stay static
     */
    public void setCursorBlinking(boolean blink) {
        if (blink) {
            this.displayControl |= LCD_BLINK_ON;
        } else {
            this.displayControl &= ~LCD_BLINK_ON;
        }

        executeCommand(LCD_DISPLAY_CONTROL, this.displayControl);
    }

    /**
     * Moves the whole displayed text one character right
     */
    public void moveDisplayRight() {
        executeCommand(LCD_CURSOR_SHIFT, (byte) (LCD_DISPLAY_MOVE | LCD_MOVE_RIGHT));
    }

    /**
     * Moves the whole displayed text one character right
     */
    public void moveDisplayLeft() {
        executeCommand(LCD_CURSOR_SHIFT, (byte) (LCD_DISPLAY_MOVE | LCD_MOVE_LEFT));
    }

    /**
     * Create a custom character by providing the single digit states of each pixel. Simply pass an Array of bytes
     * which will be translated to a character.
     *
     * @param location  Set the memory location of the character. 1 - 7 is possible.
     * @param character Byte array representing the pixels of a character
     */
    public void createCharacter(int location, byte[] character) {
        if (character.length != 8) {
            throw new IllegalArgumentException("Array has invalid length. Character is only 5x8 Digits. Only a array with length" +
                " 8 is allowed");
        }

        if (location > 7 || location < 1) {
            throw new IllegalArgumentException("Invalid memory location. Range 1-7 allowed. Value: " + location);
        }

        location &= 0x7;
        write(LCD_SET_CGRAM_ADDR | (location << 3));

        for (int i = 0; i < 8; i++) {
            write(character[i], true);
        }
    }

    /**
     * Enable and Disable the Backlight of the LCD Display
     *
     * @param state Set Backlight ON or OFF
     */
    public void setDisplayBacklight(boolean state) {
        mcp.setAndWritePin(LCD_LIGHT, state);
    }

    /**
     * Clears the display and return the cursor to home
     */
    public void clearDisplay() {
        write(LCD_CLEAR_DISPLAY);
        sleep(3);
        moveCursorHome();
    }

    /**
     * Clears a line of the display
     *
     * @param line Select line to clear
     */
    public void clearLine(int line) {
        setCursorToLine(line);

        for (int i = 0; i < 16; i++) {
            write(' ', true);
        }
    }

    /**
     * Execute Display commands
     *
     * @param command Select the LCD Command
     * @param data    Setup command data
     */
    protected void executeCommand(byte command, byte data) {
        write((byte) (command | data));
    }

    /**
     * Write a number (byte) to the LCD Display
     *
     * @param c Number to write to the Display
     */
    protected void write(int c) {
        write(c, false);
    }

    /**
     * Write a Number (byte) or character according to the LCD Display
     *
     * @param b        Data to write to the display
     * @param charMode Select data is a number or character
     */
    protected void write(int b, boolean charMode) {
        b &= 0xFF;
        mcp.setAndWritePin(LCD_RS, charMode);

        // high nibble
        mcp.setPin(LCD_D4, (b & 0b0001_0000) > 0);
        mcp.setPin(LCD_D5, (b & 0b0010_0000) > 0);
        mcp.setPin(LCD_D6, (b & 0b0100_0000) > 0);
        mcp.setPin(LCD_D7, (b & 0b1000_0000) > 0);
        mcp.writePins();
        mcp.pulsePin(LCD_EN, 1);

        // low nibble
        mcp.setPin(LCD_D4, (b & 0b0000_0001) > 0);
        mcp.setPin(LCD_D5, (b & 0b0000_0010) > 0);
        mcp.setPin(LCD_D6, (b & 0b0000_0100) > 0);
        mcp.setPin(LCD_D7, (b & 0b0000_1000) > 0);
        mcp.writePins();
        mcp.pulsePin(LCD_EN, 1);
    }

    /**
     * Get the current MCP instance
     *
     * @return MCP Instance of this LCD Display
     */
    protected MCP23008 getMcp() {
        return this.mcp;
    }

    /**
     * MCP IO Configuration makes pins to inputs or outputs
     */
    private static final byte MCP_IO_CONFIG = 0x00;

    /**
     * Commands which are available to execute on the display. Best to use execute method of this class
     */
    private static final byte LCD_CLEAR_DISPLAY = 0x01;
    private static final byte LCD_RETURN_HOME = 0x02;
    private static final byte LCD_ENTRY_MODE_SET = 0x04;
    private static final byte LCD_DISPLAY_CONTROL = 0x08;
    private static final byte LCD_CURSOR_SHIFT = 0x10;
    private static final byte LCD_FUNCTION_SET = 0x20;
    private static final byte LCD_SET_CGRAM_ADDR = 0x40;
    private static final byte LCD_SET_DDRAM_ADDR = (byte) 0x80;

    /**
     * Defines home on left side
     */
    private static final byte LCD_ENTRY_LEFT = 0x02;
    private static final byte LCD_ENTRY_SHIFT_DECREMENT = 0x00;

    /**
     * Flags to use with the display control byte
     */
    private static final byte LCD_DISPLAY_ON = 0x04;
    private static final byte LCD_CURSOR_ON = 0x02;
    private static final byte LCD_CURSOR_OFF = 0x00;
    private static final byte LCD_BLINK_ON = 0x01;
    private static final byte LCD_BLINK_OFF = 0x00;

    /**
     * Move the cursor or display flags
     */
    private static final byte LCD_DISPLAY_MOVE = 0x08;
    private static final byte LCD_CURSOR_MOVE = 0x00;
    private static final byte LCD_MOVE_RIGHT = 0x04;
    private static final byte LCD_MOVE_LEFT = 0x00;

    // Function set flags
    private static final byte LCD_4BIT_MODE = 0x00;
    private static final byte LCD_2LINE = 0x08;
    private static final byte LCD_1LINE = 0x00;
    private static final byte LCD_5x8DOTS = 0x00;

    /**
     * Display row offsets. Offset for up to 2 rows.
     */
    private static final byte[] LCD_ROW_OFFSETS = {0x00, 0x40};

    /**
     * Pin out LCD auf MCP
     */
    private static final int LCD_RS = 1;
    private static final int LCD_EN = 2;
    private static final int LCD_D4 = 3;
    private static final int LCD_D5 = 4;
    private static final int LCD_D6 = 5;
    private static final int LCD_D7 = 6;
    private static final int LCD_LIGHT = 7;

    /**
     * Enumeration with most important and used symbols. Resolves ASCII character to the LCD Display characters table
     */
    public enum Symbol {
        ZERO('0', 0x30),
        ONE('1', 0x31),
        TWO('2', 0x32),
        THREE('3', 0x33),
        FOUR('4', 0x34),
        FIVE('5', 0x35),
        SIX('6', 0x36),
        SEVEN('7', 0x37),
        EIGHT('8', 0x38),
        NINE('9', 0x39),

        A('A', 0x41),
        B('B', 0x42),
        C('C', 0x43),
        D('D', 0x44),
        E('E', 0x45),
        F('F', 0x46),
        G('G', 0x47),
        H('H', 0x48),
        I('I', 0x49),
        J('J', 0x4A),
        K('K', 0x4B),
        L('L', 0x4C),
        M('M', 0x4D),
        N('N', 0x4E),
        O('O', 0x4F),
        P('P', 0x50),
        Q('Q', 0x51),
        R('R', 0x52),
        S('S', 0x53),
        T('T', 0x54),
        U('U', 0x55),
        V('V', 0x56),
        W('W', 0x57),
        X('X', 0x58),
        Y('Y', 0x59),
        Z('Z', 0x5A),

        a('a', 0x61),
        b('b', 0x62),
        c('c', 0x63),
        d('d', 0x64),
        e('e', 0x65),
        f('f', 0x66),
        g('g', 0x67),
        h('h', 0x68),
        i('i', 0x69),
        j('j', 0x6A),
        k('k', 0x6B),
        l('l', 0x6C),
        m('m', 0x6D),
        n('n', 0x6E),
        o('o', 0x6F),
        p('p', 0x70),
        q('q', 0x71),
        r('r', 0x72),
        s('s', 0x73),
        t('t', 0x74),
        u('u', 0x75),
        v('v', 0x76),
        w('w', 0x77),
        x('x', 0x78),
        y('y', 0x79),
        z('z', 0x7A),

        EXCLAMATION_MARK('!', 0x21),
        DOUBLE_QUOTE('\"', 0x22),
        NUMBER_SIGN('#', 0x23),
        DOLLAR('$', 0x24),
        PERCENT('%', 0x25),
        AMPERSAND('&', 0x26),
        QUOTE_SINGLE('\'', 0x27),
        PARENTHESIS_LEFT('(', 0x28),
        PARENTHESIS_RIGHT(')', 0x29),
        ASTERISK('*', 0x2A),
        PLUS('+', 0x2B),
        COMMA(',', 0x2C),
        HYPHEN('-', 0x2D),
        PERIOD('.', 0x2E),
        SLASH('/', 0x2F),
        COLON(':', 0x3A),
        SEMICOLON(';', 0x3B),
        LESS('<', 0x3C),
        EQUAL('=', 0x3D),
        GREATER('>', 0x3E),
        QUESTION('?', 0x3F),
        AT('@', 0x40),
        BRACKET_LEFT('[', 0x5B),
        YEN('¥', 0x5C),
        BRACKET_RIGHT(']', 0x5D),
        CARET('^', 0x5E),
        UNDERSCORE('_', 0x5F),
        GRAV('`', 0x60),
        BRACE_LEFT('{', 0x7B),
        BAR('|', 0x7C),
        BRACE_RIGHT('}', 0x7D),
        ARROW_RIGHT('→', 0x7E),
        ARROW_LEFT('←', 0x7F),
        SQUARE('□', 0xA1),
        TOP_LEFT_CORNER('⌜', 0xA2),
        BOTTOM_RIGHT_CORNER('⌟', 0xA3),
        SMALL_BACKSLASH('﹨', 0xA4),
        KATAKANA_MIDPOINT('･', 0xA5),
        SMALL_ALPHA('α', 0xE0),
        LATIN_SMALL_A_WITH_DIAERESIS('ä', 0xE1),
        BIG_BETA('β', 0xE2),
        SMALL_EPSILON('ε', 0xE3),
        SMALL_MY('μ', 0xE4),
        SMALL_SIGMA('σ', 0xE5),
        SMALL_RHO('ρ', 0xE6),
        SQUARE_ROOT('√', 0xE8),
        LATIN_SMALL_O_WITH_DIAERESIS('ö', 0xEF),
        BIG_THETA('ϴ', 0xF2),
        INFINITY_SIGN('∞', 0xF3),
        BIG_OMEGA('Ω', 0xF4),
        LATIN_SMALL_U_WITH_DIAERESIS('ü', 0xF5),
        BIG_SIGMA('∑', 0xF6),
        SMALL_PI('π', 0xF7),
        SHIN('Ⴘ', 0xF9),
        TSHE('Ћ', 0xFB),
        DIVISION('÷', 0xFD),
        SPACE(' ', 0xFE),
        BLACKBOX('⏹', 0xFF),

        OWN_CHARACTER_1('\1', 0x01),
        OWN_CHARACTER_2('\2', 0x02),
        OWN_CHARACTER_3('\3', 0x03),
        OWN_CHARACTER_4('\4', 0x04),
        OWN_CHARACTER_5('\5', 0x05),
        OWN_CHARACTER_6('\6', 0x06),
        OWN_CHARACTER_7('\7', 0x07);

        /**
         * ASCII character to which this symbol belongs to or ? if no ASCII mapping is available
         */
        private final int ascii;

        /**
         * Byte representing the ASCII character on the LCD Display
         */
        private final int code;

        /**
         * Creates a new symbol associated to a specific ASCII character
         *
         * @param ascii ASCII character to be associated with
         * @param code  byte representing the chosen ASCII character on the LCD Display
         */
        Symbol(int ascii, int code) {
            this.ascii = ascii;
            this.code = code;
        }

        /**
         * Method to search a the corresponding byte to an ASCII sign. Returns a ? if a symbol is not found
         *
         * @param c ASCII Symbol
         * @return Byte needed to display the Symbol on the LCD Display
         */
        public static int getByChar(char c) {
            for (Symbol symbol : Symbol.values()) {
                if (symbol.ascii == c) {
                    return symbol.code;
                }
            }
            return QUESTION.code;
        }
    }
}
