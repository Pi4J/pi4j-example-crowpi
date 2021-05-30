package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.HumiTempComponent;
import com.pi4j.crowpi.components.SoundSensorComponent;

public class HumiTempApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize a HumiTempComponent with default values
        final var dht11 = new HumiTempComponent();

        System.out.println("Welcome to the HumiTempApp");
        System.out.println("Measurement starts now.. ");

        // Start some measurements in a loop
        for (int i = 0; i < 5; i++) {
            System.out.println("It is currently " + dht11.getTemperature() + "°C and the Humidity is " + dht11.getHumidity() + "%.");
            sleep(2000);
        }
    }
}
