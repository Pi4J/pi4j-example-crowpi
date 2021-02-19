package com.pi4j.example;

/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Pi4J
 * PROJECT       :  Pi4J :: EXAMPLE  :: Sample Code
 * FILENAME      :  CrowPiApp.java
 *
 * This file is part of the Pi4J project. More information about
 * this project can be found here:  https://pi4j.com/
 * **********************************************************************
 * %%
 * Copyright (C) 2012 - 2020 Pi4J
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.pi4j.Pi4J;
import com.pi4j.example.demo.LedMatrix;
import com.pi4j.example.demo.StateLed;
import com.pi4j.example.util.PrintInfo;
import com.pi4j.util.Console;

/**
 * <p>This example application can run different demos on the CrowPi</p>
 *
 * @author Frank Delporte (<a href="https://www.webtechie.be">https://www.webtechie.be</a>)
 * @version $Id: $Id
 */
public class CrowPiApp {

    @Parameter(names={"--help", "-h"}, help = true)
    private boolean help;
    @Parameter(names={"--all", "-a"}, description="Run all demos")
    private boolean all = false;
    @Parameter(names={"--stateled", "-sl"}, description="Toggle all the state LEDs")
    private boolean stateLeds = false;
    @Parameter(names={"--ledMatrix", "-lm"}, description="Run demo of the 8x8 LED matrix")
    private boolean ledMatrix = false;
    @Parameter(names={"--printinfo", "-p"}, description="Always print the available info")
    private boolean printInfo = false;

    /**
     * This application blinks a led and counts the number the button is pressed. The blink speed increases with each
     * button press, and after 5 presses the application finishes.
     *
     * @param args an array of {@link java.lang.String} objects.
     * @throws java.lang.Exception if any.
     */
    public static void main(String... args) throws Exception {
        CrowPiApp main = new CrowPiApp();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);
        main.run();
    }

    public void run() throws Exception {
        // Create Pi4J console wrapper/helper
        // (This is a utility class to abstract some of the boilerplate stdin/stdout code)
        final var console = new Console();

        // Print program title/header
        console.title("<-- The Pi4J Project -->", "CrowPi Example project");

        // Print selected options
        console.box("Selected options from startup arguments");
        console.println();
        console.println("Run all demos: " + all);
        console.println("State LEDs: " + stateLeds);
        console.println("8x8 LED matrix: " + ledMatrix);
        console.println("Print info: " + printInfo);
        console.println();

        // Initialize the Pi4J Runtime Context
        var pi4j = Pi4J.newAutoContext();

        if (printInfo) {
            PrintInfo.printLoadedPlatforms(console, pi4j);
            PrintInfo.printDefaultPlatform(console, pi4j);
            PrintInfo.printProviders(console, pi4j);
        }

        if (all || stateLeds) {
            StateLed.toggleAll(pi4j, printInfo, console);
        }
        if (all || ledMatrix) {
            LedMatrix.runDemo(pi4j, printInfo, console);
        }

        // Shutdown Pi4J
        pi4j.shutdown();
    }
}
