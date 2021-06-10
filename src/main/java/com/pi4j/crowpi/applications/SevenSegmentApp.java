package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.SevenSegmentComponent;
import com.pi4j.crowpi.components.SevenSegmentComponent.Segment;

import java.time.LocalTime;

/**
 * Shows some basic digits and a fake loading indicator before acting as a clock which displays the time on the CrowPi for 15 seconds.
 * The colon on the seven-segment display will blink for every odd second.
 */
public class SevenSegmentApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize and enable seven-segment display component
        final var segment = new SevenSegmentComponent(pi4j);
        segment.setEnabled(true);

        // Activate full brightness and disable blinking
        // These are the defaults and just here for demonstration purposes
        segment.setBlinkRate(0);
        segment.setBrightness(15);

        // Manually set some digits and symbols to demonstrate the advanced API
        segment.setDigit(0, '1'); // Place the character "1" into the first digit
        segment.setDecimalPoint(0, true); // Activate the decimal point after the first digit
        segment.setDigit(1, 'a'); // Place the character "a" into the second digit
        segment.setDigit(2, 4); // Place the number 4 into the third digit
        segment.setDigit(3, Segment.TOP, Segment.BOTTOM); // Activate top and bottom segment for fourth digit
        segment.refresh(); // Send updated buffer to display

        // Sleep for three seconds before moving on...
        sleep(3000);

        // Show fake loading indicator by moving a single dash from left to right several times
        // We do so by initializing an array with all possible states (forward & reverse) and printing it in order
        final var states = new String[]{"-   ", " -  ", "  - ", "   -", "  - ", " -  ", "-   "};
        for (int i = 0; i < 5; i++) {
            for (String state : states) {
                // Print current state and sleep for 50 milliseconds
                segment.print(state);
                sleep(50);
            }
        }

        // Loop for 15 seconds and print the current time every second
        for (int i = 0; i < 15; i++) {
            // Print current local time
            segment.print(LocalTime.now());

            // Sleep for one second before continuing
            sleep(1000);
        }

        // Disable the seven-segment display
        segment.setEnabled(false);
    }
}
