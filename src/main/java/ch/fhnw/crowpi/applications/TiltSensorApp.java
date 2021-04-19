package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.TiltSensorComponent;
import com.pi4j.context.Context;

/**
 * Prints the initial state of the tilt sensor and then informs about further state changes. To do so, you can just tilt the CrowPi to the
 * left and right side. Please note that this component requires the DIP switch 2-2 to be enabled, otherwise this example will not work
 * properly. After 20 seconds the application will be automatically stopped.
 */
public class TiltSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Create new tilt sensor component
        final var tiltSensor = new TiltSensorComponent(pi4j);

        // Print the current state of the tilt sensor
        System.out.println("Current state of tilt sensor: " + tiltSensor.getState());
        System.out.println("Is the CrowPi tilted left: " + tiltSensor.hasLeftTilt());
        System.out.println("Is the CrowPi tilted right: " + tiltSensor.hasRightTilt());

        // Register an event listener to be notified when the sensor changes
        // This will be asynchronously called and does not block the application itself
        final var eventListener = tiltSensor.addListener((listener, state) -> {
            System.out.println("Tilt State: " + state);
        });

        // Wait for 20 seconds before this application exits
        for (int i = 20; i > 0; i--) {
            System.out.println("Waiting for " + i + " second(s) before exiting... Tilt your CrowPi before it is too late :-)");
            sleep(1000);
        }

        // Cleanup by removing the event listener - while not needed, this is definitely the recommended way of doing things
        eventListener.remove();
    }
}
