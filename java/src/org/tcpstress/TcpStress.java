package org.tcpstress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class TcpStress {
	private static final String HOST = "-host";
	private static final String PORT = "-port";
	private static final String SIZE = "-size";
	private static final String WAIT_TIME = "-waittime";
	private static final String filename = "-file";
	private static IFn readString;
	private static IFn mapFn;
	private static IFn byteFn;
	private static IFn byteArrayFn;

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
		final AtomicBoolean rndRunning = new AtomicBoolean(true);
		new Thread(() ->  {
			try {
				while (!done.get()) {
					if (rndRunning.get()) {
						final int size = Math.abs(rnd.nextInt()) % maxSize;
						final byte[] data = new byte[size];
						rnd.nextBytes(data);
						try {
							out.write(data);
						} catch (final IOException e) {
							System.out.println(e.getMessage());
						}
					} 
					Thread.sleep(waitTimer);

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
		System.out.println("Enter exit to close application");
		System.out.println("Enter pause to stop sending random bytes");
		System.out.println("Enter resume to continue sending random bytes");
		System.out.println("Enter byte array to send i.e. [1 2 3 4] or a string \"a string\"");
		try {
			while (true) {
				final String line = stdIn.readLine();
				switch (line) {
				case "exit":
					done.set(true);
					cl.await(5, TimeUnit.SECONDS);
					return;
				case "pause":
					rndRunning.set(false);
					break;
				case "resume":
					rndRunning.set(true);
					break;
				default:
					initClj();
					try {
						out.write(bytesOf(readString.invoke(line)));
					} catch (IllegalArgumentException e) {
						System.out.println(String.format("Could not parse"));
					}
				}
			}
		} finally {
		}
			
		
		
	}
	private static byte[] bytesOf(Object obj) {
		if (obj instanceof List) {
			final List l = (List)obj;
			return (byte[]) byteArrayFn.invoke(mapFn.invoke(byteFn, l));
		}
		if (obj instanceof String) {
			return ((String)obj).getBytes();
		}
		
		throw new IllegalArgumentException(obj.getClass().getName());
	}
	private static final String CLOJURE_CORE = "clojure.core";

	private static void initClj() {
		if (readString == null) {
			readString = Clojure.var(CLOJURE_CORE, "read-string");
			mapFn = Clojure.var(CLOJURE_CORE, "map");
			byteFn = Clojure.var(CLOJURE_CORE, "byte");
			byteArrayFn = Clojure.var(CLOJURE_CORE, "byte-array");
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
