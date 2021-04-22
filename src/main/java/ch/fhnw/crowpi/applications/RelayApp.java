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
        System.out.println("CrowPi with Pi4J rocks!");

        RelayComponent relay = new RelayComponent(pi4j);

        for (int i = 0; i < 10; i++) {
            relay.toggle();
            sleep(1000);
        }
    }
}
