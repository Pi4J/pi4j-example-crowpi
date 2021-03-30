package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.TouchSensorComponent;
import com.pi4j.context.Context;

import static java.lang.Thread.sleep;

public class TouchSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        System.out.println("Touch Sensor is now activated.");

        final var touchSensor = new TouchSensorComponent(pi4j);

        Object listenerObject = touchSensor.addListener(state -> {
            System.out.println("State Changed! New State: " + state);
        });

        for (int i = 20; i > 0; i--) {
            System.out.println("Time until Eventlistener is killed: " + i + " seconds...");

            try {
                sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        touchSensor.removeListener(listenerObject);

        System.out.println("Press again to end this application");

        while (!touchSensor.isTouched()) {
            try {
                sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
