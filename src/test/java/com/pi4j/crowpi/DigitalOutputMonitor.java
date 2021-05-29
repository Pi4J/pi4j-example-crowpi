package com.pi4j.crowpi;

import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import com.pi4j.io.gpio.digital.DigitalStateChangeListener;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DigitalOutputMonitor implements DigitalStateChangeListener {
    private final List<StateChange> stateChanges = Collections.synchronizedList(new ArrayList<>());

    public DigitalOutputMonitor(DigitalOutput... digitalOutputs) {
        this.attach(digitalOutputs);
    }

    public void attach(DigitalOutput... digitalOutputs) {
        Arrays.asList(digitalOutputs).forEach(o -> o.addListener(this));
    }

    public void detach(DigitalOutput... digitalOutputs) {
        Arrays.asList(digitalOutputs).forEach(o -> o.removeListener(this));
    }

    public List<StateChange> getStateChanges() {
        return Collections.unmodifiableList(stateChanges);
    }

    public void assertStateChanges(StateChange... stateChanges) {
        assertStateChanges(Arrays.asList(stateChanges));
    }

    public void assertStateChanges(List<StateChange> expectedStateChanges) {
        assertEquals(expectedStateChanges, getStateChanges());
    }

    @Override
    public void onDigitalStateChange(DigitalStateChangeEvent event) {
        if (event.source() instanceof DigitalOutput) {
            stateChanges.add(new StateChange((DigitalOutput) event.source(), event.state()));
        }
    }

    public static final class StateChange {
        private final DigitalOutput digitalOutput;
        private final DigitalState state;

        public StateChange(DigitalOutput digitalOutput, DigitalState state) {
            this.digitalOutput = digitalOutput;
            this.state = state;
        }

        public DigitalOutput getDigitalOutput() {
            return digitalOutput;
        }

        public DigitalState getState() {
            return state;
        }

        @Override
        public String toString() {
            return digitalOutput.id() + "=" + state.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StateChange that = (StateChange) o;
            return Objects.equals(digitalOutput, that.digitalOutput) && state == that.state;
        }

        @Override
        public int hashCode() {
            return Objects.hash(digitalOutput, state);
        }
    }
}
