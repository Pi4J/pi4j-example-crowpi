package ch.fhnw.crowpi.applications;

import com.pi4j.context.Context;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.BuzzerComponent;
import ch.fhnw.crowpi.helpers.Note;

/**
 * Plays the Super Mario Overworld theme song over the builtin CrowPi buzzer.
 * Notes taken from https://www.hackster.io/jrance/super-mario-theme-song-w-piezo-buzzer-and-arduino-1cc2e4
 */
public class BuzzerApp implements Application {
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

    private static final int[] BEATS = new int[]{
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
        final var buzzer = new BuzzerComponent(pi4j);

        for (int i = 0; i < NOTES.length; i++) {
            // Calculate duration of note
            final var duration = 1000 / BEATS[i];

            // Print note to standard output, play over buzzer
            System.out.print(NOTES[i] + "(" + duration + ") ");
            buzzer.playTone(NOTES[i].getFrequency(), duration);

            // Wait shortly between notes to make them more distinguishable
            buzzer.playSilence((int) (duration * 1.3));
        }
    }
}
