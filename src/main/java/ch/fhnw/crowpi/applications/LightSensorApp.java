package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.LightSensorComponent;
import com.pi4j.context.Context;

public class LightSensorApp implements Application {

    @Override
    public void execute(Context pi4j) {
        final var lightsensor = new LightSensorComponent(pi4j) ;
    }
}
