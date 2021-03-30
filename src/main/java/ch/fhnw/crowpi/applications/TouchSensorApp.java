package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.TouchSensorComponent;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import com.pi4j.io.gpio.digital.DigitalStateChangeListener;

import static java.lang.Thread.sleep;

public class TouchSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        System.out.println("Reloading?");

        final var touchSensor = new TouchSensorComponent(pi4j);

        touchSensor.addListener(this::onTouched);

        for (int i = 0; i < 10; i++) {

            System.out.println("State: " + touchSensor.isTouched());

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean onTouched() {
        System.out.println("HO HO HO");

        return null;
    }
}
