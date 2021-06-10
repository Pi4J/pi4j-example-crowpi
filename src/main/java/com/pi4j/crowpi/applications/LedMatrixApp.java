package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.LedMatrixComponent;
import com.pi4j.crowpi.components.LedMatrixComponent.Symbol;
import com.pi4j.crowpi.components.definitions.Direction;

/**
 * Demonstrates the countless abilities of the 8x8 LED matrix by showing multiple demo examples one after another.
 */
public class LedMatrixApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize and enable LED matrix with medium brightness
        final var matrix = new LedMatrixComponent(pi4j);
        matrix.setEnabled(true);
        matrix.setBrightness(7);

        // Draw a cross with a circle over it using a Graphics2D lambda function
        // Further commands for Graphics2D can be found in the official Java documentation
        // This can be adjusted to draw your own symbols and images on the LED matrix
        matrix.draw(graphics -> {
            graphics.drawLine(0, 0, 7, 7);
            graphics.drawLine(7, 0, 0, 7);
            graphics.drawOval(1, 1, 5, 5);
        });

        // Sleep for a second before moving to the next example...
        sleep(1000);

        // Display list of smiley symbols with a short delay between each one
        final var symbols = new Symbol[]{
            Symbol.SMILEY_HAPPY,
            Symbol.SMILEY_SAD,
            Symbol.SMILEY_NEUTRAL,
            Symbol.SMILEY_SHOCKED
        };
        for (Symbol symbol : symbols) {
            matrix.print(symbol);
            sleep(1000);
        }

        // Transition to all four arrows with each sliding in from the direction its pointing towards
        matrix.transition(Symbol.ARROW_UP, Direction.UP);
        matrix.transition(Symbol.ARROW_DOWN, Direction.DOWN);
        matrix.transition(Symbol.ARROW_LEFT, Direction.LEFT);
        matrix.transition(Symbol.ARROW_RIGHT, Direction.RIGHT);

        // Print a long text which gets automatically scrolled across the LED matrix
        // Any text written between curly braces gets automatically looked up in the Symbol table
        // In this example, "{HEART}" will be replaced with the actual "Symbol.HEART" value
        // If such a pattern exists but no symbol is found with that name, it gets ignored and printed as-is.
        matrix.print("CrowPi + Pi4J = {HEART}");

        // Disable the LED matrix before exiting
        matrix.setEnabled(false);
    }
}
