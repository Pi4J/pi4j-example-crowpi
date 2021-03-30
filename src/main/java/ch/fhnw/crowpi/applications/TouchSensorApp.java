package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.TouchSensorComponent;
import com.pi4j.context.Context;

import static java.lang.Thread.sleep;

public class TouchSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        System.out.println("Reloading?");

        final var touchSensor = new TouchSensorComponent(pi4j);

        Object listenerObject = touchSensor.addListener(state ->  {
            System.out.println("State Changed! New State: " + state);
        });

        for (int i = 0; i < 10; i++) {

            System.out.println("Loop1: " + touchSensor.isTouched());

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        touchSensor.removeListener(listenerObject);

        for (int i = 0; i < 10; i++) {

            System.out.println("Loop2: " + touchSensor.isTouched());

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
