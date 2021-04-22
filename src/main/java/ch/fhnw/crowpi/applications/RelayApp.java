package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import com.pi4j.context.Context;

/**
 * This Example shows how to use the RelayComponent.
 */
public class RelayApp implements Application {
    @Override
    public void execute(Context pi4j) {
        System.out.println("CrowPi with Pi4J rocks!");
    }
}
