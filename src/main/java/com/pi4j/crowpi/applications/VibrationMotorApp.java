package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.VibrationMotorComponent;

import java.util.Scanner;

/**
 * This example shows how to use the vibration motor.
 * It basically represents a vibrating alarm clock which is going off and keeps making
 * noises until the user confirms being awake by writing `yes` to the console / standard input.
 * <p>
 * Please note that this example requires the
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
        sleep(1000);

       // Scanner is used to read the input from the console
        Scanner scanner = new Scanner(System.in);

        // Loops until YES is entered by user
        do {
            // Do some pulses to create a strange noise
            for (int i = 100; i < 500; i=i+100) {
                // As you see use pulse is a lot more comfortable than using .on -> sleep -> off
                vibrationMotor.pulse(i);
                sleep(i);
            }
            for (int i = 500; i > 100; i=i-100) {
                vibrationMotor.pulse(i);
                sleep(i);
            }
            System.out.println("Are you awake? Answer with YES if you are...");
        } while (!scanner.nextLine().equalsIgnoreCase("YES"));

        // User is awake, so we can turn off the vibration motor now.
        vibrationMotor.off();
    }
}
