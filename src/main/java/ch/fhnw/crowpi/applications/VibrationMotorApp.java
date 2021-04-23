package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.VibrationMotorComponent;
import com.pi4j.context.Context;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * This example shows how to use the vibration motor. As this component is a simple digital output it is really easy
 * to use. An absolute beginner friendly component of the CrowPi
 */
public class VibrationMotorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        VibrationMotorComponent vibrationMotor = new VibrationMotorComponent(pi4j);

        // Short test if the component is working
        vibrationMotor.on();
        sleep(500);
        vibrationMotor.off();

        // Write to ask the use something
        System.out.println("Are you awake? Answer with YES if you are...");
        // Scanner is used to read the input from the commandline
        Scanner scanner = new Scanner(System.in);

        // Loops until YES is found
        while (!scanner.nextLine().equalsIgnoreCase("YES")) {
            // Do some pulses to create a strange noise
            for (int i = 10; i < 100; i++) {
                // As you see use pulse is a lot more comfortable than using .on -> sleep -> off
                vibrationMotor.pulse(i);
                sleep(i);
            }
            for (int i = 100; i > 10; i--) {
                vibrationMotor.pulse(i);
                sleep(i);
            }
            System.out.println("Are you awake? Answer with YES if you are...");
        }

        // Awake so we can turn of the vibration motor now.
        vibrationMotor.off();
    }
}
