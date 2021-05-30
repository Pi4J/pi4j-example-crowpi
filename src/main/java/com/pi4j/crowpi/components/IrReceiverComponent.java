package com.pi4j.crowpi.components;

import com.pi4j.crowpi.components.events.EventHandler;
import com.pi4j.crowpi.components.helpers.ByteHelpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Implementation of the CrowPi IR receiver /not/ using GPIO with Pi4J
 * <p>
 * Unfortunately the tight timing constraints of the infrared receiver which are in the area of microseconds can not be handled by the JVM.
 * While Pi4J does not drop any event and still processes them reliably, the timing is too far off to be useful for any signal processing.
 * As the bundled IR remote of the CrowPi can be used as a great input device, this class provides an alternative without Pi4J.
 * <p>
 * The binary `mode2` provided by the LIRC software package is being used to record the pulses of the IR transmitter / remote.
 * This implementation will automatically run `mode2` pointed towards a `lirc` kernel device and parses its output to recognize IR signals.
 * While receiving the pulses is done outside of Java, all the processing and handling is still being covered as part of this class.
 */
public class IrReceiverComponent extends Component {
    /**
     * Default binary path to the `mode2` binary of the LIRC software package
     */
    private final static String DEFAULT_MODE2_BINARY = "/usr/bin/mode2";
    /**
     * Default kernel device path for the IR receiver
     */
    private final static String DEFAULT_DEVICE_PATH = "/dev/lirc0";

    /**
     * Binary path to `mode2` used for polling
     */
    private final String mode2Binary;
    /**
     * Kernel device path for interacting with the IR receiver
     */
    private final String devicePath;

    /**
     * Current instance of poller manager or null if not running
     */
    private PollerManager pollerManager;
    /**
     * Current thread instance of the poller manager or null if not running
     */
    private Thread pollerManagerThread;

    /**
     * Handler for received IR key press
     */
    private final AtomicReference<EventHandler<Key>> onKeyPressedHandler;

    /**
     * Default poller process factory, should only be changed during tests for proper mocking.
     */
    protected PollerProcessFactory pollerProcessFactory = this::createNativePollerProcess;

    /**
     * Creates a new IR receiver using the default binary and kernel device path.
     */
    public IrReceiverComponent() {
        this(DEFAULT_MODE2_BINARY, DEFAULT_DEVICE_PATH);
    }

    /**
     * Creates a new IR receiver using a custom mode2 binary and kernel device path.
     *
     * @param mode2Binary Path to `mode2` binary provided by LIRC
     * @param devicePath  Absolute path to kernel device, e.g. /dev/lirc0
     */
    public IrReceiverComponent(String mode2Binary, String devicePath) {
        this.mode2Binary = mode2Binary;
        this.devicePath = devicePath;
        this.onKeyPressedHandler = new AtomicReference<>();
    }

    /**
     * Sets or disables the handler for received IR key presses.
     * This will automatically start or stop the poller as needed.
     *
     * @param handler Event handler to call or null to disable
     */
    public synchronized void onKeyPressed(EventHandler<Key> handler) {
        if (handler != null) {
            startPollerManager();
            onKeyPressedHandler.set(handler);
        } else {
            onKeyPressedHandler.set(null);
            stopPollerManager();
        }
    }

    /**
     * Returns the instance of the poller manager or null if currently not running.
     *
     * @return Poller manager instance or null
     */
    protected PollerManager getPollerManager() {
        return pollerManager;
    }

    /**
     * Returns the instance of the poller manager thread or null if currently not running.
     *
     * @return Poller manager thread or null
     */
    protected Thread getPollerManagerThread() {
        return pollerManagerThread;
    }

    /**
     * Starts the poller manager if not already running.
     */
    private void startPollerManager() {
        if (this.pollerManagerThread == null) {
            this.pollerManager = new PollerManager();
            this.pollerManagerThread = new Thread(this.pollerManager);
            this.pollerManagerThread.start();
        }
    }

    /**
     * Stops the poller manager if currently running.
     */
    private void stopPollerManager() {
        if (this.pollerManagerThread != null) {
            this.pollerManagerThread.interrupt();
            try {
                this.pollerManagerThread.join();
            } catch (InterruptedException e) {
                logger.warn("Could not stop IR signal poller manager", e);
            } finally {
                this.pollerManagerThread = null;
                this.pollerManager = null;
            }
        }
    }

