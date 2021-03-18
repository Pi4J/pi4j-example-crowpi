package ch.fhnw.crowpi.components;

import com.pi4j.context.Context;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;
import com.pi4j.io.pwm.PwmType;

public class BuzzerComponent {
  protected static final int DEFAULT_PIN = 18;
  protected final Pwm pwm;

  public BuzzerComponent(Context pi4j) {
    this(pi4j, DEFAULT_PIN);
  }

  public BuzzerComponent(Context pi4j, int address) {
    this.pwm = pi4j.create(buildPwmConfig(pi4j, address));
  }

  public void playTone(int frequency, int duration) {
    pwm.on(50, frequency);

    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
    } finally {
      pwm.off();
    }
  }

  protected static PwmConfig buildPwmConfig(Context pi4j, int address) {
    return Pwm.newConfigBuilder(pi4j)
        .id("BCM" + address)
        .name("Buzzer")
        .address(address)
        .pwmType(PwmType.HARDWARE)
        .initial(0)
        .shutdown(0)
        .provider("pigpio-pwm")
        .build();
  }
}
