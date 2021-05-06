package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.LcdDisplayComponent;

/**
 * Example Application of using the Crow Pi LCD Display
 */
public class LcdDisplayApp implements Application {
    @Override
    public void execute(Context pi4j) {
        LcdDisplayComponent lcd = new LcdDisplayComponent(pi4j);
        System.out.println("Here we go.. let's have some fun with that LCD Display!");

        // First we need to setup the LCD Display. That for just call initialize
        lcd.initialize();

        // Write text to the lines separate
        lcd.writeLine("Hello", 1);
        lcd.writeLine("   World!", 2);

        // Wait a little to have some time to read it
        sleep(3000);

        // Clear the display to start next parts
        lcd.clearDisplay();

        // Let's try to draw a house. To keep this method short and clean we create the characters in a separate
        // method below.
        createCharacters(lcd);

        // Now all characters are ready. Just draw them on the right place by moving the cursor and writing the
        // created characters to specific positions
        lcd.writeCharacter('\1', 1, 1);
        lcd.writeCharacter('\2', 2, 1);
        lcd.writeCharacter('\3', 1, 2);
        lcd.writeCharacter('\4', 2, 2);

        // Delay a few seconds to let you enjoy our little house
        sleep(3000);

        // Uhm but the view from our house would be better from the other side. lets move it over there
        for (int i = 0; i < 10; i++) {
            lcd.moveDisplayRight();
            sleep(100);
        }

        // Enjoy the new view from our house
        sleep(5000);

        // To clean up or start with something new just use the clearDisplay method
        lcd.clearDisplay();

        // To write some text there are different methods. The simplest one is this one which automatically inserts
        // linebreaks if needed.
        lcd.writeText("Boohoo that's so simple to use!");

        // Delay again
        sleep(3000);

        // Of course it is also possible to write a single line
        lcd.writeLine("hard to usE!", 2);
        sleep(3000);

        // The display always works with a current position. That's called the cursor. You can simply modify the
        // cursor with a few method calls
        lcd.setCursorVisibility(true);
        lcd.setCursorBlinking(true);

        // Now you can see the cursor is right behind the last character we wrote before.
        sleep(5000);

        // The last text had a mistake so let's move there and fix it.
        lcd.moveCursorLeft();
        lcd.moveCursorLeft();
        lcd.moveCursorLeft();
        sleep(1000);
        // Oops moved too far, lets move one to the right
        lcd.moveCursorRight();
        // And fix it
        lcd.writeCharacter('e');

        // Now it looks fine
        sleep(3000);

        // Cursor annoying? Simply turn it off and clear the display again
        lcd.setCursorBlinking(false);
        lcd.setCursorVisibility(false);
        lcd.clearDisplay();

        // Some more text writings
        lcd.writeText("Want more fun?");
        sleep(3000);
        // Its also possible to just clear a single line
        lcd.clearLine(2);
        // Write on the previously cleared line
        lcd.writeLine("No, thanks.", 2);
        sleep(3000);
        // A \n makes a manual line break
        lcd.writeText("Okay ...\nBye Bye! :-(");
        sleep(5000);

        // Turn off the backlight makes the display appear turned off
        lcd.setDisplayBacklight(false);
    }

    public void createCharacters(LcdDisplayComponent lcd) {
        // Create upper left part of the house
        lcd.createCharacter(1, new byte[]{
            0b00000,
            0b00000,
            0b00000,
            0b00001,
            0b00011,
            0b00111,
            0b01111,
            0b11111
        });

        // Create upper right part of the house
        lcd.createCharacter(2, new byte[]{
            0b00000,
            0b00000,
            0b00010,
            0b10010,
            0b11010,
            0b11110,
            0b11110,
            0b11111
        });

        // Create lower left part of the house
        lcd.createCharacter(3, new byte[]{
            0b11111,
            0b11111,
            0b11111,
            0b11111,
            0b10001,
            0b10001,
            0b10001,
            0b10001
        });

        // Create lower right part of the house
        lcd.createCharacter(4, new byte[]{
            0b11111,
            0b11111,
            0b11111,
            0b10001,
            0b10001,
            0b10001,
            0b11111,
            0b11111
        });
    }
}
