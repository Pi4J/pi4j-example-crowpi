package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.plugin.mock.provider.pwm.MockPwm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServoMotorComponentTest extends ComponentTest {
    private ServoMotorComponent servoMotor;
    private MockPwm pwm;

    private final static int FREQUENCY = ServoMotorComponent.DEFAULT_FREQUENCY;
    private final static float ANGLE_MIN = ServoMotorComponent.DEFAULT_MIN_ANGLE;
    private final static float ANGLE_MAX = ServoMotorComponent.DEFAULT_MAX_ANGLE;
    private final static float ANGLE_CENTER = (ANGLE_MIN + ANGLE_MAX) / 2;
    private final static float DUTY_CYCLE_MIN = ServoMotorComponent.DEFAULT_MIN_DUTY_CYCLE;
    private final static float DUTY_CYCLE_MAX = ServoMotorComponent.DEFAULT_MAX_DUTY_CYCLE;
    private final static float DUTY_CYCLE_CENTER = (DUTY_CYCLE_MIN + DUTY_CYCLE_MAX) / 2;

    @BeforeEach
    void setUp() {
        this.servoMotor = new ServoMotorComponent(pi4j);
        this.pwm = toMock(servoMotor.getPwm());
    }

    @Test
    void testGetMinAngle() {
        // when
        final var result = servoMotor.getMinAngle();

        // then
        assertEquals(ANGLE_MIN, result);
    }

    @Test
    void testGetMaxAngle() {
        // when
        final var result = servoMotor.getMaxAngle();

        // then
        assertEquals(ANGLE_MAX, result);
    }

    @Test
    void testInvertedOutput() {
        // given
        final var servoMotor = new ServoMotorComponent(pi4j, 0, FREQUENCY, ANGLE_MIN, ANGLE_MAX, DUTY_CYCLE_MAX, DUTY_CYCLE_MIN);
        final var pwm = servoMotor.getPwm();

        // when
        servoMotor.setAngle(ANGLE_CENTER);

        // then
        assertEquals(DUTY_CYCLE_CENTER, pwm.getDutyCycle());
    }

    @ParameterizedTest
    @CsvSource({
        ANGLE_MIN + "," + DUTY_CYCLE_MIN,
        ANGLE_CENTER + "," + DUTY_CYCLE_CENTER,
        ANGLE_MAX + "," + DUTY_CYCLE_MAX,
    })
    void testSetAngle(float angle, float expectedDutyCycle) {
        // when
        servoMotor.setAngle(angle);

        // then
        assertEquals(expectedDutyCycle, pwm.getDutyCycle());
    }

    @ParameterizedTest
    @CsvSource({
        (ANGLE_MIN - 2) + "," + DUTY_CYCLE_MIN,
        (ANGLE_MIN - 1) + "," + DUTY_CYCLE_MIN,
        (ANGLE_MAX + 1) + "," + DUTY_CYCLE_MAX,
        (ANGLE_MAX + 2) + "," + DUTY_CYCLE_MAX
    })
    void testSetAngleClamping(float angle, float expectedDutyCycle) {
        // when
        servoMotor.setAngle(angle);

        // then
        assertEquals(expectedDutyCycle, pwm.getDutyCycle());
    }

    @ParameterizedTest
    @CsvSource({
        "0," + DUTY_CYCLE_MIN,
        "50," + DUTY_CYCLE_CENTER,
        "100," + DUTY_CYCLE_MAX,
    })
    void testSetPercent(float percent, float expectedDutyCycle) {
        // when
        servoMotor.setPercent(percent);

        // then
        assertEquals(expectedDutyCycle, pwm.getDutyCycle());
    }

    @ParameterizedTest
    @CsvSource({
        "-2," + DUTY_CYCLE_MIN,
        "-1," + DUTY_CYCLE_MIN,
        "101," + DUTY_CYCLE_MAX,
        "102," + DUTY_CYCLE_MAX,
    })
    void testSetPercentClamping(float percent, float expectedDutyCycle) {
        // when
        servoMotor.setPercent(percent);

        // then
        assertEquals(expectedDutyCycle, pwm.getDutyCycle());
    }

    @ParameterizedTest
    @CsvSource({
        "0," + DUTY_CYCLE_MIN,
        "3," + DUTY_CYCLE_CENTER,
        "6," + DUTY_CYCLE_MAX,
    })
    void testMoveOnRange(float value, float expectedDutyCycle) {
        // given
        servoMotor.setRange(0, 6);

        // when
        servoMotor.moveOnRange(value);

        // then
        assertEquals(expectedDutyCycle, pwm.getDutyCycle());
    }

    @ParameterizedTest
    @CsvSource({
        "-2," + DUTY_CYCLE_MIN,
        "-1," + DUTY_CYCLE_MIN,
        "7," + DUTY_CYCLE_MAX,
        "8," + DUTY_CYCLE_MAX,
    })
    void testSetRangeClamping(float value, float expectedDutyCycle) {
        // given
        servoMotor.setRange(0, 6);

        // when
        servoMotor.moveOnRange(value);

        // then
        assertEquals(expectedDutyCycle, pwm.getDutyCycle());
    }

    @Test
    void testSetRangeInverted() {
        // given
        servoMotor.setRange(6, 0);

        // when
        servoMotor.moveOnRange(3);

        // then
        assertEquals(DUTY_CYCLE_CENTER, pwm.getDutyCycle());
    }
}
