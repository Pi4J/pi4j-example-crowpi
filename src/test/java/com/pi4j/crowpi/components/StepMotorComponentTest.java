package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.crowpi.DigitalOutputMonitor;
import com.pi4j.crowpi.DigitalOutputMonitor.StateChange;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pi4j.io.gpio.digital.DigitalState.HIGH;
import static com.pi4j.io.gpio.digital.DigitalState.LOW;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StepMotorComponentTest extends ComponentTest {
    protected StepMotorComponent stepMotor;
    protected MockDigitalOutput[] digitalOutputs;
    protected DigitalOutputMonitor digitalOutputMonitor;

    @BeforeEach
    void setUp() {
        this.stepMotor = new StepMotorComponent(pi4j);
        this.digitalOutputs = toMock(stepMotor.getDigitalOutputs());
        this.digitalOutputMonitor = new DigitalOutputMonitor(digitalOutputs);
    }

    @Test
    void testInvalidStepData() {
        // when
        final Executable t = () -> new StepMotorComponent(pi4j, new int[]{1, 2, 3}, new int[][]{{4}}, 1);

        // then
        assertThrows(IllegalArgumentException.class, t);
    }

    @Test
    void testTurnPositiveDegrees() {
        // when rotating by +5 degrees
        stepMotor.turnDegrees(5);

        // then expect 7 forward steps
        digitalOutputMonitor.assertStateChanges(repeatStateChanges(7, getForwardSteps()));
    }

    @Test
    void testTurnNegativeDegrees() {
        // when rotating by -5 degrees
        stepMotor.turnDegrees(-5);

        // then expect 7 backward steps
        digitalOutputMonitor.assertStateChanges(repeatStateChanges(7, getBackwardSteps()));
    }

    @Test
    void testTurnForward() {
        // when
        stepMotor.turnForward(1);

        // then
        digitalOutputMonitor.assertStateChanges(getForwardSteps());
    }

    @Test
    void testTurnBackward() {
        // when
        stepMotor.turnBackward(1);

        // then
        digitalOutputMonitor.assertStateChanges(getBackwardSteps());
    }

    private StateChange[] getForwardSteps() {
        return new StateChange[]{
            // Step #0
            stateChange(3, HIGH), stateChange(0, HIGH),
            stateChange(3, LOW), stateChange(0, LOW),

            // Step #1
            stateChange(0, HIGH),
            stateChange(0, LOW),

            // Step #2
            stateChange(0, HIGH), stateChange(1, HIGH),
            stateChange(0, LOW), stateChange(1, LOW),

            // Step #3
            stateChange(1, HIGH),
            stateChange(1, LOW),

            // Step #4
            stateChange(1, HIGH), stateChange(2, HIGH),
            stateChange(1, LOW), stateChange(2, LOW),

            // Step #5
            stateChange(2, HIGH),
            stateChange(2, LOW),

            // Step #6
            stateChange(3, HIGH), stateChange(2, HIGH),
            stateChange(3, LOW), stateChange(2, LOW),

            // Step #7
            stateChange(3, HIGH),
            stateChange(3, LOW),
        };
    }

    private StateChange[] getBackwardSteps() {
        return new StateChange[]{
            // Step #0
            stateChange(3, HIGH),
            stateChange(3, LOW),

            // Step #1
            stateChange(3, HIGH), stateChange(2, HIGH),
            stateChange(3, LOW), stateChange(2, LOW),

            // Step #2
            stateChange(2, HIGH),
            stateChange(2, LOW),

            // Step #3
            stateChange(1, HIGH), stateChange(2, HIGH),
            stateChange(1, LOW), stateChange(2, LOW),

            // Step #4
            stateChange(1, HIGH),
            stateChange(1, LOW),

            // Step #5
            stateChange(0, HIGH), stateChange(1, HIGH),
            stateChange(0, LOW), stateChange(1, LOW),

            // Step #6
            stateChange(0, HIGH),
            stateChange(0, LOW),

            // Step #7
            stateChange(3, HIGH), stateChange(0, HIGH),
            stateChange(3, LOW), stateChange(0, LOW),
        };
    }

    private StateChange stateChange(int digitalOutputIndex, DigitalState digitalState) {
        return new StateChange(digitalOutputs[digitalOutputIndex], digitalState);
    }

    private List<StateChange> repeatStateChanges(int count, StateChange... stateChanges) {
        // Convert state changes into list
        final var singleList = Arrays.asList(stateChanges);

        // Create new list with enough capacity to store the initial list times `count` and insert elements
        final var resultList = new ArrayList<StateChange>(singleList.size() * count);
        for (int i = 0; i < count; i++) {
            resultList.addAll(singleList);
        }

        return resultList;
    }
}
