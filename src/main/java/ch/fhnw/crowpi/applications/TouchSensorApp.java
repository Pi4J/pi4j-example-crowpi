package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.TouchSensorComponent;
import com.pi4j.context.Context;

/**
 * Writes some Text output on Button Events. After 20 seconds event handling is disable and the App waits for
 * termination by a final touch sensor press.
 */
public class TouchSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // create a touch sensor instance
        final var touchSensor = new TouchSensorComponent(pi4j);

        // create a listener to handle touch events
        final var eventListener = touchSensor.addListener((listener, state) -> {
            System.out.println("State Changed! New State: " + state);
        });

        System.out.println("Touch Sensor is now activated.");

        // just any delay
        for (int i = 20; i > 0; i--) {
            System.out.println("Time until Eventlistener is killed: " + i + " seconds...");
            sleep(1000);
        }

        // remove the created listener because we do not need it anymore
        eventListener.remove();

        // end the program as soon as isTouched returns true
        System.out.println("Press again to end this application");
        while (!touchSensor.isTouched()) {
            sleep(10);
        }
    }
}
