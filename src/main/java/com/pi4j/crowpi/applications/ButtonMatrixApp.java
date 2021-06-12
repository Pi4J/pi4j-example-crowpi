package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.ButtonMatrixComponent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This example is actually also known as "memory game" or "Ich packe meinen Koffer mit ..." in German. During startup, it will first ask
 * for at least two players which will play against each other. Once all players have been entered, the actual game starts. During each turn
 * the player has to repeat all the previously pressed buttons in the same order and then pick a new button. The active player loses if an
 * incorrect button is pressed while having to repeat the previous ones. Otherwise, the game will move to the next player. The last player
 * remaining automatically wins the game.
 */
public class ButtonMatrixApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize button matrix component
        final var buttonMatrix = new ButtonMatrixComponent(pi4j);

        // Initialize game state
        final List<String> players = determinePlayers();
        final List<Integer> history = new ArrayList<>();

        // Repeat the game loop until there is only a single active player left
        // This means the game will go on until everyone but one has lost
        var playerIterator = players.iterator();
        while (players.size() >= 2) {
            // Determine active player by fetching the next active player from the list
            // An iterator basically loops over all entries and returns one after another each time .next() is called
            // When we reach the end of the iterator (hasNext() returns false), we create a new iterator to start from the beginning
            if (!playerIterator.hasNext()) {
                playerIterator = players.iterator();
            }
            final var activePlayer = playerIterator.next();

            // Print a message which informs about the currently active player whose turn it is
            System.out.println();
            System.out.println(">>> NEXT TURN: Good luck, " + activePlayer);

            // Print a message about the total number of moves which have to be repeated
            if (history.size() > 0) {
                System.out.println("Please repeat all previous " + history.size() + " button presses in order...");
            }

            // Make the player repeat the whole history and abort if incorrect
            boolean hasFailed = false;
            for (int i = 0; i < history.size(); i++) {
                // Wait for the player to press a button
                final int number = buttonMatrix.readBlocking();

                // Compare the button with the history at the currently checked position "i"
                // If incorrect, break out of this loop with the failed flag set to true
                if (number != history.get(i)) {
                    hasFailed = true;
                    break;
                }

                // Inform the user about the remaining moves if there are any
                final int movesLeft = history.size() - i - 1;
                if (movesLeft > 0) {
                    System.out.println("Correct! " + (history.size() - i - 1) + " more numbers to go...");
                } else {
                    System.out.println("Well done, you've repeated all buttons correctly!");
                }
            }

            // If the player has failed, print a message, remove the player and continue with the next turn
            if (hasFailed) {
                System.out.println("Incorrect! You've lost and are out, " + activePlayer + "!");
                playerIterator.remove();
                continue;
            }

            // Let the player choose a new button which shall be added to the history
            System.out.println("Press any button of your choice to add it to the list...");
            final int number = buttonMatrix.readBlocking();

            // Add the chosen number to the history and print it on the CLI
            history.add(number);
            System.out.println("Turn completed, button " + number + " has been added.");
        }

        // Print the winner, which will be the last and single element in the list
        System.out.println("Congratulations, " + players.get(0) + ", you have won!");
        System.out.println("Your score: " + history.size() + " points");

        // Stop the button matrix poller now that the application has ended
        buttonMatrix.stopPoller();
    }

    private List<String> determinePlayers() {
        // Initialize empty list of players
        final var players = new ArrayList<String>();

        // Initialize buffered reader for reading from command line
        final var reader = new BufferedReader(new InputStreamReader(System.in));

        // Gather player names from command line
        while (true) {
            // Print prompt to ask for the player names
            if (players.size() >= 2) {
                System.out.print("Please enter another player name or nothing to start the game: ");
            } else {
                System.out.print("At least 2 players are needed. Please enter a player name: ");
            }

            // Read the next player name from command line
            final String player;
            try {
                player = reader.readLine();
            } catch (IOException e) {
                continue;
            }

            // Determine if we are ready to start the game or need more players
            if (players.size() >= 2 && player.isEmpty()) {
                break;
            } else if (!player.isEmpty()) {
                players.add(player);
            }
        }

        return players;
    }
}
