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
    private final I2C i2c;
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
        this.i2c = pi4j.create(buildI2CConfig(pi4j, bus, device));
        this.mcp = new MCP23008(i2c);
        play();
    }

    public void play() {
        mcp.write((byte) 0, false);

        mcp.initializeIo();
    }

    /**
     * Builds a new I2C instance for the seven-segment display
     *
     * @param pi4j   Pi4J context
     * @param bus    Bus address
     * @param device Device address
     * @return I2C instance
     */
    private static I2CConfig buildI2CConfig(Context pi4j, int bus, int device) {
        return I2C.newConfigBuilder(pi4j)
            .id("I2C-" + device + "@" + bus)
            .name("LCD Display")
            .bus(bus)
            .device(device)
            .build();
    }

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

    // Char LCD plate GPIO numbers.
    int LCD_PLATE_RS            =15;
    int LCD_PLATE_RW            =14;
    int LCD_PLATE_EN            =13;
    int LCD_PLATE_D4            =12;
    int LCD_PLATE_D5            =11;
    int LCD_PLATE_D6            =10;
    int LCD_PLATE_D7            =9;
    int LCD_PLATE_RED           =6;
    int LCD_PLATE_GREEN         =7;
    int LCD_PLATE_BLUE          =8;

    // Char LCDplate buttonnames.
   int SELECT                  =0;
   int RIGHT                   =1;
   int DOWN                    =2;
   int UP                      =3;
   int LEFT                    =4;

    // Char LCD backpack GPIO numbers.
   int LCD_BACKPACK_RS         =1;
   int LCD_BACKPACK_EN         =2;
   int LCD_BACKPACK_D4         =3;
   int LCD_BACKPACK_D5         =4;
   int LCD_BACKPACK_D6         =5;
   int LCD_BACKPACK_D7         =6;
   int LCD_BACKPACK_LITE       =7;
}
