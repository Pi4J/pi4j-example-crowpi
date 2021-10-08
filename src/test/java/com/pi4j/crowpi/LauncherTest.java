package com.pi4j.crowpi;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pi4j.context.Context;
import com.pi4j.crowpi.helpers.InstanceAlreadyRunningException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LauncherTest {
    protected static final List<Application> TEST_APPLICATIONS = List.of(new AppA(), new AppB());

    protected static boolean EXECUTED_APP_A, EXECUTED_APP_B;
    protected Launcher launcher;

    @BeforeEach
    public void setUp() throws Exception {
        this.launcher = new Launcher(TEST_APPLICATIONS);

        // Reset verification variables for checking app execution
        EXECUTED_APP_A = false;
        EXECUTED_APP_B = false;
    }

    @Test
    public void testExecuteManual() {
        // given
        InputStream stdinOriginal = System.in;
        ByteArrayInputStream stdin = new ByteArrayInputStream("invalid-choice\n2".getBytes());
        System.setIn(stdin);

        // when
        final int rc;
        try {
            rc = launcher.execute(new String[]{});
        } finally {
            System.setIn(stdinOriginal);
        }

        // then
        assertEquals(rc, 0);
        assertTrue(EXECUTED_APP_A || EXECUTED_APP_B); // either one of two apps must have been executed
        assertTrue(EXECUTED_APP_A ^ EXECUTED_APP_B); // only one of two apps must have been executed
    }

    @Test
    public void testExecuteDirectA() {
        // when
        final var rc = launcher.execute(new String[]{"AppA"});

        // then
        assertEquals(rc, 0);
        assertTrue(EXECUTED_APP_A);
        assertFalse(EXECUTED_APP_B);
    }

    @Test
    public void testExecuteDirectB() {
        // when
        final var rc = launcher.execute(new String[]{"AppB"});

        // then
        assertEquals(0, rc);
        assertFalse(EXECUTED_APP_A);
        assertTrue(EXECUTED_APP_B);
    }

    @Test
    public void testExecuteUnknown() {
        // when
        final var rc = launcher.execute(new String[]{"AppC"});

        // then
        assertNotEquals(0, rc);
        assertFalse(EXECUTED_APP_A);
        assertFalse(EXECUTED_APP_B);
    }

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	@Test
	public void shouldNotStartTwice() throws ExecutionException, InterruptedException {
		// given
		final WaitApp waitApp = new WaitApp();
		Launcher launcher = new Launcher(List.of(waitApp));

		// when
		ExecutorService executorService = Executors.newCachedThreadPool();
		var future = executorService.submit(() -> launcher.execute(new String[] { "WaitApp" }));
		Thread.sleep(50L);

		// then 1
		assertThrows(InstanceAlreadyRunningException.class, () -> launcher.execute(new String[] { "AppB" }));

		// stop execution
		synchronized (waitApp) {
			System.out.println("Notifying  " + waitApp);
			waitApp.notifyAll();
		}
		future.get();

		// then 2
		launcher.execute(new String[] { "AppB" });

		// cleanup
		executorService.shutdownNow();
	}

    private static final class AppA implements Application {
        @Override
        public void execute(Context pi4j) {
            LauncherTest.EXECUTED_APP_A = true;
        }
    }

    private static final class AppB implements Application {
        @Override
        public void execute(Context pi4j) {
            LauncherTest.EXECUTED_APP_B = true;
        }
    }

	private static final class WaitApp implements Application {
		@Override
		public void execute(Context pi4j) {
			synchronized (this) {
				try {
					System.out.println("Waiting on " + this);
					this.wait();
				} catch (InterruptedException e) {
					System.out.println("Interrupted, ending thread.");
				}
			}
		}
	}
}
