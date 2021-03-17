module ch.fhnw.crowpi {
  // SLF4J Modules
  requires org.slf4j;
  requires org.slf4j.simple;

  // Pi4J Modules
  requires com.pi4j.plugin.pigpio;
  requires com.pi4j;

  // PicoCLI Modules
  requires info.picocli;

  uses com.pi4j.extension.Extension;
  uses com.pi4j.provider.Provider;

  opens ch.fhnw.crowpi to com.pi4j;
}
