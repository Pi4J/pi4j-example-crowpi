package ch.fhnw.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of the CrowPi ultrasonic distance sensor (HC-SR04) using GPIO with Pi4J
 */
public class UltrasonicDistanceSensorComponent {
    /**
     * Pi4J digital output instance used by this component
     */
    private final DigitalOutput digitalOutputTrigger;

    /**
     * Pi4J digital input instance used by this component
     */
    private final DigitalInput digitalInputEcho;

    /**
     * Default BCM pins of ultrasonic distance sensor for CrowPi
     */
    protected static final int DEFAULT_PIN_TRIGGER = 16;
    protected static final int DEFAULT_PIN_ECHO = 12;
    /**
     * Default temperature setting to calculate distances
     */
    protected static final double DEFAULT_TEMPERATURE = 20.0;

    /**
     * Creates a new ultrasonic distance sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public UltrasonicDistanceSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN_TRIGGER, DEFAULT_PIN_ECHO);
    }

    /**
     * Creates a new ultrasonic distance sensor component with custom GPIO addresses
     *
     * @param pi4j           Pi4J context
     * @param triggerAddress GPIO address of trigger output
     * @param echoAddress    GPIO address of echo input
     */
    public UltrasonicDistanceSensorComponent(Context pi4j, int triggerAddress, int echoAddress) {
        this.digitalInputEcho = pi4j.create(buildDigitalInputConfig(pi4j, echoAddress));
        this.digitalOutputTrigger = pi4j.create(buildDigitalOutputConfig(pi4j, triggerAddress));
    }

    public double measure() {
        return measure(DEFAULT_TEMPERATURE);
    }

    public double measure(double temperature) {
        double pulseLength = measurePulse();
        return calculateDistance(pulseLength, temperature);
    }

    volatile double delay;

    protected double measurePulse() {

        var meas = new Thread(() -> {
            var t = new Thread(() -> digitalOutputTrigger.pulse(10, TimeUnit.MILLISECONDS));

            long startTime = 0;
            long endTime = 0;

            t.start();
            while (digitalInputEcho.isLow()) {
                startTime = System.nanoTime();
            }

            while (digitalInputEcho.isHigh()) {
                endTime = System.nanoTime();
            }

            delay = (double) (endTime - startTime) / 1_000_000;
        });

        meas.start();
        try {
            meas.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return delay;
    }

    /**
     * @param pulseLength pulse duration in milliseconds
     * @param temperature temperature during the measurement
     * @return distance in centimeters
     */
    protected double calculateDistance(double pulseLength, double temperature) {
        if (temperature > 40 || temperature < -20) {
            throw new IllegalArgumentException("Temperature out of range. Allowed only -20°C to 40°C");
        }

        if (pulseLength >= 25 || pulseLength < 0.1) {
            throw new IllegalArgumentException("Invalid Measurement. Too long pulse.");
        }

        double sonicSpeed = (331.5 + (0.6 * temperature)) / 10; // [cm/ms]
        double distance = pulseLength * sonicSpeed / 2;

        return (double) Math.round(distance * 100) / 100;
    }

    /**
     * {@inheritDoc}
     */
    public DigitalInput getDigitalInputEcho() {
        return this.digitalInputEcho;
    }

    /**
     * {@inheritDoc}
     */
    public DigitalOutput getDigitalOutputTrigger() {
        return this.digitalOutputTrigger;
    }

    /**
     * Builds a new DigitalInput instance for the ultrasonic distance sensor.
     *
     * @param pi4j    Pi4J context
     * @param address GPIO address of ultrasonic distance sensor echo input
     * @return DigitalInput configuration
     */
    protected DigitalInputConfig buildDigitalInputConfig(Context pi4j, int address) {
        return DigitalInput.newConfigBuilder(pi4j)
                .id("BCM-" + address)
                .name("Ultrasonic Distance Sensor ECHO")
                .address(address)
                .pull(PullResistance.PULL_DOWN)
                .build();
    }

    /**
     * Builds a new  instance for the ultrasonic distance sensor.
     *
     * @param pi4j    Pi4J context
     * @param address GPIO address of ultrasonic distance sensor trigger output
     * @return DigitalOutput configuration
     */
    protected DigitalOutputConfig buildDigitalOutputConfig(Context pi4j, int address) {
        return DigitalOutput.newConfigBuilder(pi4j)
                .id("BCM-" + address)
                .name("Ultrasonic Distance Sensor TRIGGER")
                .initial(DigitalState.LOW)
                .address(address)
                .build();
    }
}
