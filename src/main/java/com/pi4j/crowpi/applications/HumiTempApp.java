package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.HumiTempComponent;

public class HumiTempApp implements Application {
    @Override
    public void execute(Context pi4j) {
        System.out.println("Fire Humi Temp go go go hahahah hacky hacky ");

        var humiTempSensor = new HumiTempComponent();

        while (true) {
            ;
        }
    }
}
