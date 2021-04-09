package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.PirMotionSensorComponent;
import com.pi4j.context.Context;

import java.time.LocalDateTime;

/**
 * Waits for the motion sensor to report stillstand before activating a simple alarm system for 30 seconds. Whenever the state of the
 * motion sensor changes, an alarm message with the current timestamp will be printed on the console.
 */
public class PirMotionSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Create new PIR motion sensor component
        final var motionSensor = new PirMotionSensorComponent(pi4j);

        // Wait for stillstand before activating alarm system
        while (!motionSensor.hasStillstand()) {
            System.out.println("Waiting for motion sensor to detect stillstand...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        System.out.println("Alarm system activated, watching for movement...");

        // Register listener to react to any state changes
        // This will be asynchronously called and does not block the application itself
        final var eventListener = motionSensor.addListener((listener, state) -> {
            final var timestamp = LocalDateTime.now();
            System.out.printf("[%s] ALARM - State of motion sensor changed: %s\n", timestamp, state);
        });

        // Wait 30 seconds before exiting this application
        for (int i = 30; i > 0; i--) {
            System.out.println("Waiting for " + i + " second(s) before exiting...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        // Cleanup the event listener before exiting
        eventListener.remove();
    }
}
