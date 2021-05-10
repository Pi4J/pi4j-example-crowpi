package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.UltrasonicDistanceSensorComponent;
import com.pi4j.crowpi.components.exceptions.MeasurementException;

/**
 * Example Application of using the Crow Pi Ultrasonic Distance Sensor.
 */
public class UltrasonicDistanceSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Create new tilt sensor component
        final var distanceSensor = new UltrasonicDistanceSensorComponent(pi4j);

        // Configures the Sensor to find Object passing in a predefined distance
        System.out.println("Searching for an object now...");
        distanceSensor.setDetectionRange(5,50);
        distanceSensor.setMeasurementTemperature(23);
        distanceSensor.onObjectFound(() -> System.out.println("Sensor has found a Object in Range!"));
        distanceSensor.onObjectDisappeared(() -> System.out.println("Found Object disappeared!"));
        sleep(10000);

        // Clean up event handlers
        System.out.println("Searching completed.");
        distanceSensor.onObjectFound(null);
        distanceSensor.onObjectDisappeared(null);

        // Just printing some text to the users
        System.out.println("Let's find out the impact of temperature to ultrasonic measurements!");

        // Start a measurement with a temperature compensation like it is -10°C while measuring.
        double measurementCold = distanceSensor.measure(-10);
        System.out.println("If you room has -10°C now we measure: " + measurementCold + " cm");

        // Start a measurement with a temperature compensation like it is 30°C while measuring.
        double measurementHot = distanceSensor.measure(30);
        System.out.println("If you room has 30°C now we measure: " + measurementHot + " cm");
        System.out.format("That's a difference of %.2f %%. Only caused by the difference of sonics. Physic is " +
            "crazy\n", (measurementHot - measurementCold) / measurementCold * 100);

        System.out.println("Lets now just measure for 10 Seconds. That gives some time to try the sensor a little.");

        // Loop 10 times through the measurement. Print the result to the user
        for (int i = 0; i < 10; i++) {
            // Measures the current distance without temperature compensation and prints it to the user.
            try {
                System.out.println("Measured distance is: " + distanceSensor.measure() + " cm");
            } catch (MeasurementException e) {
                // If the measurement fails with a MeasurementException, we inform the user and try again next time
                System.out.println("Oh no. Measurement failed... lets try again");
            }

            // Delay the measurements a little. This gives you some time to move in front of the sensor.
            sleep(1000);
        }
    }
}
