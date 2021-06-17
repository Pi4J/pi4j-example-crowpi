package com.pi4j.crowpi.components;

import com.pi4j.crowpi.ComponentTest;
import com.pi4j.crowpi.components.IrReceiverComponent.Key;
import com.pi4j.crowpi.components.IrReceiverComponent.PollerProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class IrReceiverComponentTest extends ComponentTest {
    protected IrReceiverComponent irReceiver;

    @BeforeEach
    void setUp() {
        this.irReceiver = new IrReceiverComponent();
        this.irReceiver.pollerProcessFactory = MockPollerProcess::new;
    }

    @AfterEach
    void tearDown() {
        this.irReceiver.onKeyPressed(null);
    }

    @Test
    void testAutoStartPoller() {
        // when
        irReceiver.onKeyPressed(key -> {
        });

        // then
        assertNotNull(irReceiver.getPollerManagerThread());
    }

    @Test
    void testAutoStopPoller() {
        // given
        irReceiver.onKeyPressed(key -> {
        });
        final var thread = irReceiver.getPollerManagerThread();

        // when
        irReceiver.onKeyPressed(null);

        // then
        assertFalse(thread.isAlive());
        assertNull(irReceiver.getPollerManagerThread());
    }

    @Test
    void testAutoRestartPoller() throws InterruptedException {
        // given
        final var oldPollerLatch = new CountDownLatch(1);
        irReceiver.pollerProcessFactory = () -> new MockPollerProcess(oldPollerLatch);
        irReceiver.onKeyPressed(key -> {
        });

        final var pollerManager = irReceiver.getPollerManager();
        assertTrue(oldPollerLatch.await(250, TimeUnit.MILLISECONDS));
        final var oldPollerProcess = pollerManager != null ? pollerManager.getPollerProcess() : null;

        final var newPollerLatch = new CountDownLatch(1);
        irReceiver.pollerProcessFactory = () -> new MockPollerProcess(newPollerLatch);

        // when
        if (oldPollerProcess != null) {
            oldPollerProcess.destroy();
        }

        // then
        assertNotNull(pollerManager);
        assertNotNull(oldPollerProcess);
        assertTrue(newPollerLatch.await(1, TimeUnit.SECONDS));

        final var newPollerProcess = pollerManager.getPollerProcess();
        assertNotEquals(oldPollerProcess, newPollerProcess);
        assertFalse(oldPollerProcess.isAlive());
        assertTrue(newPollerProcess.isAlive());
    }

    @Test
    void testPlayPauseSignal() throws InterruptedException {
        // given
        irReceiver.pollerProcessFactory = () -> new MockPollerProcess(IR_SIGNAL_PLAY_PAUSE);
        final var latch = new CountDownLatch(1);
        final var detectedKey = new AtomicReference<Key>();

        // when
        irReceiver.onKeyPressed(key -> {
            detectedKey.set(key);
            latch.countDown();
        });

        // then
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(Key.PLAY_PAUSE, detectedKey.get());
    }

    @Test
    void testInvalidSignal() throws InterruptedException, IOException {
        // given
        final var pollerProcess = new MockPollerProcess(IR_SIGNAL_INVALID);
        irReceiver.pollerProcessFactory = () -> pollerProcess;
        final var latch = new CountDownLatch(1);

        // when
        irReceiver.onKeyPressed(key -> latch.countDown());

        // then
        assertFalse(latch.await(1, TimeUnit.SECONDS));
        assertEquals(0, pollerProcess.getInputStream().available());
    }

    @Test
    void testIncompleteSignal() throws InterruptedException, IOException {
        // given
        final var pollerProcess = new MockPollerProcess(IR_SIGNAL_INCOMPLETE);
        irReceiver.pollerProcessFactory = () -> pollerProcess;
        final var latch = new CountDownLatch(1);

        // when
        irReceiver.onKeyPressed(key -> latch.countDown());

        // then
        assertFalse(latch.await(1, TimeUnit.SECONDS));
        assertEquals(0, pollerProcess.getInputStream().available());
    }

    private static final class MockPollerProcess implements PollerProcess {
        private final ByteArrayInputStream inputStream;
        private final CountDownLatch isReadyLatch;
        private boolean isAlive = true;

        public MockPollerProcess() {
            this("", null);
        }

        public MockPollerProcess(String stdout) {
            this(stdout, null);
        }

        public MockPollerProcess(CountDownLatch isReadyLatch) {
            this("", isReadyLatch);
        }

        public MockPollerProcess(String stdout, CountDownLatch isReadyLatch) {
            this.inputStream = new ByteArrayInputStream(stdout.getBytes());
            this.isReadyLatch = isReadyLatch;
        }

        @Override
        public InputStream getInputStream() {
            if (isReadyLatch != null) {
                isReadyLatch.countDown();
            }
            return inputStream;
        }

        @Override
        public boolean isAlive() {
            return isAlive;
        }

        @Override
        public void destroy() {
            isAlive = false;
        }
    }

    /**
     * Captured output of "mode2 --device /dev/lirc0 --driver default" when pressing the PLAY/PAUSE button on the CrowPi remote.
     * To be used in tests for simulating a valid IR signal.
     */
    private static final String IR_SIGNAL_PLAY_PAUSE = "Using driver default on device /dev/lirc0\n" +
        "Trying device: /dev/lirc0\n" +
        "Using device: /dev/lirc0\n" +
        "space 16777215\n" +
        "pulse 9064\n" +
        "space 4443\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 620\n" +
        "space 534\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 617\n" +
        "space 537\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 619\n" +
        "space 1616\n" +
        "pulse 613\n" +
        "space 1614\n" +
        "pulse 616\n" +
        "space 1612\n" +
        "pulse 618\n" +
        "space 1611\n" +
        "pulse 620\n" +
        "space 1610\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 617\n" +
        "space 540\n" +
        "pulse 614\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1613\n" +
        "pulse 617\n" +
        "space 1611\n" +
        "pulse 619\n" +
        "space 535\n" +
        "pulse 617\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 39301\n" +
        "pulse 9074\n" +
        "space 2186\n" +
        "pulse 612\n" +
        "pulse 140477";

    /**
     * Hand-crafted IR signal with checksum mismatch, based on play/pause button with one space modified
     */
    private static final String IR_SIGNAL_INVALID = "Using driver default on device /dev/lirc0\n" +
        "Trying device: /dev/lirc0\n" +
        "Using device: /dev/lirc0\n" +
        "space 16777215\n" +
        "pulse 9064\n" +
        "space 4443\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 620\n" +
        "space 534\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 617\n" +
        "space 537\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 619\n" +
        "space 1616\n" +
        "pulse 613\n" +
        "space 1614\n" +
        "pulse 616\n" +
        "space 1612\n" +
        "pulse 618\n" +
        "space 1611\n" +
        "pulse 620\n" +
        "space 1610\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 617\n" +
        "space 540\n" +
        "pulse 614\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1613\n" +
        "pulse 617\n" +
        "space 1611\n" +
        "pulse 619\n" +
        "space 535\n" +
        "pulse 617\n" +
        "space 540\n" + // this space has been shortened to cause a checksum mismatch
        "pulse 619\n" +
        "space 39301\n" +
        "pulse 9074\n" +
        "space 2186\n" +
        "pulse 612\n" +
        "pulse 140477";

    /**
     * Hand-crafted IR signal with checksum mismatch, based on play/pause button with one pulse and space missing
     */
    private static final String IR_SIGNAL_INCOMPLETE = "Using driver default on device /dev/lirc0\n" +
        "Trying device: /dev/lirc0\n" +
        "Using device: /dev/lirc0\n" +
        "space 16777215\n" +
        "pulse 9064\n" +
        "space 4443\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 620\n" +
        "space 534\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 617\n" +
        "space 537\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 618\n" +
        "space 537\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 619\n" +
        "space 1616\n" +
        "pulse 613\n" +
        "space 1614\n" +
        "pulse 616\n" +
        "space 1612\n" +
        "pulse 618\n" +
        "space 1611\n" +
        "pulse 620\n" +
        "space 1610\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 536\n" +
        "pulse 619\n" +
        "space 536\n" +
        "pulse 617\n" +
        "space 540\n" +
        "pulse 614\n" +
        "space 1612\n" +
        "pulse 619\n" +
        "space 1611\n" +
        "pulse 618\n" +
        "space 1613\n" +
        "pulse 617\n" +
        "space 1611\n" +
        "pulse 619\n" +
        "space 535\n" +
        // here a pulse and space was removed
        "pulse 619\n" +
        "space 39301\n" +
        "pulse 9074\n" +
        "space 2186\n" +
        "pulse 612\n" +
        "pulse 140477";
}
