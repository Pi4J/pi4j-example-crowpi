package ch.fhnw.crowpi.components.internal;

import com.pi4j.io.i2c.I2C;

public class MCP23008 {
    private final I2C i2c;

    /**
     * IODIR register controls the direction of the GPIO on the port expander
     */
    private final byte IODIR_REGISTER_ADDRESS = 0x00;
    /**
     * GPIO register is used to read the pins input
     */
    private final byte GPIO_REGISTER_ADDRESS = 0x09; //
    /**
     * Output Latch register is used to set the pins output high/low
     */
    private final byte OLAT_REGISTER_ADDRESS = 0x0A;

    /**
     * Pin out LCD auf MCP
     */
    private final byte LCD_BACKPACK_RS = 1;
    private final byte LCD_BACKPACK_EN = 2;
    private final byte LCD_BACKPACK_D4 = 3;
    private final byte LCD_BACKPACK_D5 = 4;
    private final byte LCD_BACKPACK_D6 = 5;
    private final byte LCD_BACKPACK_D7 = 6;
    private final byte LCD_BACKPACK_LIGHT = 7;

    /**
     * Commands
     */
    byte LCD_CLEARDISPLAY = 0x01;
    byte LCD_RETURNHOME = 0x02;
    byte LCD_ENTRYMODESET = 0x04;
    byte LCD_DISPLAYCONTROL = 0x08;
    byte LCD_CURSORSHIFT = 0x10;
    byte LCD_FUNCTIONSET = 0x20;
    byte LCD_SETCGRAMADDR = 0x40;
    byte LCD_SETDDRAMADDR = (byte) 0x80;

    /**
     * Control Flags
     */
    byte LCD_DISPLAYON = 0x04;
    byte LCD_DISPLAYOFF = 0x00;
    byte LCD_CURSORON = 0x02;
    byte LCD_CURSOROFF = 0x00;
    byte LCD_BLINKON = 0x01;
    byte LCD_BLINKOFF = 0x00;

    /**
     * Function Set Flags
     */
    byte LCD_8BITMODE = 0x10;
    byte LCD_4BITMODE = 0x00;
    byte LCD_2LINE = 0x08;
    byte LCD_1LINE = 0x00;
    byte LCD_5x10DOTS = 0x04;
    byte LCD_5x8DOTS = 0x00;

    /**
     * Entry Flags
     */
    byte LCD_ENTRYRIGHT = 0x00;
    byte LCD_ENTRYLEFT = 0x02;
    byte LCD_ENTRYSHIFTINCREMENT = 0x01;
    byte LCD_ENTRYSHIFTDECREMENT = 0x00;

    public MCP23008(I2C i2c) {
        this.i2c = i2c;
    }

    public void writeByte(byte b) {
        write(b, false);
    }

    public void write(byte b, boolean charMode) {


        // Set Character or Data bit


        // Write upper 4 bits

        // Write lower 4 bits

    }

    public void execute(byte command, byte data) {
        write8((byte) (command | data));
    }

    public void initializeIo() {
        // Setup all ports as output
        i2c.writeRegister(IODIR_REGISTER_ADDRESS, 0x00);

        // Enable backlight
        setPin(LCD_BACKPACK_LIGHT, true);

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

        // MEGA TEST
        message("T" + (char) (0b1110_1111) + "bi du Banane");
    }

    public void message(String s) {
        for (int i = 0; i < s.length(); i++) {
            System.out.println("Write Character: " + s.charAt(i));
            write8(s.charAt(i), true);
        }
    }

    public void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void write8(int c) {
        write8(c, false);
    }

    public void write8(int c, boolean charmode) {
        byte b = (byte) c;
        sleep(1);
        setPin(LCD_BACKPACK_RS, charmode);
        setPin(LCD_BACKPACK_D4, (b & 0b0001_0000) > 0);
        setPin(LCD_BACKPACK_D5, (b & 0b0010_0000) > 0);
        setPin(LCD_BACKPACK_D6, (b & 0b0100_0000) > 0);
        setPin(LCD_BACKPACK_D7, (b & 0b1000_0000) > 0);
        pulseEnable();
        setPin(LCD_BACKPACK_D4, (b & 0b0000_0001) > 0);
        setPin(LCD_BACKPACK_D5, (b & 0b0000_0010) > 0);
        setPin(LCD_BACKPACK_D6, (b & 0b0000_0100) > 0);
        setPin(LCD_BACKPACK_D7, (b & 0b0000_1000) > 0);
        pulseEnable();
    }

    public void pulseEnable() {
        setPin(LCD_BACKPACK_EN, false);
        sleep(1);
        setPin(LCD_BACKPACK_EN, true);
        sleep(1);
        setPin(LCD_BACKPACK_EN, false);
        sleep(1);
    }

    protected byte gpioState = 0x00;

    public void setPin(int bit, boolean state) {
        // byte currentIo = (byte) i2c.readRegister(GPIO_REGISTER_ADDRESS);
        // sleep(1);

        System.out.println("PIN Read: " + Integer.toBinaryString(gpioState));

        if (state) {
            gpioState |= (1 << bit);
        } else {
            gpioState &= ~(1 << bit);
        }

        System.out.println("PIN Write: " + Integer.toBinaryString(gpioState));

        i2c.writeRegister(GPIO_REGISTER_ADDRESS, gpioState);
        sleep(1);
    }
}
