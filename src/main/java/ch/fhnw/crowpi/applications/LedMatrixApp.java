package ch.fhnw.crowpi.applications;

import ch.fhnw.crowpi.Application;
import ch.fhnw.crowpi.components.LedMatrixComponent;
import ch.fhnw.crowpi.components.definitions.Direction;
import com.pi4j.context.Context;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LedMatrixApp implements Application {
    @Override
    public void execute(Context pi4j) {
        final var matrix = new LedMatrixComponent(pi4j);
        matrix.setEnabled(true);
        matrix.setBrightness(1);

//        // Test Graphics2D lambda call
//        matrix.draw(graphics -> {
//            graphics.drawLine(0, 0, 7, 7);
//            graphics.drawLine(7, 0, 0, 7);
//            graphics.drawOval(1, 1, 5, 5);
//        });
//        sleep(500);
//
//        // Test manual scrolling of custom BufferedImage
//        final var image = new BufferedImage(80, 8, BufferedImage.TYPE_BYTE_BINARY);
//        final var graphics = image.createGraphics();
//
//        graphics.setColor(Color.WHITE);
//        graphics.fillRect(0, 0, 80, 8);
//        graphics.setColor(Color.BLACK);
//        graphics.setFont(new Font("pixelmix", Font.PLAIN, 9));
//        graphics.drawString("The cake is a lie", 0, 7);
//
//        for (int x = 0; x < image.getWidth() - 8; x++) {
//            matrix.draw(image, x, 0);
//            sleep(100);
//        }
//
//        // Test integrated print function with scrolling
//        matrix.print("Still Alive");

        for(int i = 0; i < 10; i++) {
            matrix.transition(LedMatrixComponent.Symbol.ARROW_UP, Direction.UP);
            matrix.transition(LedMatrixComponent.Symbol.ARROW_DOWN, Direction.DOWN);
            matrix.transition(LedMatrixComponent.Symbol.ARROW_LEFT, Direction.LEFT);
            matrix.transition(LedMatrixComponent.Symbol.ARROW_RIGHT, Direction.RIGHT);
        }
    }

    public void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {
        }
    }
}
