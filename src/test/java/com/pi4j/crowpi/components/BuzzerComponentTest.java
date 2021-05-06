package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.io.pwm.Pwm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuzzerComponentTest extends ComponentTest {
    protected static final double DUTY_CYCLE_DELTA = 0.1;

    protected BuzzerComponent buzzer;
    protected Pwm buzzerPwm;

    @BeforeEach
    public void setUp() {
        this.buzzer = new BuzzerComponent(pi4j);
        this.buzzerPwm = this.buzzer.getPwm();
    }

    @Test
    public void testPlayTone() {
        // when
        this.buzzer.playTone(1000);

        // then
        assertTrue(this.buzzerPwm.isOn());
        assertEquals(1000, this.buzzerPwm.frequency());
        assertEquals(50, this.buzzerPwm.dutyCycle(), DUTY_CYCLE_DELTA);
    }

    @Test
    public void testPlayToneWithDuration() {
        // when
        this.buzzer.playTone(1000, 10);

        // then
        assertTrue(this.buzzerPwm.isOff());
        assertEquals(1000, buzzerPwm.frequency());
        assertEquals(50, buzzerPwm.dutyCycle(), DUTY_CYCLE_DELTA);
    }

    @Test
    public void testPlayToneWithInterrupt() {
        // when
        Thread.currentThread().interrupt();
        buzzer.playTone(1000, 5000);

        // then
        assertTrue(this.buzzerPwm.isOff());
    }

    @Test
    public void testPlaySilence() {
        // given
        buzzer.playTone(1000);

        // when
        buzzer.playSilence();

        // then
        assertTrue(buzzerPwm.isOff());
    }

    @Test
    public void testPlaySilenceInterrupt() {
        // given
        buzzer.playTone(1000);

        // when
        Thread.currentThread().interrupt();
        buzzer.playSilence(5000);

        // then
        assertTrue(buzzerPwm.isOff());
    }
}
