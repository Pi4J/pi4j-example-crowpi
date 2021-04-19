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
        lcd.setDisplayBacklight(false);
    }
}