    /**
     * Starts a new poller process using {@link ProcessBuilder} and returns a {@link NativePollerProcess} instance.
     * This will use {@link #mode2Binary} and {@link #devicePath} for constructing the proper arguments.
     * Using a separate method for this allows properly unit testing the poller thread itself.
     *
     * @return Process instance
     */
    private NativePollerProcess createNativePollerProcess() throws IOException {
        final var processBuilder = new ProcessBuilder(
            mode2Binary,
            "--driver", "default",
            "--device", devicePath
        );
        return new NativePollerProcess(processBuilder.start());
    }

    /**
     * Functional interface for a poller process factory which creates new {@link PollerProcess} instances on demand.
     */
    @FunctionalInterface
    protected interface PollerProcessFactory {
        PollerProcess create() throws IOException;
    }

    /**
     * Custom interface as an alternative to the Java native {@link Process} interface.
     * This is required for proper unit testing of the poller as Java provides no sane way for mocking {@link Process} itself.
     */
    protected interface PollerProcess {
        InputStream getInputStream();

        boolean isAlive();

        void destroy();
    }

    /**
     * Implementation of {@link PollerProcess} which wraps around a regular Java {@link Process} instance.
     */
    private static final class NativePollerProcess implements PollerProcess {
        private final Process process;

        public NativePollerProcess(Process process) {
            this.process = process;
        }

        @Override
        public InputStream getInputStream() {
            return process.getInputStream();
        }

        @Override
        public boolean isAlive() {
            return process.isAlive();
        }

        @Override
        public void destroy() {
            process.destroy();
        }
    }

    /**
     * Poller manager class which implements {@link Runnable} and should be ran in a separate thread.
     * This poller manager will automatically launch a poller process and thread and monitors them.
     * In case either the poller process or thread dies, it will be automatically restarted.
     * This thread continues to run until the interrupted flag gets set.
     */
    protected final class PollerManager implements Runnable {
        private Poller poller;
        private PollerProcess pollerProcess;
        private Thread pollerThread;

        @Override
        public void run() {
            // Attempt to start IR signal poller
            try {
                restartPoller();
            } catch (IOException | InterruptedException e) {
                logger.error("Could not start IR signal poller", e);
                return;
            }

            // Monitor IR signal poller and auto-restart if needed
            while (!Thread.interrupted()) {
                if (!pollerProcess.isAlive() || !pollerThread.isAlive()) {
                    logger.warn("Restarting IR signal poller after crash");
                    try {
                        restartPoller();
                    } catch (IOException | InterruptedException e) {
                        logger.error("Could not restart IR signal poller", e);
                        return;
                    }
                }
            }
        }

        /**
         * Returns the current poller process managed by this poller manager or null if not running.
         *
         * @return Instance of poller process
         */
        protected PollerProcess getPollerProcess() {
            return pollerProcess;
        }

        /**
         * (Re-)start the poller process and thread.
         * If the poller is not already running, it will be just launched regularly.
         * If the poller is already running, it will be stopped and a new poller gets launched.
         *
         * @throws InterruptedException Stopping of previous poller instance was interrupted
         * @throws IOException          Could not launch poller process
         */
        private void restartPoller() throws InterruptedException, IOException {
            // Stop any previous poller, we can not run twice
            stopPoller();

            // Start poller process and thread
            this.pollerProcess = pollerProcessFactory.create();
            this.poller = new Poller(pollerProcess.getInputStream());
            this.pollerThread = new Thread(poller);
            this.pollerThread.start();
        }

        /**
         * Stops the poller process and thread if currently running.
         *
         * @throws InterruptedException Stopping of previous poller instance was interrupted
         */
        private void stopPoller() throws InterruptedException {
            // Stop previous poller process if available
            if (this.pollerProcess != null) {
                this.pollerProcess.destroy();
                this.pollerProcess = null;
            }

            // Stop previous poller thread if available
            if (this.pollerThread != null) {
                this.pollerThread.interrupt();
                this.pollerThread.join();
                this.pollerThread = null;
            }

            // Kill previous poller instance
            this.poller = null;
        }
    }

    /**
     * Poller class which implements {@link Runnable} and is supposed to be launched in a separate thread by {@link PollerManager}.
     * This poller will permanently monitor the standard output of the mode2 and tries to interpret them as a IR signal.
     * In case of a successful match, the handler specified by {@link #onKeyPressedHandler} will be dispatched.
     */
    private final class Poller implements Runnable {
        /**
         * Timeout in milliseconds before signal processing gets aborted
         */
        private final static long SIGNAL_TIMEOUT_MILLISECONDS = 250;
        /**
         * Maximum time for raising or falling edge of a pulse in microseconds before being discarded
         */
        private final static long PULSE_TIMEOUT_MICROSECONDS = 2400;
        /**
         * Time in microseconds for a time slice of a pulse, can be used to measure a pulse length
         */
        private final static long PULSE_TIME_SLICE_MICROSECONDS = 60;
        /**
         * Threshold of time slice count before considering the current bit as set
         */
        private final static long PULSE_SET_BIT_THRESHOLD = 12;

