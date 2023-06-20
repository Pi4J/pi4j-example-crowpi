open module com.pi4j.crowpi {
    // Module Exports
    exports com.pi4j.crowpi.components;
    exports com.pi4j.crowpi.components.definitions;
    exports com.pi4j.crowpi.components.events;
    exports com.pi4j.crowpi.components.exceptions;
    exports com.pi4j.crowpi.components.helpers;
    exports com.pi4j.crowpi.components.internal;
    exports com.pi4j.crowpi.components.internal.rfid;
    exports com.pi4j.crowpi.helpers;

    // Pi4J Modules
    requires com.pi4j;
    requires com.pi4j.library.pigpio;
    requires com.pi4j.plugin.pigpio;
    requires com.pi4j.plugin.raspberrypi;
    uses com.pi4j.extension.Extension;
    uses com.pi4j.provider.Provider;

    // Logging
    requires java.logging;

    // PicoCLI Modules
    requires info.picocli;

    // AWT
    requires java.desktop;
}
