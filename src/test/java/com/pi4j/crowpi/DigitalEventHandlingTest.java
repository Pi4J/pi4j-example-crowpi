package com.pi4j.crowpi;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.events.DigitalEventProvider;
import com.pi4j.crowpi.components.events.EventHandler;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

public class DigitalEventHandlingTest extends ComponentTest {
    protected DummyComponent component;
    protected static final EventHandler<Boolean> dummyHandler = state -> {
    };

    @BeforeEach
    void setUp() {
        this.component = new DummyComponent(pi4j);
    }

    @Test
    void testAddListener() {
        // when
        final var listener = component.addListener(dummyHandler);

        // then
        assertNotNull(listener);
    }

    @Test
    void testRemoveListener() {
        // given
        final var listener = component.addListener(dummyHandler);

        // when
        final Executable t = listener::remove;

        // then
        assertDoesNotThrow(t);
    }

    @Test
    void testAddTwoListenersAreNotEqual() {
        // when
        final var listener1 = component.addListener(dummyHandler);
        final var listener2 = component.addListener(dummyHandler);

        // then
        assertNotEquals(listener1, listener2);
    }

    private final static class DummyComponent implements DigitalEventProvider<Boolean> {
        private final DigitalInput digitalInput;

        public DummyComponent(Context pi4j) {
            this.digitalInput = pi4j.create(buildDigitalInputConfig(pi4j));
        }

        private DigitalInputConfig buildDigitalInputConfig(Context pi4j) {
            return DigitalInput.newConfigBuilder(pi4j)
                .id("DUMMY")
                .name("Dummy")
                .address(0)
                .build();
        }

        @Override
        public DigitalInput getDigitalInput() {
            return digitalInput;
        }

        @Override
        public Boolean mapDigitalState(DigitalState digitalState) {
            return true;
        }
    }
}
