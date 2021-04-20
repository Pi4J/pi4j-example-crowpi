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

        sleep(1000);

        lcd.writeLine("<!?dfjäeüöü>", 2);

        sleep(1000);
        lcd.writeLine("First Line", 1);

        sleep(1000);
        lcd.clearLine(1);
        lcd.writeLine("EY GEHT DOCH!", 2);
        lcd.setCursorVisibility(true);
        lcd.setCursorBlinking(true);

        lcd.returnHome();
        sleep(2000);
        lcd.moveDisplayRight();
        sleep(2000);
        lcd.moveDisplayLeft();
        sleep(2000);
        lcd.moveCursorRight();
        sleep(2000);

        sleep(2000);
        lcd.clearDisplay();

        lcd.createCharacter(1, new byte[]{
            0b01111,
            0b10111,
            0b11011,
            0b11101,
            0b11110,
            0b11101,
            0b11011,
            0b10111,
        });

        lcd.createCharacter(2, new byte[]{
            0b11111,
            0b11111,
            0b00000,
            0b11101,
            0b11110,
            0b11101,
            0b11011,
            0b10111,
        });


        lcd.writeCharacter('\1', 0, 2);
        sleep(1000);
        lcd.setCursorBlinking(false);
        lcd.writeCharacter('\1', 15 , 2);
        sleep(1000);
        lcd.setCursorVisibility(false);

        sleep(1000);

        lcd.writeText("c \1 b \2 a");
    }
}
