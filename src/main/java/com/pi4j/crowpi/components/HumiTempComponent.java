package com.pi4j.crowpi.components;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This example demonstrates the temperature and humidity component on the CrowPi.
 * As Java does not allow for precise enough timings itself, this component does not use Pi4J to retrieve the pulses
 * of the GPIO pin for DHT11 sensor and instead relies on a linux system driver which reads the pulses and write the
 * results into a file.ut.
 * <p>
 * A clean alternative would be using a separate microcontroller which handles the super precise timing-based communication itself and
 * interacts with the Raspberry Pi using I²C, SPI or any other bus. This would offload the work and guarantee even more accurate results. As
 * the CrowPi does not have such a dedicated microcontroller though, using this driver was the best available approach.
 */
public class HumiTempComponent extends Component {
    /**
     * Scheduler instance for running the poller thread.
     */
    private final ScheduledExecutorService scheduler;
    /**
     * Active poller thread or null if currently not running.
     */
    private ScheduledFuture<?> poller;

    /**
     * Default paths to the files which are written by the DHT11 driver
     */
    private final static String DEFAULT_HUMI_PATH = "/sys/devices/platform/dht11@4/iio:device0/in_humidityrelative_input";
    private final static String DEFAULT_TEMP_PATH = "/sys/devices/platform/dht11@4/iio:device0/in_temp_input";
    /**
     * Paths effectively used to read the values
     */
    private final String humiPath;
    private final String tempPath;

    /**
     * Polling interval of the file reading poller. Do not go to fast it might cause some issues.
     */
    private final static int DEFAULT_POLLING_DELAY_MS = 1000;

    private volatile double temperature;
    private volatile double humidity;

    /**
     * Creates a new humidity and temperature sensor component with default path and polling interval
     */
    public HumiTempComponent() {
        this(DEFAULT_HUMI_PATH, DEFAULT_TEMP_PATH, DEFAULT_POLLING_DELAY_MS);
    }

    /**
     * Creates a new humidity and temperature sensor component with default paths and custom polling interval
     *
     * @param pollingDelayMs Delay in millis between reading measurement values
     */
    public HumiTempComponent(int pollingDelayMs) {
        this(DEFAULT_HUMI_PATH, DEFAULT_TEMP_PATH, pollingDelayMs);
    }

    /**
     * Creates a new humidity and temperature sensor component with custom paths and polling interval
     *
     * @param humiPath       Path to the file containing humidity
     * @param tempPath       Path to the file containing temperature
     * @param pollingDelayMs Polling cycle of reading the measured values
     */
    public HumiTempComponent(String humiPath, String tempPath, int pollingDelayMs) {
        this.humiPath = humiPath;
        this.tempPath = tempPath;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.startPoller(pollingDelayMs);
    }

    /**
     * Gets the last read humidity
     *
     * @return Return the humidity in %
     */
    public double getHumidity() {
        return this.humidity;
    }

    /**
     * Gets the last read temperature
     *
     * @return Returns the temperature in °C
     */
    public double getTemperature() {
        return this.temperature;
    }

    /**
     * (Re-)starts the poller with the desired time period in milliseconds.
     * If the poller is already running, it will be cancelled and rescheduled with the given time.
     * The first poll happens immediately in a separate thread and does not get delayed.
     *
     * @param pollerPeriodMs Polling period in milliseconds
     */
    protected synchronized void startPoller(long pollerPeriodMs) {
        if (this.poller != null) {
            this.poller.cancel(true);
        }
        this.poller = scheduler.scheduleAtFixedRate(new Poller(), 0, pollerPeriodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the internal scheduled future for the poller thread or null if currently stopped.
     *
     * @return Active poller instance or null
     */
    protected ScheduledFuture<?> getPoller() {
        return this.poller;
    }

    /**
     * Poller class which implements {@link Runnable} to be used with {@link ScheduledExecutorService} for repeated execution.
     * This poller consecutively starts reads the values in the humidity and temperature files
     */
    private final class Poller implements Runnable {
        @Override
        public void run() {
            // Read humidity file and convert to value
            try {
                humidity = convertToValue(readFile(humiPath));
            } catch (IOException ignored) {
                ;
            }
            // Read temperature file and convert to value
            try {
                temperature = convertToValue(readFile(tempPath));
            } catch (IOException ignored) {
                ;
            }
        }

        /**
         * Reads a specified file and returns the first line as string
         *
         * @param path Path to the file
         * @return First line of the file as string
         * @throws IOException If the reading fails the IOException is thrown
         */
        protected String readFile(String path) throws IOException {
            try (var input = new BufferedReader(new FileReader(path))) {
                return input.readLine();
            }
        }

        /**
         * Calculates and converts a string into a temperature or humidity value
         *
         * @param line Pass the a line of a humidity or temperature file here
         * @return Return the calculated value as double
         */
        protected double convertToValue(String line) {
            return Double.parseDouble(line) / 1000;
        }
    }
}
