package ch.fhnw.crowpi.applications;

import com.pi4j.context.Context;

import ch.fhnw.crowpi.Application;

public class DummyApp implements Application {
  @Override
  public void execute(Context pi4j) {
    System.out.println("The cake is a lie"); 
  }
}
