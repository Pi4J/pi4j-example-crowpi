open module ch.fhnw.crowpi {
  // Module Exports
  exports ch.fhnw.crowpi.components;
  exports ch.fhnw.crowpi.components.definitions;
  exports ch.fhnw.crowpi.components.events;
  exports ch.fhnw.crowpi.helpers;

  // Pi4J Modules
  requires com.pi4j;
  requires com.pi4j.library.pigpio;
  requires com.pi4j.plugin.pigpio;
  requires com.pi4j.plugin.raspberrypi;
  uses com.pi4j.extension.Extension;
  uses com.pi4j.provider.Provider;

  // SLF4J Modules
  requires org.slf4j;
  requires org.slf4j.simple;

  // PicoCLI Modules
  requires info.picocli;

  // AWT
  requires java.desktop;
}
