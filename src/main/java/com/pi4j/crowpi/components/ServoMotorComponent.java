package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;

public class ServoMotorComponent extends Component {
    private final static int DEFAULT_PIN = 25;
    private final static int DEFAULT_FREQUENCY = 50;

    private final static float DEFAULT_MIN_ANGLE = -90;
    private static final float DEFAULT_MAX_ANGLE = 90;
    private final static float DEFAULT_MIN_DUTY_CYCLE = 2;
    private final static float DEFAULT_MAX_DUTY_CYCLE = 12;

    private final Pwm pwm;
    private final float minAngle;
    private final float maxAngle;
    private final float minDutyCycle;
    private final float maxDutyCycle;

    public ServoMotorComponent(Context pi4j) {
        this(pi4j, DEFAULT_MIN_ANGLE, DEFAULT_MAX_ANGLE, DEFAULT_MIN_DUTY_CYCLE, DEFAULT_MAX_DUTY_CYCLE);
    }

    public ServoMotorComponent(Context pi4j, float minAngle, float maxAngle, float minDutyCycle, float maxDutyCycle) {
        this(pi4j, DEFAULT_PIN, DEFAULT_FREQUENCY, minAngle, maxAngle, minDutyCycle, maxDutyCycle);
    }

    public ServoMotorComponent(Context pi4j, int address, int frequency, float minAngle, float maxAngle, float minDutyCycle, float maxDutyCycle) {
        this.pwm = pi4j.create(buildDigitalOutputConfig(pi4j, address, frequency));
        this.minAngle = minAngle;
        this.maxAngle = maxAngle;
        this.minDutyCycle = minDutyCycle;
        this.maxDutyCycle = maxDutyCycle;
    }

    public void setAngle(float angle) {
        pwm.on(mapAngleToDutyCycle(angle));
    }

    public void setPercent(float percent) {
        setRange(percent, 0, 100);
    }

    public void setRange(float value, float minValue, float maxValue) {
        pwm.on(mapToDutyCycle(value, minValue, maxValue));
    }

    protected float mapAngleToDutyCycle(float angle) {
        final float clampedAngle = Math.min(maxAngle, Math.max(minAngle, angle));
        return mapToDutyCycle(clampedAngle, minAngle, maxAngle);
    }

    protected float mapToDutyCycle(float input, float inputStart, float inputEnd) {
        return mapRange(input, inputStart, inputEnd, minDutyCycle, maxDutyCycle);
    }

    private static float mapRange(float input, float inputStart, float inputEnd, float outputStart, float outputEnd) {
        return outputStart + ((outputEnd - outputStart) / (inputEnd - inputStart)) * (input - inputStart);
    }

    public float getMinAngle() {
        return minAngle;
    }

    public float getMaxAngle() {
        return maxAngle;
    }

    public float getMinDutyCycle() {
        return minDutyCycle;
    }

    public float getMaxDutyCycle() {
        return maxDutyCycle;
    }

    protected PwmConfig buildDigitalOutputConfig(Context pi4j, int address, int frequency) {
        return Pwm.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("Servo Motor")
            .address(address)
            .frequency(frequency)
            .initial(0)
            .shutdown(0)
            .build();
    }
}
