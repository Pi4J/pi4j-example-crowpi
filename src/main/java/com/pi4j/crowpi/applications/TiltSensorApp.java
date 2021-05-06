package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.TiltSensorComponent;

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

        // Register two event listeners to be notified when the tilt changes
        // These handlers will be asynchronously called and do not block the application itself
        tiltSensor.onTiltLeft(() -> System.out.println("<<< Left Tilt <<<"));
        tiltSensor.onTiltRight(() -> System.out.println(">>> Right Tilt >>>"));

        // Register an additional event listener to detect shaking
        tiltSensor.onShake(() -> System.out.println("!!! Shaking !!!"));

        // Wait for 20 seconds before this application exits
        for (int i = 20; i > 0; i--) {
            System.out.println("Waiting for " + i + " second(s) before exiting... Tilt your CrowPi before it is too late :-)");
            sleep(1000);
        }

        // Cleanup by disabling the event listeners
        tiltSensor.onTiltLeft(null);
        tiltSensor.onTiltRight(null);
        tiltSensor.onShake(null);
    }
}