        /**
         * Buffered reader for processing stdout of mode2 line-by-line
         */
        private final BufferedReader stdout;
        /**
         * Regex pattern for interpreting a mode2 "pulse" via stdout (pulse = raising edge)
         */
        private final Pattern pulsePattern = Pattern.compile("^pulse (\\d+)$");
        /**
         * Regex pattern for interpreting a mode2 "space" via stdout (space = falling edge)
         */
        private final Pattern spacePattern = Pattern.compile("^space (\\d+)$");

        /**
         * Constructs a new poller for the given stdout input stream of a mode2 process.
         *
         * @param stdout Input stream of mode2 stdout
         */
        public Poller(InputStream stdout) {
            this.stdout = new BufferedReader(new InputStreamReader(stdout));
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    // Sleep shortly before retrying if no output is available
                    if (!stdout.ready()) {
                        sleep(1);
                        continue;
                    }

                    // We have some output ready, attempt to parse IR signal
                    processSignal();
                } catch (IOException e) {
                    logger.warn("Received exception during IR signal processing", e);
                }
            }
        }

        /**
         * Processes a single IR signal consisting out of 32 different pulses, resulting in a 4 byte value.
         * These 4 bytes are used to calculate two checksums and the third value is equal to the payload / keycode.
         *
         * @throws IOException Reading from stdout stream has failed
         */
        private void processSignal() throws IOException {
            // Initialize state variables for this measurement
            long deadline = System.currentTimeMillis() + SIGNAL_TIMEOUT_MILLISECONDS;
            final var pulses = new ArrayList<Pulse>();
            long lastRaisingEdge = -1;

            // Output debug message informing about measurement start
            logger.debug("Starting IR signal measurement with timeout of {}ms", SIGNAL_TIMEOUT_MILLISECONDS);

            // Wait for 32 valid pulses or timeout to occur
            while (!Thread.interrupted() && System.currentTimeMillis() < deadline && pulses.size() < 32) {
                // If there is no output available, sleep shortly before retrying
                if (!stdout.ready()) {
                    sleep(1);
                    continue;
                }

                // Read output line and initialize matchers
                final var line = stdout.readLine();
                final var pulseMatcher = pulsePattern.matcher(line);
                final var spaceMatcher = spacePattern.matcher(line);
                logger.trace("Received output from mode2: {}", line);

                // If we have a pulse, store the time in `lastRaisingEdge` and continue loop
                if (pulseMatcher.matches()) {
                    lastRaisingEdge = Long.parseLong(pulseMatcher.group(1));
                    continue;
                }

                // Ignore and continue loop if we do not match a space or have no known raising edge time
                if (!spaceMatcher.matches() || lastRaisingEdge == -1) {
                    continue;
                }

                // Parse time needed for falling edge, instantiate a new pulse and reset last raising edge
                final long lastFallingEdge = Long.parseLong(spaceMatcher.group(1));
                final var pulse = new Pulse(lastRaisingEdge, lastFallingEdge);
                lastRaisingEdge = -1;

                // Skip this pulse if either edge has taken too long
                if (pulse.getMaxEdgeTime() >= PULSE_TIMEOUT_MICROSECONDS) {
                    logger.trace("Skipping pulse which took too long: {}", pulse);
                    continue;
                }

                // Otherwise collect this pulse for later processing
                logger.trace("Added pulse for later processing: {}", pulse);
                pulses.add(pulse);
            }

            // Silently abort if we have not received enough pulses
            if (pulses.size() != 32) {
                logger.debug("IR signal measurement timed out with {} detected pulses", pulses.size());
                return;
            }

            // Construct IR payload by analyzing pulses
            final var payload = new byte[4];
            int shiftOffset = 0;
            int payloadIndex = 0;

            for (final var pulse : pulses) {
                // Count number of full time slices which occurred during falling edge
                final long count = pulse.getFallingEdge() / PULSE_TIME_SLICE_MICROSECONDS;

                // Check if we crossed the threshold to consider the current bit as set
                if (count >= PULSE_SET_BIT_THRESHOLD) {
                    payload[payloadIndex] |= 1 << shiftOffset;
                    logger.trace("Setting bit {} for payload[{}] for pulse length {}", shiftOffset, payloadIndex, count);
                } else {
                    logger.trace("Clearing bit {} for payload[{}] for pulse length {}", shiftOffset, payload, count);
                }

                // Once we have reached bit 7 we will wrap around to the next bit
                // Otherwise we continue shifting, allowing us to set each bit one-by-one
                if (shiftOffset == 7) {
                    shiftOffset = 0;
                    payloadIndex++;
                } else {
                    shiftOffset++;
                }
            }

            // Calculate checksums for IR signal
            final byte checksumA = (byte) (payload[0] + payload[1]);
            final byte checksumB = (byte) (payload[2] + payload[3]);
            logger.debug("Calculated checksums {} and {} for IR payload {}",
                ByteHelpers.toString(checksumA), ByteHelpers.toString(checksumB), ByteHelpers.toString(payload));

            // Silently abort if checksum does not match expected values
            if (checksumA != (byte) 0xFF || checksumB != (byte) 0xFF) {
                logger.debug("Skipping IR signal due to checksum mismatch");
                return;
            }

            // Attempt to map keycode to well-known key
            final byte keyCode = payload[2];
            final var key = Key.fromCode(keyCode);
            if (key == null) {
                logger.info("Ignoring unknown IR key code {}", ByteHelpers.toString(keyCode));
                return;
            }

            // Dispatch onKeyPressed handler with IR key if available
            final var handler = onKeyPressedHandler.get();
            if (handler != null) {
                logger.debug("Dispatching onKeyPressed event for IR key {}", key);
                handler.handle(key);
            } else {
                logger.debug("Skipping dispatch of IR key {} due to missing handler", key);
            }
        }

        /**
         * Helper class for storing a pulse which consist of a raising and falling edge
         */
        private final class Pulse {
            /**
             * Time it took in microseconds to get the raising edge for this pulse
             */
            private final long raisingEdge;
            /**
             * Time it took in microseconds to get the falling edge for this pulse
             */
            private final long fallingEdge;

            /**
             * Constructs a new pulse for the given times.
             *
             * @param raisingEdge Time of raising edge in microseconds
             * @param fallingEdge Time of falling edge in microseconds
             */
            public Pulse(long raisingEdge, long fallingEdge) {
                this.raisingEdge = raisingEdge;
                this.fallingEdge = fallingEdge;
            }

            /**
             * Returns either the raising or falling edge time in microseconds, based on which value is higher.
             *
             * @return Time in microseconds
             */
            public long getMaxEdgeTime() {
                return Math.max(raisingEdge, fallingEdge);
            }

            /**
             * Returns the time for the raising edge in microseconds.
             *
             * @return Time in microseconds
             */
            public long getRaisingEdge() {
                return raisingEdge;
            }

            /**
             * Returns the time for the falling edge in microseconds.
             *
             * @return Time in microseconds
             */
            public long getFallingEdge() {
                return fallingEdge;
            }

            @Override
            public String toString() {
                return "Pulse{" +
                    "raisingEdge=" + raisingEdge +
                    ", fallingEdge=" + fallingEdge +
                    '}';
            }
        }
    }

    /**
     * Enumeration which represents all known keycodes for the bundled CrowPi IR remote
     */
    public enum Key {
        CH_MINUS("CH-", 0x45),
        CH("CH", 0x46),
        CH_PLUS("CH+", 0x47),
        PREV("PREV", 0x44),
        NEXT("NEXT", 0x40),
        PLAY_PAUSE("PLAY/PAUSE", 0x43),
        VOL_MINUS("VOL-", 0x07),
        VOL_PLUS("VOL+", 0x15),
        EQ("EQ", 0x09),
        ZERO("0", 0x16),
        HUNDRED_PLUS("100+", 0x19),
        TWO_HUNDRED_PLUS("200+", 0x0D),
        ONE("1", 0x0C),
        TWO("2", 0x18),
        THREE("3", 0x5E),
        FOUR("4", 0x08),
        FIVE("5", 0x1C),
        SIX("6", 0x5A),
        SEVEN("7", 0x42),
        EIGHT("8", 0x52),
        NINE("9", 0x4A);

        private final byte code;
        private final String description;

        Key(String description, int code) {
            this(description, (byte) code);
        }

        Key(String description, byte code) {
            this.code = code;
            this.description = description;
        }

        /**
         * Returns the first key which matches the passed key code or null if no match is found.
         *
         * @param code Keycode as byte
         * @return Matched key if found or null if not found
         */
        public static Key fromCode(byte code) {
            for (final var key : Key.values()) {
                if (key.getCode() == code) {
                    return key;
                }
            }
            return null;
        }

        /**
         * Returns the keycode of the key.
         *
         * @return Keycode as byte
         */
        public byte getCode() {
            return code;
        }

        /**
         * Returns the description of the key.
         *
         * @return Human-readable key description matching the labels on the remote
         */
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return getDescription();
        }
    }
}
