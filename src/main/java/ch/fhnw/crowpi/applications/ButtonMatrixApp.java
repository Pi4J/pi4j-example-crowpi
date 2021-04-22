package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.ButtonMatrixComponent;
import com.pi4j.context.Context;

public class ButtonMatrixApp implements Application {
    @Override
    public void execute(Context pi4j) {
        final var buttonMatrix = new ButtonMatrixComponent(pi4j);

        buttonMatrix.onDown(1, () -> System.out.println("Button 1 pressed"));
        buttonMatrix.onUp(1, () -> System.out.println("Button 1 released"));
        buttonMatrix.onUp(16, () -> System.out.println("Easter Egg!"));

        sleep(30000);
    }
}
