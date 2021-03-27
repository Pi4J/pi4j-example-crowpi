package ch.fhnw.crowpi.components.internal;

import com.pi4j.io.i2c.I2C;

import java.util.Arrays;

public class HT16K33 {
    private static final int BUFFER_SIZE = 16;

    // HT16K33: Display Data Command
    private static final int CMD_DISPLAY_DATA = 0b0000;

    // HT16K33: Dimming Set Command
    private static final int CMD_DIMMING_SET = 0b1110;

    // HT16K33: System Setup Command
    private static final int CMD_SYSTEM_SETUP = 0b0010;
    private static final int SET_OSCILLATOR_ON = 0b0001;
    private static final int SET_OSCILLATOR_OFF = 0b0000;

    // HT16K33: Display Setup Command
    private static final int CMD_DISPLAY_SETUP = 0b1000;
    private static final int SET_DISPLAY_ON = 0b0001;
    private static final int SET_DISPLAY_OFF = 0b0000;

    private final I2C i2c;
    protected final byte[] buffer = new byte[BUFFER_SIZE];

    public HT16K33(I2C i2c) {
        this.i2c = i2c;
    }

    public void clear() {
        Arrays.fill(buffer, (byte) 0);
    }

    public void refresh() {
        execute(CMD_DISPLAY_DATA, 0);
        i2c.writeRegister(0, buffer, BUFFER_SIZE);
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            execute(CMD_SYSTEM_SETUP, SET_OSCILLATOR_ON);
            execute(CMD_DISPLAY_SETUP, SET_DISPLAY_ON);
        } else {
            execute(CMD_SYSTEM_SETUP, SET_OSCILLATOR_OFF);
            execute(CMD_DISPLAY_SETUP, SET_DISPLAY_OFF);
        }
    }

    public void setBlinkRate(int rate) {
        if (rate < 0 || rate > 3)
            throw new IllegalArgumentException("Blink rate must be an integer in the range 0-3");
        execute(CMD_DISPLAY_SETUP, SET_DISPLAY_ON | (rate << 1));
    }

    public void setBrightness(int brightness) {
        if (brightness < 0 || brightness > 15)
            throw new IllegalArgumentException("Brightness must be an integer in the range 0-15");
        execute(CMD_DIMMING_SET, brightness);
    }

    private void execute(int command, int setting) {
        if (command < 0 || command > 0xF)
            throw new IllegalArgumentException("Command must be nibble");
        if (setting < 0 || setting > 0xF)
            throw new IllegalArgumentException("Setting must be nibble");

        final var address = (command << 4) | setting;
        i2c.writeRegister(address, 0);
    }
}
