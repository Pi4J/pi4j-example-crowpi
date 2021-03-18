module ch.fhnw.crowpi {
  // Pi4J Modules
  requires com.pi4j;
  requires com.pi4j.plugin.pigpio;
  uses com.pi4j.extension.Extension;
  uses com.pi4j.provider.Provider;

  // SLF4J Modules
  requires org.slf4j;
  requires org.slf4j.simple;

  // PicoCLI Modules
  requires info.picocli;

  // Open this package for Pi4J and PicoCLI
  opens ch.fhnw.crowpi to com.pi4j, info.picocli;
}
