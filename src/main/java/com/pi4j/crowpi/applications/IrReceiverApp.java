package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.IrReceiverComponent;

/**
 * This example demonstrates the infrared receiver component on the CrowPi. Please note that the receiver LED must first be plugged into the
 * three small pinholes labelled as "IR" for this example to work. As Java does not allow for precise enough timings itself, this component
 * does not use Pi4J to retrieve the pulses of the GPIO pin for the IR sensor and instead relies on mode2, an executable provided as part of
 * LIRC for reading from an IR input.
 * <p>
 * A clean alternative would be using a separate microcontroller which handles the super precise timing-based communication itself and
 * interacts with the Raspberry Pi using I²C, SPI or any other bus. This would offload the work and guarantee even more accurate results. As
 * the CrowPi does not have such a dedicated microcontroller though, using `mode2` was the best available approach.
 */
public class IrReceiverApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize the IR receiver component
        // We do not use Pi4J here at all, so there is no need to pass the context...
        final var ir = new IrReceiverComponent();

        // Register an event listener for key presses
        System.out.println("Welcome to the IR demo! An event listener for key presses will now be registered...");
        ir.onKeyPressed(key -> {
            // Print the key which just has been pressed
            System.out.println("Key on IR remote has been pressed: " + key);

            // It is also possible to check if a specific key was pressed
            if (key == IrReceiverComponent.Key.CH) {
                System.out.println("You pressed the super special CH key! (it is actually not special, sorry to disappoint)");
            }
        });

        // Give the user some time to press buttons
        System.out.println("Done! You now have 30 seconds to try out pressing various keys on the IR remote...");
        sleep(30000);

        // Cleanup the event listener
        ir.onKeyPressed(null);
    }
}
