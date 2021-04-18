package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.LcdDisplayComponent;
import com.pi4j.context.Context;

public class LcdDisplayApp implements Application {
    @Override
    public void execute(Context pi4j) {
        LcdDisplayComponent lcd = new LcdDisplayComponent(pi4j);
        System.out.println("CrowPi with Pi4J rocks!");

        lcd.play();
    }
}
