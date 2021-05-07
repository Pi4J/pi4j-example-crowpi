package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.crowpi.components.events.SimpleEventHandler;
import com.pi4j.crowpi.components.exceptions.MeasurementException;
import com.pi4j.io.gpio.digital.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of the CrowPi ultrasonic distance sensor (HC-SR04) using GPIO with Pi4J
 */
public class UltrasonicDistanceSensorComponent extends Component {
    /**
     * Scheduler instance for running the poller thread.
     */
    private final ScheduledExecutorService scheduler;
    /**
     * Active poller thread or null if currently not running.
     */
    private ScheduledFuture<?> poller;

    private final AtomicReference<SimpleEventHandler> objectFoundHandler;
    private final AtomicReference<SimpleEventHandler> objectDisappearedHandler;

    private final AtomicBoolean state;
    private double minRange;
    private double maxRange;
    private double temperature;

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
     * Default period in milliseconds of button state poller.
     * The poller will be run in a separate thread and executed every X milliseconds.
     */
    protected static final long DEFAULT_POLLER_PERIOD_MS = 100;

    /**
     * Pulse length measured
     */
    private volatile double pulseLength;

    /**
     * Creates a new ultrasonic distance sensor component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public UltrasonicDistanceSensorComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN_TRIGGER, DEFAULT_PIN_ECHO, DEFAULT_POLLER_PERIOD_MS);
    }

    /**
     * Creates a new ultrasonic distance sensor component with custom GPIO addresses
     *
     * @param pi4j           Pi4J context
     * @param triggerAddress GPIO address of trigger output
     * @param echoAddress    GPIO address of echo input
     * @param pollerPeriodMs Period of poller in milliseconds
     */
    public UltrasonicDistanceSensorComponent(Context pi4j, int triggerAddress, int echoAddress, long pollerPeriodMs) {
        this.digitalInputEcho = pi4j.create(buildDigitalInputConfig(pi4j, echoAddress));
        this.digitalOutputTrigger = pi4j.create(buildDigitalOutputConfig(pi4j, triggerAddress));

        this.objectFoundHandler = new AtomicReference<>();
        this.objectDisappearedHandler = new AtomicReference<>();
        this.state = new AtomicBoolean(false);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.startPoller(pollerPeriodMs);
    }

    /**
     * (Re-)starts the poller with the desired time period in milliseconds.
     * If the poller is already running, it will be cancelled and rescheduled with the given time.
     * The first poll happens immediately in a separate thread and does not get delayed.
     *
     * @param pollerPeriodMs Polling period in milliseconds
     */
    public void startPoller(long pollerPeriodMs) {
        if (this.poller != null) {
            this.poller.cancel(true);
        }
        this.poller = scheduler.scheduleAtFixedRate(new Poller(), 0, pollerPeriodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the poller immediately, therefore causing the button states to be no longer refreshed.
     * If the poller is already stopped, this method will silently return and do nothing.
     */
    public void stopPoller() {
        if (this.poller != null) {
            this.poller.cancel(true);
            this.poller = null;
        }
    }

    /**
     * Start a measurement with default temperature setting
     *
     * @return Measured distance [cm]
     */
    public double measure() {
        return measure(DEFAULT_TEMPERATURE);
    }

    /**
     * Start a measurement with custom temperature setting. Use this to have a temperature compensation.
     *
     * @param temperature Current environment temperature the ultra sonic sensor is working in. Range -20°C to 40°C
     * @return Measured distance [cm]
     */
    public double measure(double temperature) {
        double pulseLength = measurePulse();
        return calculateDistance(pulseLength, temperature);
    }

    public void onObjectFound(double min, double max, SimpleEventHandler handler) {
        this.onObjectFound(min, max, DEFAULT_TEMPERATURE, handler);
    }

    public void onObjectFound(double min, double max, double temperature, SimpleEventHandler handler) {
        this.minRange = min;
        this.maxRange = max;
        this.temperature = temperature;
        this.objectFoundHandler.set(handler);
    }

    public void onObjectDisappeared(double min, double max, SimpleEventHandler handler) {
        this.onObjectDisappeared(min, max, DEFAULT_TEMPERATURE, handler);
    }

    public void onObjectDisappeared(double min, double max, double temperature, SimpleEventHandler handler) {
        this.minRange = min;
        this.maxRange = max;
        this.temperature = temperature;
        this.objectDisappearedHandler.set(handler);
    }

    /**
     * Triggers the ultrasonic sensor to start a measurement. Measures the time until the ECHO is recognized.
     *
     * @return Time which the ultrasonic signal needs to travel to the next object and return to the sensor
     */
    protected synchronized double measurePulse() {
        // Threading is used to compensate Java delays. The sensor is just a little to fast.
        var measurementTask = new Thread(() -> {
            var triggerTask = new Thread(() -> digitalOutputTrigger.pulse(10, TimeUnit.MILLISECONDS));

            long startTime = 0;
            long endTime = 0;

            triggerTask.start();
            while (digitalInputEcho.isLow() && !Thread.currentThread().isInterrupted()) {
                startTime = System.nanoTime();
            }

            while (digitalInputEcho.isHigh() && !Thread.currentThread().isInterrupted()) {
                endTime = System.nanoTime();
            }

            pulseLength = (double) (endTime - startTime) / 1_000_000;
        });

        measurementTask.start();
        try {
            // Sometimes a measurement can fail. Timeout and return invalid measurement value.
            measurementTask.join(1000);
        } catch (InterruptedException timeout) {
            measurementTask.interrupt();
            throw new MeasurementException("Timed out while retrieving measurement");
        }

        return pulseLength;
    }

    /**
     * Calculates measured distance from pulse length with temperature compensation.
     *
     * @param pulseLength pulse duration in milliseconds
     * @param temperature temperature during the measurement. Range -20°C to 40°C
     * @return distance in centimeters
     */
    protected double calculateDistance(double pulseLength, double temperature) {
        if (temperature > 40 || temperature < -20) {
            throw new IllegalArgumentException("Temperature out of range. Allowed only -20°C to 40°C");
        }

        if (pulseLength >= 25 || pulseLength < 0.1) {
            throw new MeasurementException("Invalid pulse length " + pulseLength + " found, expected value in range 0" +
                ".1 - 25");
        }

        double sonicSpeed = (331.5 + (0.6 * temperature)) / 10; // [cm/ms]
        double distance = pulseLength * sonicSpeed / 2;

        return (double) Math.round(distance * 100) / 100;
    }

    /**
     * Get the instance of the echo input
     *
     * @return A digital input configuration
     */
    public DigitalInput getDigitalInputEcho() {
        return this.digitalInputEcho;
    }

    /**
     * Get the instance of the trigger output
     *
     * @return A digital output configuration
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

    /**
     * Poller class which implements {@link Runnable} to be used with {@link ScheduledExecutorService} for repeated execution.
     * This poller consecutively starts a measurement and checks if it's in range of object
     * Additionally, simple event handlers will be triggered during state transitions.
     */
    private final class Poller implements Runnable {
        @Override
        public void run() {
            double result;

            try {
                result = measure(temperature);
            } catch (MeasurementException e) {
                return;
            }
            final var newState = result <= maxRange && result >= minRange;
            final var oldState = state.getAndSet(newState);

            System.out.println("Measured: " + result);
            if (oldState == newState) {
                return;
            }

            if (newState) {
                triggerSimpleEvent(objectFoundHandler.get());
            } else {
                triggerSimpleEvent(objectDisappearedHandler.get());
            }
        }
    }
}

