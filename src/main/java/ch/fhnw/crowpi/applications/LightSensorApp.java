package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.LightSensorComponent;
import com.pi4j.context.Context;

public class LightSensorApp implements Application {

    @Override
    public void execute(Context pi4j) {
        // Initialize a new Light Sensor
        final var sensor = new LightSensorComponent(pi4j);

        // Define a measurement interval in millis (to short interval can't be handled by the sensor)
        final int DELAY = 1000;
        // Define a number of loops until the App shuts down
        final int NUMBER_OF_LOOPS = 20;
        // Define some light limits to work with
        final double DARK_VALUE = 150;
        final double BRIGHT_VALUE = 3000;

        // Start the measurements:
        System.out.println("Starting new Measurements ...");

        // Loop until number of loops is reached
        for (int i = 0; i < NUMBER_OF_LOOPS; i++) {
            // Loop a measurement until a button is pressed
            double value = sensor.readLight(2);

            // Work with the measure value to define some actions
            if (value < DARK_VALUE) {
                System.out.println("Whoo that's dark... You should turn on light");
            } else if (value >= DARK_VALUE && value < BRIGHT_VALUE) {
                System.out.println("I feel good. Very nice light in here!");
            } else if (value >= BRIGHT_VALUE) {
                System.out.println("Oh no .. it's so bright ... please ... please turn off the light");
            }

            // Delay the thread
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
