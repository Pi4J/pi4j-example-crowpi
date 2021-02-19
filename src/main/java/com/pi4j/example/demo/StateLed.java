package com.pi4j.example.demo;

import com.pi4j.context.Context;
import com.pi4j.example.util.PrintInfo;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.util.Console;

import java.util.Arrays;

public class StateLed {

    private static Integer[] bcmList = new Integer[]{ 2, 3, 4, 17};

    public static void toggleAll(Context pi4j, boolean printInfo, Console console) {
        for (int bcm : Arrays.asList(bcmList)) {
            try {
                var ledConfig = DigitalOutput.newConfigBuilder(pi4j)
                        .id("led")
                        .name("LED Flasher")
                        .address(bcm)
                        .shutdown(DigitalState.LOW)
                        .initial(DigitalState.LOW)
                        .provider("pigpio-digital-output");
                var led = pi4j.create(ledConfig);
                led.high();
                Thread.sleep(500);
                led.low();
                Thread.sleep(500);
                if (printInfo) {
                    PrintInfo.printRegistry(console, pi4j);
                }
            } catch (Exception ex) {
                console.println("Error during LED toggling: " + ex.getMessage());
            }
        }
    }
}
