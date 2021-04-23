package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.VibrationMotorComponent;
import com.pi4j.context.Context;

import java.util.concurrent.TimeUnit;

public class VibrationMotorApp implements Application {
    @Override
    public void execute(Context pi4j) {
        VibrationMotorComponent vibrationMotor = new VibrationMotorComponent(pi4j);

        vibrationMotor.on();

        for (int j = 0; j < 2; j++) {

            for (int i = 1; i < 75; i = i + 2) {
                vibrationMotor.pulse(i);
                sleep(i);
            }

            for (int i = 75; i > 1; i = i - 2) {
                vibrationMotor.pulse(i);
                sleep(i);
            }
        }
    }
}
