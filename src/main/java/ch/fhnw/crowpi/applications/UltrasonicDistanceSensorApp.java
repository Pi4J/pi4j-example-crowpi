package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.TiltSensorComponent;
import ch.fhnw.crowpi.components.UltrasonicDistanceSensorComponent;
import com.pi4j.context.Context;

public class UltrasonicDistanceSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Create new tilt sensor component
        final var distanceSensor = new UltrasonicDistanceSensorComponent(pi4j);

        while (true) {
            System.out.println(distanceSensor.measure());
        }
    }
}
