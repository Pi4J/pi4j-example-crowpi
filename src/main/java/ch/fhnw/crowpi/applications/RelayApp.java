package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.RelayComponent;
import com.pi4j.context.Context;

/**
 * This Example shows how to use the RelayComponent.
 */
public class RelayApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Create a new RelayComponent with default Pin
        RelayComponent relay = new RelayComponent(pi4j);

        // Turn off the relay to have a defined state
        relay.on();
        System.out.println("on");
        sleep(2000);

        // Make a clock alike sound by toggle the relais every second once
        for (int i = 0; i < 10; i++) {
            System.out.println(relay.toggle());

            sleep(1000);
        }

        // That's all so turn off the relay and quit
        relay.off();
        System.out.println("off");
        sleep(2000);
    }
}
