package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.SevenSegmentComponent;
import ch.fhnw.crowpi.components.SevenSegmentComponent.Segment;
import com.pi4j.context.Context;

import java.time.LocalTime;

/**
 * Shows some basic digits and a fake loading indicator before acting as a clock which endlessly displays the current time on the CrowPi.
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
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }

        // Show fake loading indicator by moving a single dash from left to right several times
        // We do so by initializing an array with all possible states (forward & reverse) and printing it in order
        final var states = new String[]{"-   ", " -  ", "  - ", "   -", "  - ", " -  ", "-   "};
        for (int i = 0; i < 5; i++) {
            for (String state : states) {
                // Print current state and sleep for 50 milliseconds
                segment.print(state);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // Loop endlessly and print the current time every second
        while (true) {
            // Print current local time
            segment.print(LocalTime.now());

            // Sleep for one second before continuing
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
