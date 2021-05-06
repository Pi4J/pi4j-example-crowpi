package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.BuzzerComponent;
import com.pi4j.crowpi.helpers.Note;

/**
 * Plays the Super Mario Overworld theme song over the builtin CrowPi buzzer.
 * Notes taken from https://www.hackster.io/jrance/super-mario-theme-song-w-piezo-buzzer-and-arduino-1cc2e4
 */
public class BuzzerApp implements Application {
    /**
     * Array of notes which should be played by this example
     */
    private static final Note[] NOTES = new Note[]{
        Note.E7, Note.E7, Note.PAUSE, Note.E7,
        Note.PAUSE, Note.C7, Note.E7, Note.PAUSE,
        Note.G7, Note.PAUSE, Note.PAUSE, Note.PAUSE,
        Note.G6, Note.PAUSE, Note.PAUSE, Note.PAUSE,

        Note.C7, Note.PAUSE, Note.PAUSE, Note.G6,
        Note.PAUSE, Note.PAUSE, Note.E6, Note.PAUSE,
        Note.PAUSE, Note.A6, Note.PAUSE, Note.B6,
        Note.PAUSE, Note.AS6, Note.A6, Note.PAUSE,

        Note.G6, Note.E7, Note.G7,
        Note.A7, Note.PAUSE, Note.F7, Note.G7,
        Note.PAUSE, Note.E7, Note.PAUSE, Note.C7,
        Note.D7, Note.B6, Note.PAUSE, Note.PAUSE,

        Note.C7, Note.PAUSE, Note.PAUSE, Note.G6,
        Note.PAUSE, Note.PAUSE, Note.E6, Note.PAUSE,
        Note.PAUSE, Note.A6, Note.PAUSE, Note.B6,
        Note.PAUSE, Note.AS6, Note.A6, Note.PAUSE,

        Note.G6, Note.E7, Note.G7,
        Note.A7, Note.PAUSE, Note.F7, Note.G7,
        Note.PAUSE, Note.E7, Note.PAUSE, Note.C7,
        Note.D7, Note.B6, Note.PAUSE, Note.PAUSE,
    };

    /**
     * Array of note types, e.g. 4 represents a quarter-note with 0.25s duration.
     * Must have the same length as the NOTES array.
     */
    private static final int[] TEMPO = new int[]{
        12, 12, 12, 12,
        12, 12, 12, 12,
        12, 12, 12, 12,
        12, 12, 12, 12,

        12, 12, 12, 12,
        12, 12, 12, 12,
        12, 12, 12, 12,
        12, 12, 12, 12,

        9, 9, 9,
        12, 12, 12, 12,
        12, 12, 12, 12,
        12, 12, 12, 12,

        12, 12, 12, 12,
        12, 12, 12, 12,
        12, 12, 12, 12,
        12, 12, 12, 12,

        9, 9, 9,
        12, 12, 12, 12,
        12, 12, 12, 12,
        12, 12, 12, 12,
    };

    @Override
    public void execute(Context pi4j) {
        // Initialize buzzer component with default pin
        final var buzzer = new BuzzerComponent(pi4j);

        // Loop through all notes and play them one-by-one
        for (int i = 0; i < NOTES.length; i++) {
            // Calculate duration of note by dividing one second with tempo
            // Tempo represents the actual note type, e.g. tempo=4 -> quarter note -> 0.25s
            final var duration = 1000 / TEMPO[i];

            // Print note to standard output, play over buzzer
            System.out.print(NOTES[i] + "(" + duration + ") ");
            buzzer.playTone(NOTES[i].getFrequency(), duration);

            // Wait shortly between notes to make them more distinguishable
            buzzer.playSilence((int) (duration * 1.3));
        }
    }
}
