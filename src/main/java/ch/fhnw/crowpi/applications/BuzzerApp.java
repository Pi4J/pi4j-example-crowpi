package ch.fhnw.crowpi.applications;

import com.pi4j.context.Context;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.BuzzerComponent;
import ch.fhnw.crowpi.helpers.Note;

/**
 * Plays the Super Mario overworld theme song over the builtin CrowPi buzzer.
 * Notes taken from https://www.hackster.io/jrance/super-mario-theme-song-w-piezo-buzzer-and-arduino-1cc2e4
 */
public class BuzzerApp implements Application {
  private static final Note[] SUPER_MARIO_NOTES = new Note[] {
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

  private static final int[] SUPER_MARIO_BEATS = new int[] {
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
    final var notes = SUPER_MARIO_NOTES;
    final var beats = SUPER_MARIO_BEATS;

    for (int i = 0; i < notes.length; i++) {
      // Calculate duration and play tone over buzzer
      final var duration = 1000 / beats[i];
      System.out.print(notes[i] + "(" + duration + ") ");
      buzzer.playTone(notes[i].getFrequency(), duration);

      // Wait shortly between notes to make them more distinguishable
      try {
        Thread.sleep((long) (duration * 1.3));
      } catch (InterruptedException e) {
      }
    }
  }
}
