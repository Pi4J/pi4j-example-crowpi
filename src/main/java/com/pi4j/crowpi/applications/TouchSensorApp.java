package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.TouchSensorComponent;

/**
 * Writes some Text output on Touch Sensor Events. After 20 seconds event handling is disable and the App waits for
 * termination by a final touch sensor press.
 */
public class TouchSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // create a touch sensor instance
        final var touchSensor = new TouchSensorComponent(pi4j);

        // create two listeners for detecting touch events
        touchSensor.onTouch(() -> System.out.println("Seems like you are touching the sensor!"));
        touchSensor.onRelease(() -> System.out.println("You stopped touching the sensor!"));

        // just any delay
        System.out.println("Touch Sensor is now activated.");
        for (int i = 20; i > 0; i--) {
            System.out.println("Time until event listeners are killed: " + i + " seconds...");
            sleep(1000);
        }

        // disable the event listeners we no longer need
        touchSensor.onTouch(null);
        touchSensor.onRelease(null);

        // end the program as soon as isTouched returns true
        System.out.println("Press again to end this application");
        while (!touchSensor.isTouched()) {
            sleep(10);
        }
    }
}
