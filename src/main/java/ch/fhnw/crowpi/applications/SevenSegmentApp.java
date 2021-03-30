package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.SevenSegmentComponent;
import com.pi4j.context.Context;

import java.time.LocalTime;

/**
 * Shows a fake loading indicator before acting as a clock which endlessly displays the current time on the CrowPi.
 * The colon on the seven-segment display will blink for every odd second.
 */
public class SevenSegmentApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize and enable seven-segment display component
        final var segment = new SevenSegmentComponent(pi4j);
        segment.setEnabled(true);

        // Activate full brightness and disable blinking
        // These are the defaults and just here for demonstration
        segment.setBlinkRate(0);
        segment.setBrightness(15);

        // Show fake loading indicator by moving a single dash from left to right
        // We do so by initializing an array with all possible states (forward & reverse) and printing it in order
        final var states = new String[]{"-   ", " -  ", "  - ", "   -", "  - ", " -  ", "-   "};
        for (int i = 0; i < states.length * 5; i++) {
            // Print state based on current index in loop
            // As we loop more often than the amount of states, we use modulo to stay within boundaries
            // E.g. "x % 5" will ensure that "x" can only be "0", "1", "2", "3" or "4"
            segment.print(states[i % states.length]);

            // Sleep for 50ms to make it look prettier
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
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
