package com.pi4j.crowpi.components;

import com.pi4j.crowpi.components.exceptions.MeasurementException;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the CrowPi DHT11.
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

    private final static String DEFAULT_HUMI_PATH = "/sys/devices/platform/dht11@4/iio:device0/in_humidityrelative_input";
    private final static String DEFAULT_TEMP_PATH = "/sys/devices/platform/dht11@4/iio:device0/in_temp_input";
    private final static int DEFAULT_POLLING_DELAY_MS = 1000;

    private final String humiPath;
    private final String tempPath;
    private final int pollingDelayMs;

    private volatile double temperature;
    private volatile double humidity;

    /**
     * Creates a new humidity and temperature sensor component with default paths
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
     * Creates a new humidity and temperature sensor component with custom paths
     *
     * @param humiPath       Path to the file containing humidity
     * @param tempPath       Path to the file containing temperature
     * @param pollingDelayMs Polling cycle of reading the measured values
     */
    public HumiTempComponent(String humiPath, String tempPath, int pollingDelayMs) {
        this.humiPath = humiPath;
        this.tempPath = tempPath;
        this.pollingDelayMs = pollingDelayMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.startPoller(pollingDelayMs);
    }

    public double getHumidity() {
        return this.humidity;
    }

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
     * Stops the poller immediately, therefore causing the button states to be no longer refreshed.
     * If the poller is already stopped, this method will silently return and do nothing.
     */
    protected synchronized void stopPoller() {
        if (this.poller != null) {
            this.poller.cancel(true);
            this.poller = null;
        }
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
            } catch (IOException e) {
                System.out.println("Reading the file failed. Don't worry that happens sometimes");
            }
            // Read temperature file and convert to value
            try {
                temperature = convertToValue(readFile(tempPath));
            } catch (IOException e) {
                System.out.println("Reading the file failed. Don't worry that happens sometimes");
            }

            System.out.println("Temperature: " + temperature);
            System.out.println("Humidity: " + humidity);
        }

        private String readFile(String path) throws IOException {
            try (var input = new BufferedReader(new FileReader(path))) {
                return input.readLine();
            }
        }

        private double convertToValue(String line) {
            return Double.parseDouble(line) / 1000;
        }
    }
}
