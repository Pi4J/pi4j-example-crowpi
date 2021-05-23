package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.SoundSensorComponent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A example application to show how the sound sensor could be used. It registers a event handler to the sensors and
 * the program loops until you clapped 3 times.
 */
public class SoundSensorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        System.out.println("Welcome to the SoundSensor Test");

        // Initialize clap counting variable
        // There is a special kind of integer used because java lambda functions support only final variables
        AtomicInteger count = new AtomicInteger();

        // Initialize a SoundSensor component
        var soundSensor = new SoundSensorComponent(pi4j);

        // If it is already noisy we can not run the clap example. You need to setup the potentiometer first.
        if (soundSensor.isNoisy()) {
            System.out.println("It is already really noisy. Setup the potentiometer first");
            return;
        }

        // Ready to start the example so register the event handler which counts the number of claps.
        soundSensor.onNoise(() -> {
            var currentCounter = count.getAndIncrement();
            if (3 - currentCounter > 0) {
                System.out.println("You clapped! " + (3 - currentCounter) + " more times " +
                    "to finish");
            } else {
                System.out.println("Done");
            }
        });

        // Loop until clapped 3 times
        while (count.get() < 3) {
            sleep(10);
        }

        // Clean the event handler
        soundSensor.onNoise(null);
    }
}
