package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.LightSensorComponent;
import com.pi4j.context.Context;

public class LightSensorApp implements Application {

    @Override
    public void execute(Context pi4j) {
        // Initialize a new Light Sensor
        final var sensor = new LightSensorComponent(pi4j);

        // Define a measurement interval in milliseconds (sensor does not support too short intervals)
        final int DELAY = 1000;
        // Define a number of loops until the App shuts down
        final int NUMBER_OF_LOOPS = 20;
        // Define some light limits to work with
        final double DARK_VALUE = 150;
        final double BRIGHT_VALUE = 3000;

        // Start the measurements:
        System.out.println("Starting new measurements ...");

        // Loop until number of loops is reached
        for (int i = 0; i < NUMBER_OF_LOOPS; i++) {
            // Loop a measurement until a button is pressed
            double value = sensor.readLight(2);

            // Analyze measured value and react accordingly
            if (value < DARK_VALUE) {
                System.out.println("Whoo that's dark... You should turn on light");
            } else if (value >= DARK_VALUE && value < BRIGHT_VALUE) {
                System.out.println("I feel good. Very nice light in here!");
            } else if (value >= BRIGHT_VALUE) {
                System.out.println("Oh no .. it's so bright ... please ... please turn off the light");
            }

            // Sleep before continuing with next measurement
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
