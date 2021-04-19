package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.internal.MCP23008;
import com.pi4j.context.Context;

// Diese Ding benutzt IO via MCP23008.
// Also I2C -> MCP23008 -> IO -> Display
// Brauch MCP23008 die richtig auf IO schreibt
// braucht komponente die richtig auf MCP schreibt
// helligkeit nur mit poti
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
     * Default Pulsewidth in Millis
     */
    private final int DEFAULT_PULSE_WIDTH = 1;

    /**
     * Creates a new LCD Display component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public LcdDisplayComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new  LCD Display component with custom bus, device address
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
        // Enable backlight
        setDisplayBacklight(true);

        // Initialize display
        write((byte) 0b001_10011);
        write((byte) 0b001_10010);

        // Initialize display settings
        byte displayControl = (byte) (LCD_DISPLAYON | LCD_CURSOROFF | LCD_BLINKOFF);
        byte displayFunction = (byte) (LCD_4BITMODE | LCD_1LINE | LCD_2LINE | LCD_5x8DOTS);
        byte displayMode = (byte) (LCD_ENTRYLEFT | LCD_ENTRYSHIFTDECREMENT);

        // Write Display settings
        executeCommand(LCD_DISPLAYCONTROL, displayControl);
        write((byte) (LCD_FUNCTIONSET | displayFunction));
        write((byte) (LCD_ENTRYMODESET | displayMode));

        // Clear display
        clearDisplay();
    }

    /**
     * Write a Line of Text on the LCD Display
     *
     * @param text Text to display
     * @param line Select Line of Display
     */
    public void writeLine(String text, int line) {
        returnHome();
        clearLine(line);
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

        // Before writing set Cursor to line 1
        setCursorToLine(1);

        // Iterate through characters and write them to the display
        for (int i = 0; i < text.length(); i++) {
            // line break in text found
            if (text.charAt(i) == '\n') {
                setCursorToLine(2);
                continue;
            }

            // Write character to display
            write(Symbol.getByChar(text.charAt(i)), true);

            // Was last character on first line? switch to second
            if (i == 15) {
                setCursorToLine(2);
            }
        }
    }

    /**
     * Set the cursor to line 1 or 2
     *
     * @param number Sets the cursor to this line. Only Range 1-2 allowed.
     */
    public void setCursorToLine(int number) {
        if (number > 2 || number < 1) {
            throw new IllegalArgumentException("CrowPi Display has only 2 Rows!");
        }

        executeCommand(LCD_SETDDRAMADDR, LCD_ROW_OFFSETS[number - 1]);
    }

    public void createOwnCharacter(int location, byte[] character) {
        if (character.length > 7) {
            throw new IllegalArgumentException("Array to long. Character is only 5x8 Digits. Only a array with length" +
                " 8 is allowed");
        }

        location &= 0x7;
        write(LCD_SETCGRAMADDR | (location << 3));

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
        write(LCD_CLEARDISPLAY);
        sleep(3);
        returnHome();
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
     * Returns the Cursor to Home Position (First line, first character)
     */
    public void returnHome() {
        write(LCD_RETURNHOME);
        sleep(3);
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
    public void write(int c) {
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
        mcp.pulsePin(LCD_EN, DEFAULT_PULSE_WIDTH);

        // low nibble
        mcp.setPin(LCD_D4, (b & 0b0000_0001) > 0);
        mcp.setPin(LCD_D5, (b & 0b0000_0010) > 0);
        mcp.setPin(LCD_D6, (b & 0b0000_0100) > 0);
        mcp.setPin(LCD_D7, (b & 0b0000_1000) > 0);
        mcp.writePins();
        mcp.pulsePin(LCD_EN, DEFAULT_PULSE_WIDTH);
    }

    // Default MCP Configuration
    byte MCP_IO_CONFIG = 0x00;

    // Commands
    byte LCD_CLEARDISPLAY = 0x01;
    byte LCD_RETURNHOME = 0x02;
    byte LCD_ENTRYMODESET = 0x04;
    byte LCD_DISPLAYCONTROL = 0x08;
    byte LCD_CURSORSHIFT = 0x10;
    byte LCD_FUNCTIONSET = 0x20;
    byte LCD_SETCGRAMADDR = 0x40;
    byte LCD_SETDDRAMADDR = (byte) 0x80;

    // Entry flags
    byte LCD_ENTRYRIGHT = 0x00;
    byte LCD_ENTRYLEFT = 0x02;
    byte LCD_ENTRYSHIFTINCREMENT = 0x01;
    byte LCD_ENTRYSHIFTDECREMENT = 0x00;

    // Control flags
    byte LCD_DISPLAYON = 0x04;
    byte LCD_DISPLAYOFF = 0x00;
    byte LCD_CURSORON = 0x02;
    byte LCD_CURSOROFF = 0x00;
    byte LCD_BLINKON = 0x01;
    byte LCD_BLINKOFF = 0x00;

    // Move flags
    byte LCD_DISPLAYMOVE = 0x08;
    byte LCD_CURSORMOVE = 0x00;
    byte LCD_MOVERIGHT = 0x04;
    byte LCD_MOVELEFT = 0x00;

    // Function set flags
    byte LCD_8BITMODE = 0x10;
    byte LCD_4BITMODE = 0x00;
    byte LCD_2LINE = 0x08;
    byte LCD_1LINE = 0x00;
    byte LCD_5x10DOTS = 0x04;
    byte LCD_5x8DOTS = 0x00;

    // Offset forup to 4 rows.
    byte[] LCD_ROW_OFFSETS = {0x00, 0x40, 0x14, 0x54};

    /**
     * Pin out LCD auf MCP
     */
    private final int LCD_RS = 1;
    private final int LCD_EN = 2;
    private final int LCD_D4 = 3;
    private final int LCD_D5 = 4;
    private final int LCD_D6 = 5;
    private final int LCD_D7 = 6;
    private final int LCD_LIGHT = 7;

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
        BLACKBOX('⏹', 0xFF);

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
