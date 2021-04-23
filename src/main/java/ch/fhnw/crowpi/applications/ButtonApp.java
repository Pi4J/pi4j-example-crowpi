package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.ButtonComponent;
import ch.fhnw.crowpi.components.definitions.Button;
import com.pi4j.context.Context;

/**
 * This example app initializes all four directional buttons and registers event handlers for every button. While this example itself does
 * not do much, it showcases how it could be used for controlling a player character in a game.
 */
public class ButtonApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize all four button components
        final var upButton = new ButtonComponent(pi4j, Button.UP);
        final var downButton = new ButtonComponent(pi4j, Button.DOWN);
        final var leftButton = new ButtonComponent(pi4j, Button.LEFT);
        final var rightButton = new ButtonComponent(pi4j, Button.RIGHT);

        // Register event handlers to print a message for each button when pressed (onDown) and depressed (onUp)
        upButton.onDown(() -> System.out.println("Alright, moving upwards!"));
        downButton.onDown(() -> System.out.println("Aye aye, moving downwards!"));
        leftButton.onDown(() -> System.out.println("Gotcha, moving to the left!"));
        rightButton.onDown(() -> System.out.println("Yep, moving to the right!"));

        upButton.onUp(() -> System.out.println("Stopped moving upwards."));
        downButton.onUp(() -> System.out.println("Stopped moving downwards."));
        leftButton.onUp(() -> System.out.println("Stopped moving to the left."));
        rightButton.onUp(() -> System.out.println("Stopped moving to the right."));

        // Wait for 15 seconds while handling events before exiting
        sleep(15000);
    }
}
