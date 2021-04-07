package ch.fhnw.crowpi.components.internal;

import com.pi4j.io.spi.Spi;

import java.util.Arrays;

public class MAX7219 {
    private static final byte CMD_SET_FIRST_ROW = 0x01;
    private static final byte CMD_DECODE_MODE = 0x09;
    private static final byte CMD_INTENSITY = 0x0A;
    private static final byte CMD_SCAN_LIMIT = 0x0B;
    private static final byte CMD_SHUTDOWN = 0x0C;
    private static final byte CMD_DISPLAY_TEST = 0x0F;

    protected static final int WIDTH = 8;
    protected static final int HEIGHT = 8;

    protected final byte[] buffer = new byte[HEIGHT];
    protected final Spi spi;

    public MAX7219(Spi spi) {
        this.spi = spi;
    }

    public void clear() {
        Arrays.fill(buffer, (byte) 0);
    }

    public void refresh() {
        for (int row = 0; row < HEIGHT; row++) {
            refreshRow(row);
        }
    }

    protected void refreshRow(int row) {
        if (row < 0 || row > HEIGHT) {
            throw new IllegalArgumentException("Row must be an integer in the range 0-" + HEIGHT);
        }

        execute((byte) (CMD_SET_FIRST_ROW + row), buffer[row]);
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            execute(CMD_SHUTDOWN, (byte) 0x01);
            // FIXME: Move into separate functions... maybe?
            execute(CMD_DECODE_MODE, (byte) 0x00);
            execute(CMD_SCAN_LIMIT, (byte) 0x07);
        } else {
            execute(CMD_SHUTDOWN, (byte) 0x00);
        }
    }

    public void setTestMode(boolean enabled) {
        execute(CMD_DISPLAY_TEST, (byte) (enabled ? 0x01 : 0x00));
    }

    public void setBrightness(int brightness) {
        if (brightness < 0 || brightness > 15) {
            throw new IllegalArgumentException("Brightness must be an integer in the range 0-15");
        }
        execute(CMD_INTENSITY, (byte) brightness);
    }

    public void setPixel(int x, int y, boolean enabled) {
        final byte mask = (byte) (1 << (WIDTH - 1 - x));
        if (enabled) {
            buffer[y] |= mask;
        } else {
            buffer[y] &= ~mask;
        }
    }

    public boolean getPixel(int x, int y) {
        final byte mask = (byte) (1 << (WIDTH - 1 - x));
        return (buffer[y] & mask) != 0;
    }

    private void execute(byte command, byte data) {
        spi.write(command, data);
    }
}
