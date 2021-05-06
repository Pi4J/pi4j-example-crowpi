package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.PirMotionSensorComponent;

import java.time.LocalDateTime;

/**
 * Waits for the motion sensor to report stillstand before activating a simple alarm system for 30 seconds. Whenever the state of the
 * motion sensor changes, an alarm or recovery message with the current timestamp will be printed on the console.
 */
public class PirMotionSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Create new PIR motion sensor component
        final var motionSensor = new PirMotionSensorComponent(pi4j);

        // Wait for stillstand before activating alarm system
        while (!motionSensor.hasStillstand()) {
            System.out.println("Waiting for motion sensor to detect stillstand...");
            sleep(1000);
        }
        System.out.println("Alarm system activated, watching for movement...");

        // Register event listeners to react to movement and stillstand
        // Those will be asynchronously called and do not block the application itself
        motionSensor.onMovement(() -> {
            final var timestamp = LocalDateTime.now();
            System.out.printf("[%s] ALARM - Movement has been detected\n", timestamp);
        });
        motionSensor.onStillstand(() -> {
            final var timestamp = LocalDateTime.now();
            System.out.printf("[%s] RECOVERY - No more movement detected\n", timestamp);
        });

        // Wait 30 seconds before exiting this application
        for (int i = 30; i > 0; i--) {
            System.out.println("Waiting for " + i + " second(s) before exiting...");
            sleep(1000);
        }

        // Cleanup the event listeners before exiting
        motionSensor.onMovement(null);
        motionSensor.onStillstand(null);
    }
}
