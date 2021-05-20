package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.SoundSensorComponent;

public class SoundSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        System.out.println("SoundSensor App Started");

        var soundSensor = new SoundSensorComponent(pi4j);
        soundSensor.onNoise(() -> System.out.println("Shut up!"));

        sleep(10000);
    }
}
