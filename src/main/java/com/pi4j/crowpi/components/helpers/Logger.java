package com.pi4j.crowpi.components.helpers;

import java.util.logging.Level;
import java.util.logging.LogManager;

public class Logger {
    private final java.util.logging.Logger logger;

    public Logger() {
        logger = LogManager.getLogManager().getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);

        //set appropriate log level
        logger.setLevel(Level.INFO);
    }

    public void trace(String message, Object... args) {
        logger.finest(() -> String.format(message, args));
    }

    public void debug(String message, Object... args) {
        logger.finer(() -> String.format(message, args));
    }

    public void info(String message, Object... args) {
        logger.info(() -> String.format(message, args));
    }

    public void warn(String message, Object... args) {
        logger.warning(() -> String.format(message, args));
    }

    public void error(String message, Object... args) {
        logger.severe(() -> String.format(message, args));
    }
}
