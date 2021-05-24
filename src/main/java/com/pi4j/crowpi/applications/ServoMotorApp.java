package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.ServoMotorComponent;

public class ServoMotorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        final var servoMotor = new ServoMotorComponent(pi4j);

        while (true) {
            servoMotor.setPercent(0); // left
            sleep(500);
            servoMotor.setPercent(50); // center
            sleep(500);
            servoMotor.setAngle(90); // right
            sleep(500);
            servoMotor.setRange(0, -100, 100); // center
            sleep(500);
        }
    }
}
