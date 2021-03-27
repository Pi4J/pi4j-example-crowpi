package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.LightSensorComponent;
import com.pi4j.context.Context;

import static java.lang.Thread.sleep;

public class LightSensorApp implements Application {

    @Override
    public void execute(Context pi4j) {
        final var sensor = new LightSensorComponent(pi4j) ;


    }
}
