package ch.fhnw.crowpi.components;

import ch.fhnw.crowpi.ComponentTest;
import ch.fhnw.crowpi.components.ButtonComponent.ButtonState;
import ch.fhnw.crowpi.components.definitions.Button;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ButtonComponentTest extends ComponentTest {
    protected ButtonComponent button;
    protected DigitalInput digitalInput;

    @BeforeEach
    void setUp() {
        button = new ButtonComponent(pi4j, Button.UP);
        digitalInput = button.getDigitalInput();
    }

    @ParameterizedTest
    @CsvSource({
        "UNKNOWN,UNKNOWN",
        "LOW,DOWN",
        "HIGH,UP"
    })
    void mapDigitalState(DigitalState digitalState, ButtonState expected) {
        // when
        final var actual = button.mapDigitalState(digitalState);

        // then
        assertEquals(expected, actual);
    }
}
