package ch.fhnw.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import java.util.function.Consumer;

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

    public Object addListener(Consumer<DigitalState> onTouched) {
        DigitalStateChangeListener digitalStateChangeListener = createStateChangeListener(onTouched);
        din.addListener(digitalStateChangeListener);

        return digitalStateChangeListener;
    }

    public void removeListener(Object stateChangeListenerObject) {
        din.removeListener((DigitalStateChangeListener) stateChangeListenerObject);
    }

    protected DigitalStateChangeListener createStateChangeListener(Consumer<DigitalState> consumer) {
        return event -> consumer.accept(event.state());
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
