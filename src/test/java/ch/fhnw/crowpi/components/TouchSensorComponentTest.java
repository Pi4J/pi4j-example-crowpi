package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.ComponentTest;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TouchSensorComponentTest extends ComponentTest {
    protected TouchSensorComponent touchSensor;
    protected DigitalInput din;
    protected Consumer<DigitalState> consumer;

    @BeforeEach
    public void setUp() {
        touchSensor = new TouchSensorComponent(pi4j);
        din = touchSensor.getDigitalInput();
    }

    @Test
    public void testGetSensorState() {
        //given
        DigitalState resultState;

        // when
        resultState = touchSensor.getState();

        // then
        assertEquals(DigitalState.LOW, resultState);
    }

    @Test
    public void testIsTouched() {
        // given
        boolean result;

        // when
        result = touchSensor.isTouched();

        // then
        assertEquals(din.isHigh(), result);
    }

    @Test
    public void testAddingTwoListenersAreNotSame() {
        // given
        consumer = new Consumer<DigitalState>() {
            @Override
            public void accept(DigitalState digitalState) {
                var dummy = 0;
            }
        };

        // when
        Object listener1 = touchSensor.addListener(consumer);
        Object listener2 = touchSensor.addListener(consumer);

        // then
        assertNotEquals(listener1, listener2);
    }

    @Test
    public void testAddAndRemoveListener() {
        // given
        consumer = new Consumer<DigitalState>() {
            @Override
            public void accept(DigitalState digitalState) {
                var dummy = 0;
            }
        };

        // when
        Object listener1 = touchSensor.addListener(consumer);

        // then
        assertDoesNotThrow(() -> {
            touchSensor.removeListener(listener1);
        });
    }
}
