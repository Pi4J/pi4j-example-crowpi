package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.ServoMotorComponent;

/**
 * This example shows how the servo motor on the CrowPi can be used. It requires the right DIP switch to have 7 and 8 turned on and mainly
 * demonstrates the various ways of setting the servo position, namely either by percentage, angle in degrees or a custom value range.
 */
public class ServoMotorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize servo motor component
        final var servoMotor = new ServoMotorComponent(pi4j);

        // Demonstrate the percentage mapping on the servo
        System.out.println("In 2 seconds, the servo motor will move to the left-most position which is 0%");
        sleep(2000);
        servoMotor.setPercent(0);

        System.out.println("In another 2 seconds, the servo motor will show 100% by moving to the right-most position");
        sleep(2000);
        servoMotor.setPercent(100);

        System.out.println("Last but not least, in 2 more seconds the servo will be centered to display 50%");
        sleep(2000);
        servoMotor.setPercent(50);

        // Sweep once from left to right using the setAngle function
        System.out.println("We will sweep once to the left in 2 seconds...");
        sleep(2000);
        servoMotor.setAngle(-90);

        System.out.println("... and now to the right in 2 more seconds!");
        sleep(2000);
        servoMotor.setAngle(90);

        // Use a custom range for displaying the data
        System.out.println("Imagine a pointer on the servo positioned above a label between -20ºC and +40ºC");
        System.out.println("By using the setRange() method, we can automatically map our temperature range to the servo range!");
        System.out.println("As an example, in five seconds the servo will show -10º which should be on the far left of the servo.");
        sleep(2000);

        servoMotor.setRange(-20, +40); // This will define our range as values between -20 and +40
        servoMotor.moveOnRange(-10); // This will map -10 based on the previously defined range

        // And this demo is over, sleep for a second to give the servo some time to position itself
        sleep(1000);
    }
}
