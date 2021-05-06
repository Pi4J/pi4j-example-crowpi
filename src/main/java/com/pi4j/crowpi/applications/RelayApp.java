package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.RelayComponent;

/**
 * This example shows how to use the relay component by creating a clock-like sound by repeatedly toggling the relay on and off.
 */
public class RelayApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Create a new RelayComponent with default Pin
        RelayComponent relay = new RelayComponent(pi4j);

        // Turn on the relay to have a defined state
        relay.setStateOn();
        sleep(1000);

        // Make a clock alike sound by toggle the relay every second once
        for (int i = 0; i < 10; i++) {
            System.out.println(relay.toggleState());
            sleep(1000);
        }

        // That's all so turn off the relay and quit
        relay.setStateOff();
        System.out.println("off");
        sleep(2000);
    }
}
