package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.LightSensorComponent;

/**
 * Measures the current illuminance with the CrowPi Light sensor and prints some outputs to the user.
 */
public class LightSensorApp implements Application {

    /**
     * Define a measurement interval in millis (to short interval can't be handled by the sensor)
     */
    private static final int DELAY = 1000;
    /**
     * Define a number of loops until the App shuts down
     */
    private static final int NUMBER_OF_LOOPS = 20;
    /**
     * Define lower light limit to work with
     */
    private static final double DARK_VALUE = 150;
    /**
     * Define upper light limit to work with
     */
    private static final double BRIGHT_VALUE = 3000;

    @Override
    public void execute(Context pi4j) {
        // Initialize a new Light Sensor
        final var sensor = new LightSensorComponent(pi4j);

        // Start the measurements:
        System.out.println("Starting new measurements ...");

        // Loop until number of loops is reached
        for (int i = 0; i < NUMBER_OF_LOOPS; i++) {
            // Read the current illumination from sensor
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
            sleep(DELAY);
        }
    }
}
