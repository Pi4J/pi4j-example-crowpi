package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.UltrasonicDistanceSensorComponent;
import ch.fhnw.crowpi.components.exceptions.MeasurementException;
import com.pi4j.context.Context;

/**
 * Example Application of using the Crow Pi Ultrasonic Distance Sensor.
 */
public class UltrasonicDistanceSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Create new tilt sensor component
        final var distanceSensor = new UltrasonicDistanceSensorComponent(pi4j);

        // Just printing some text to the users
        System.out.println("Ultrasonic Distance Measurement starting ...");
        System.out.println("Let's find out the impact of temperature to ultrasonic measurements!");

        // Start a measurement with a temperature compensation like it is -10°C while measuring.
        double measurementCold = distanceSensor.measure(-10);
        System.out.println("If you room has -10°C now we measure: " + measurementCold + " cm");

        // Start a measurement with a temperature compensation like it is 30°C while measuring.
        double measurementHot = distanceSensor.measure(30);
        System.out.println("If you room has 30°C now we measure: " + measurementHot + " cm");
        System.out.format("That's a difference of %.2f %%. Only caused by the difference of sonics. Physic is " +
            "crazy", (measurementHot - measurementCold) / measurementCold * 100);

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
