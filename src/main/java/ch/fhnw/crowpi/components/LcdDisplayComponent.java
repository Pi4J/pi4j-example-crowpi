package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.components.events.DigitalEventProvider;
import ch.fhnw.crowpi.components.internal.MCP23008;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

// Diese Ding benutzt IO via MCP23008.
// Also I2C -> MCP23008 -> IO -> Display
// Brauch MCP23008 die richtig auf IO schreibt
// braucht komponente die richtig auf MCP schreibt
// helligkeit nur mit poti
public class LcdDisplayComponent {
    private final MCP23008 mcp;

    /**
     * Those default address are to use this class with default CrowPi setup
     */
    private static final int DEFAULT_BUS = 0x1;
    private static final int DEFAULT_DEVICE = 0x21;

    /**
     * Creates a new light sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public LcdDisplayComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    public LcdDisplayComponent(Context pi4j, int bus, int device) {
        this.mcp = new MCP23008(pi4j, bus, device);
        this.mcp.initializeIo(MCP_IO_CONFIG);
        this.initializeLcd();
    }

    public void play() {
        write8('A', true);
    }

    protected void initializeLcd() {
        // Enable backlight
        mcp.setAndWritePin(LCD_LIGHT, true);

        // Initialize display
        write8((byte) 0b001_10011);
        write8((byte) 0b001_10010);

        // Initialize display settings
        byte displayControl = (byte) (LCD_DISPLAYON | LCD_CURSORON | LCD_BLINKOFF);
        byte displayFunction = (byte) (LCD_4BITMODE | LCD_1LINE | LCD_2LINE | LCD_5x8DOTS);
        byte displayMode = (byte) (LCD_ENTRYLEFT | LCD_ENTRYSHIFTDECREMENT);


        execute(LCD_DISPLAYCONTROL, displayControl);
        // write8((byte) (LCD_DISPLAYCONTROL | displayControl));
        write8((byte) (LCD_FUNCTIONSET | displayFunction));
        write8((byte) (LCD_ENTRYMODESET | displayMode));

        // Clear display
        write8(LCD_CLEARDISPLAY);
        sleep(3);

        // Move the cursor to its home position
        write8(LCD_RETURNHOME);
        sleep(3);
    }

    public void execute(byte command, byte data) {
        write8((byte) (command | data));
    }

    public void write8(int c) {
        write8(c, false);
    }

    public void write8(int b, boolean charMode) {
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

    public void message(String s) {
        for (int i = 0; i < s.length(); i++) {
            write8(s.charAt(i), true);
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // Default MCP Configuration
    byte MCP_IO_CONFIG = 0x00;

    // Default Pulsewidth
    private final int DEFAULT_PULSE_WIDTH = 1;

    // Commands
    byte LCD_CLEARDISPLAY = 0x01;
    byte LCD_RETURNHOME = 0x02;
    byte LCD_ENTRYMODESET = 0x04;
    byte LCD_DISPLAYCONTROL = 0x08;
    byte LCD_CURSORSHIFT = 0x10;
    byte LCD_FUNCTIONSET = 0x20;
    byte LCD_SETCGRAMADDR = 0x40;
    //byte LCD_SETDDRAMADDR = 0x80;


    // Entry flags
    byte LCD_ENTRYRIGHT = 0x00;
    byte LCD_ENTRYLEFT = 0x02;
    byte LCD_ENTRYSHIFTINCREMENT = 0x01;
    byte LCD_ENTRYSHIFTDECREMENT = 0x00;


    // Control flags
    byte LCD_DISPLAYON           =0x04;
    byte LCD_DISPLAYOFF          =0x00;
    byte LCD_CURSORON            =0x02;
    byte LCD_CURSOROFF           =0x00;
    byte LCD_BLINKON             =0x01;
    byte LCD_BLINKOFF            =0x00;

    // Move flags
    byte LCD_DISPLAYMOVE         =0x08;
    byte LCD_CURSORMOVE          =0x00;
    byte LCD_MOVERIGHT           =0x04;
    byte LCD_MOVELEFT            =0x00;

    // Function set flags
    byte LCD_8BITMODE           = 0x10;
    byte LCD_4BITMODE            =0x00;
    byte LCD_2LINE               =0x08;
    byte LCD_1LINE               =0x00;
    byte LCD_5x10DOTS            =0x04;
    byte LCD_5x8DOTS             =0x00;

    // Offset forup to 4 rows.
    byte[] LCD_ROW_OFFSETS         ={0x00,0x40,0x14,0x54};

    // Char LCDplate buttonnames.
   int SELECT                  =0;
   int RIGHT                   =1;
   int DOWN                    =2;
   int UP                      =3;
   int LEFT                    =4;

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
}
