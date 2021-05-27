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
        final var count = new AtomicInteger();

        // Initialize a SoundSensor component
        final var soundSensor = new SoundSensorComponent(pi4j);

        // If it is already noisy we can not run the clap example. You need to setup the potentiometer first.
        if (soundSensor.isNoisy()) {
            System.out.println("It is already really noisy. Setup the potentiometer first");
            return;
        }

        // Define how many times to clap
        final int CLAP_THRESHOLD = 3;

        // Ready to start the example so register the event handler which counts the number of claps.
        soundSensor.onNoise(() -> {
            final int currentCounter = count.incrementAndGet();
            if (currentCounter < CLAP_THRESHOLD) {
                System.out.println("You clapped! " + (CLAP_THRESHOLD - currentCounter) + " more times to finish");
            }
        });

        // Loop until clapped 3 times
        while (count.get() < CLAP_THRESHOLD) {
            sleep(10);
        }

        // Three times so application completed
        System.out.println("Done");

        // Clean the event handler
        soundSensor.onNoise(null);
    }
}
