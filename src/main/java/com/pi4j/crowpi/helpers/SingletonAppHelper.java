package com.pi4j.crowpi.helpers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

public class SingletonAppHelper {

	private static final int PORT = 65000;  // random large port number
	private static volatile ServerSocket s;

	public static synchronized void initialize() {
		try {
			s = new ServerSocket(PORT, 1, InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			// shouldn't happen for localhost
			System.err.println("Unexpected UnknownHostException: " + e.getMessage());
			throw new IllegalStateException("Unexpected UnknownHostException: " + e.getMessage(), e);
		} catch (IOException e) {
			// port taken, so app is already running
			System.err.println(
					"Could not create socket on port " + PORT + ", another instance is probably already running.");
			throw new InstanceAlreadyRunningException(
					"Could not create socket on port " + PORT + ", another instance is probably already running.", e);
		}
	}

	public static synchronized void close() {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {
				System.err.println("Closing semaphore threw exception: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
