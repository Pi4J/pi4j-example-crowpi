package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.VibrationMotorComponent;

import java.util.Scanner;

/**
 * This example shows how to use the vibration motor. It basically represents a vibrating alarm clock which is going off and keeps making
 * noises until the user confirms being awake by writing `yes` to the console / standard input. Please note that this example requires the
 * DIP switch 2-1 to be activated for it to work.
 */
public class VibrationMotorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        VibrationMotorComponent vibrationMotor = new VibrationMotorComponent(pi4j);

        // Short test if the component is working
        vibrationMotor.on();
        sleep(500);
        vibrationMotor.off();

        // Write to ask the user something
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

        // User is awake, so we can turn off the vibration motor now.
        vibrationMotor.off();
    }
}
