package ch.fhnw.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.event.Listener;
import com.pi4j.io.gpio.digital.*;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

@FunctionalInterface
public interface DigitalEvent {
    void onTouch();
}


public class TouchSensorComponent {
    protected final DigitalInput din;
    protected static final int DEFAULT_PIN = 17;

    public TouchSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN);
    }

    public TouchSensorComponent(Context pi4j, int address) {
        this.din = pi4j.create(buildDigitalInputConfig(pi4j, address));
    }

    public boolean isTouched() {
        return din.state().isHigh();
    }

    public DigitalState getState() {
        return din.state();
    }

    public void addListener(Callable<boolean> onTouched) {
        din.addListener(event -> { onTouched(); });
    }

    public void removeListener(DigitalStateChangeListener listener) {
        din.removeListener(listener);
    }

    protected DigitalInput getDigitalInput() {
        return din;
    }

    protected DigitalInputConfig buildDigitalInputConfig(Context pi4j, int address) {
        return DigitalInput.newConfigBuilder(pi4j)
                .id("BCM" + address)
                .name("TouchSensor")
                .address(address)
                .pull(PullResistance.PULL_UP)
                .build();
    }

}
