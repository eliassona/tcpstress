package org.tcpstress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpStress {
	private static final String HOST = "-host";
	private static final String PORT = "-port";
	private static final String SIZE = "-size";
	private static final String WAIT_TIME = "-waittime";

	public static void main(final String[] args) throws IOException, InterruptedException {
		if (args.length == 1 && args[0].equals("help")) {
			printHelp();
			return;
		}
		final Map<String, Object> argMap = argMapOf(args);
		final Socket socket = new Socket((String) argMap.get(HOST), (int)argMap.get(PORT));
		final OutputStream out = socket.getOutputStream();
		final CountDownLatch cl = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		final Random rnd = new Random();
		int waitTimer = (int) argMap.get(WAIT_TIME);
		final int maxSize = (int) argMap.get(SIZE);
		new Thread(() ->  {
			try {
				while (!done.get()) {
					final int size = Math.abs(rnd.nextInt()) % maxSize;
					final byte[] data = new byte[size];
					rnd.nextBytes(data);
					try {
						out.write(data);
						Thread.sleep(waitTimer);
					} catch (final IOException e) {
						System.out.println(e.getMessage());
					}
				}
			} catch (InterruptedException e) {
				
			} finally {
				try {
					out.close();
					socket.close();
				} catch (final IOException e) {
					System.out.println(e.getMessage());
				}
				cl.countDown();
			}
		}).start();
		
		final BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			if (stdIn.readLine().equals("exit")) {
				done.set(true);
				cl.await(5, TimeUnit.SECONDS);
				return;
			}
		}
		
		
	}

	private static void printHelp() {
		System.out.println("Uility that sends random bytes to tcp server");
		System.out.println("Optional arguments");
		System.out.println(String.format("%s, default is localhost", HOST));
		System.out.println(String.format("%s, default is 3210", PORT));
		System.out.println(String.format("%s, maximum size of the data to be sent each time, default is 1000 bytes", SIZE));
		System.out.println(String.format("%s, wait time between sending data in ms", WAIT_TIME));
	}

	private static Map<String, Object> argMapOf(final String[] args) {
		if (args.length % 2 != 0) throw new IllegalArgumentException();
		final Map<String, Object> m = new HashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			m.put(args[i], args[i+1]);
		}
		if (m.get(HOST) == null) m.put(HOST, "localhost");
		if (m.get(PORT) == null) {
			m.put(PORT, 3210);
		} else {
			m.put(PORT, Integer.parseInt((String)m.get(PORT)));
		}
		if (m.get(SIZE) == null) {
			m.put(SIZE, 1000);
		} else {
			m.put(SIZE, Integer.parseInt((String)m.get(SIZE)));
		}
		if (m.get(WAIT_TIME) == null) {
			m.put(WAIT_TIME, 10);
		} else {
			m.put(WAIT_TIME, Integer.parseInt((String)m.get(WAIT_TIME)));
		}
		return m;
	}
	
}
