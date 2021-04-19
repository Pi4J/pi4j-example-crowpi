package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.LcdDisplayComponent;
import com.pi4j.context.Context;

public class LcdDisplayApp implements Application {
    @Override
    public void execute(Context pi4j) {
        LcdDisplayComponent lcd = new LcdDisplayComponent(pi4j);
        System.out.println("CrowPi with Pi4J rocks!");

        lcd.initialize();

        lcd.writeText("ABC\nQRSTUVWXYZ123456");

        sleep(2000);

        lcd.writeLine("<!?dfjäeüöü>", 2);

        sleep(2000);
        lcd.writeLine("First Line", 1);

        sleep(2000);
        lcd.clearLine(1);
        lcd.writeLine("EY GEHT DOCH!", 2);

        sleep(5000);
        lcd.clearDisplay();

        lcd.createOwnCharacter(0x01, new byte[] {
            0b01111,
            0b10111,
            0b11011,
            0b11101,
            0b11110,
            0b11101,
            0b11011,
            0b10111,
        });

        lcd.createOwnCharacter(0x02, new byte[] {
                0b11111,
                0b11111,
                0b00000,
                0b11101,
                0b11110,
                0b11101,
                0b11011,
                0b10111,
        });

        lcd.returnHome();
        lcd.write(0x01, true);
        lcd.write(0x02, true);
    }
}
