package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ButtonMatrixComponentTest extends ComponentTest {
    protected ButtonMatrixComponent buttonMatrix;

    @BeforeEach
    void setUp() {
        this.buttonMatrix = new ButtonMatrixComponent(pi4j);
    }

    @Test
    void testInvalidStateMapping() {
        // when
        final Executable t = () -> {
            new ButtonMatrixComponent(pi4j, new int[]{1, 2}, new int[]{3, 4}, new int[]{5, 6}, 1);
        };

        // then
        assertThrows(IllegalArgumentException.class, t);
    }

    @Test
    void testStartPollerOverride() {
        // when
        final var oldPoller = buttonMatrix.getPoller();
        buttonMatrix.startPoller(1);
        final var newPoller = buttonMatrix.getPoller();

        // then
        assertTrue(oldPoller.isDone());
        assertFalse(newPoller.isDone());
    }

    @Test
    void testStopPoller() {
        // when
        final var oldPoller = buttonMatrix.getPoller();
        buttonMatrix.stopPoller();

        // then
        assertNull(buttonMatrix.getPoller());
        assertTrue(oldPoller.isDone());
    }

    @Test
    void testReadBlockingTimeout() {
        // given
        final var executor = Executors.newSingleThreadExecutor();
        final var future = executor.submit(() -> buttonMatrix.readBlocking(1));

        // when
        final ThrowingSupplier<Integer> t = () -> future.get(1, TimeUnit.SECONDS);

        // then
        final var result = assertDoesNotThrow(t);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertEquals(-1, result);
    }

    @Test
    void testGetPressedButtons() {
        // when
        final var result = buttonMatrix.getPressedButtons();

        // then
        assertNotNull(result);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16})
    void testGetState(int number) {
        // when
        final Executable t = () -> buttonMatrix.getState(number);

        // then
        assertDoesNotThrow(t);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 17, 18})
    void testGetStateBounds(int number) {
        // when
        final Executable t = () -> buttonMatrix.getState(number);

        // then
        assertThrows(IndexOutOfBoundsException.class, t);
    }
}
