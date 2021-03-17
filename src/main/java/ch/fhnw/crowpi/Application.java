package ch.fhnw.crowpi;

import com.pi4j.context.Context;

public interface Application {
  public void execute(Context pi4j);

  default public String getName() {
    return this.getClass().getSimpleName();
  }

  default public String getDescription() {
    final String classFqdn = this.getClass().getName();
    return "Runs application " + classFqdn;
  }
}
