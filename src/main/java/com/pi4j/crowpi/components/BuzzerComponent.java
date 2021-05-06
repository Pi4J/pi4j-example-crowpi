package com.pi4j.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;
import com.pi4j.io.pwm.PwmType;

/**
 * Implementation of the CrowPi buzzer using PWM with Pi4J
 */
public class BuzzerComponent extends Component {
    /**
     * If no pin is specified by the user, the default BCM pin 18 is used.
     */
    protected static final int DEFAULT_PIN = 18;
    protected final Pwm pwm;

    /**
     * Creates a new buzzer component using the default pin.
     *
     * @param pi4j Pi4J context
     */
    public BuzzerComponent(Context pi4j) {
        this(pi4j, DEFAULT_PIN);
    }

    /**
     * Creates a new buzzer component with a custom BCM pin.
     *
     * @param pi4j    Pi4J context
     * @param address Custom BCM pin address
     */
    public BuzzerComponent(Context pi4j, int address) {
        this.pwm = pi4j.create(buildPwmConfig(pi4j, address));
    }

    /**
     * Plays a tone with the given frequency in Hz indefinitely.
     * This method is non-blocking and returns immediately.
     * A frequency of zero causes the buzzer to play silence.
     *
     * @param frequency Frequency in Hz
     */
    public void playTone(int frequency) {
        playTone(frequency, 0);
    }

    /**
     * Plays a tone with the given frequency in Hz for a specific duration.
     * This method is blocking and will sleep until the specified duration has passed.
     * A frequency of zero causes the buzzer to play silence.
     * A duration of zero to play the tone indefinitely and return immediately.
     *
     * @param frequency Frequency in Hz
     * @param duration  Duration in milliseconds
     */
    public void playTone(int frequency, int duration) {
        if (frequency > 0) {
            // Activate the PWM with a duty cycle of 50% and the given frequency in Hz.
            // This causes the buzzer to be on for half of the time during each cycle, resulting in the desired frequency.
            pwm.on(50, frequency);

            // If the duration is larger than zero, the tone should be automatically stopped after the given duration.
            if (duration > 0) {
                sleep(duration);
                this.playSilence();
            }
        } else {
            this.playSilence(duration);
        }
    }

    /**
     * Silences the buzzer and returns immediately.
     */
    public void playSilence() {
        pwm.off();
    }

    /**
     * Silences the buzzer and waits for the given duration.
     * This method is blocking and will sleep until the specified duration has passed.
     *
     * @param duration Duration in milliseconds
     */
    public void playSilence(int duration) {
        this.playSilence();
        sleep(duration);
    }

    /**
     * Returns the created PWM instance for the buzzer
     *
     * @return PWM instance
     */
    protected Pwm getPwm() {
        return this.pwm;
    }

    /**
     * Builds a new PWM configuration for the buzzer
     *
     * @param pi4j    Pi4J context
     * @param address BCM pin address
     * @return PWM configuration
     */
    protected static PwmConfig buildPwmConfig(Context pi4j, int address) {
        return Pwm.newConfigBuilder(pi4j)
            .id("BCM" + address)
            .name("Buzzer")
            .address(address)
            .pwmType(PwmType.HARDWARE)
            .initial(0)
            .shutdown(0)
            .build();
    }
}
