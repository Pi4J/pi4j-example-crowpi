package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.StepMotorComponent;

public class StepMotorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        final var stepMotor = new StepMotorComponent(pi4j);

        stepMotor.turnDegrees(360);
        stepMotor.turnDegrees(-360);
    }
}
