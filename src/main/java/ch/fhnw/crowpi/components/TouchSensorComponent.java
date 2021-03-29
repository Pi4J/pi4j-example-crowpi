package ch.fhnw.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.PullResistance;

public class TouchSensorComponent {
    protected final DigitalInput din;
    protected static final int DEFAULT_PIN = 17;
    protected static final PullResistance DEFAULT_PULL_RESISTANCE = PullResistance.PULL_UP;

    public TouchSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN, DEFAULT_PULL_RESISTANCE);
    }

    public TouchSensorComponent(Context pi4j, int address, PullResistance pullResistance) {
        this.din = pi4j.create(buildDinConfig(pi4j, address, pullResistance));
    }

    protected DigitalInput getDin() {
        return din;
    }

    public boolean isHigh() {
        return din.isHigh();
    }

    public void test() {
        din.addListener(event -> {
            Integer count = (Integer) event.source().metadata().get("count").value();
            System.out.println(event + " === " + count);
        });
    }

    protected DigitalInputConfig buildDinConfig(Context pi4j, int address, PullResistance pullResistance) {
        return DigitalInput.newConfigBuilder(pi4j)
                .id("BCM" + address)
                .name("TouchSensor")
                .address(address)
                .pull(pullResistance)
                .build();
    }
}
