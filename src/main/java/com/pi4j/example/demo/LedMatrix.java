package com.pi4j.example.demo;

import com.pi4j.context.Context;
import com.pi4j.example.util.PrintInfo;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.util.Console;

import java.util.Arrays;

public class LedMatrix {

    public static void runDemo(Context pi4j, boolean printInfo, Console console) {
        try {
            console.println();
            console.box("TODO LED matrix demo");
            console.println();

            if (printInfo) {
                PrintInfo.printRegistry(console, pi4j);
            }
        } catch (Exception ex) {
            console.println("Error during LED toggling: " + ex.getMessage());
        }
    }
}
