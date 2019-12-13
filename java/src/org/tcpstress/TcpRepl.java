package org.tcpstress;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Var;

public class TcpRepl {
	private static final String CLOJURE_CORE = "clojure.core";
	private final IFn readString;
	private final IFn eval;
	private final ServerSocket serverSocket;
	private Var ns;
	public TcpRepl() throws IOException {
		readString = Clojure.var(CLOJURE_CORE, "read-string");
		eval = Clojure.var(CLOJURE_CORE, "eval");
		ns = (Var) Clojure.var(CLOJURE_CORE, "*ns*");
		serverSocket = startServer(7865, 0); 
		System.out.println(String.format("REPL listening on %s", serverSocket.getLocalPort()));
	}
	
	private ServerSocket startServer(final int port, final int n) throws IOException {
		
		try {
			return new ServerSocket(port + n);
		} catch (final BindException e) {
			if (n < 100) return startServer(port, n + 1);
			throw e;
		}
	}

	public void start() {
		new Thread(() -> {
			while (true) {
				try {
					startConnection(serverSocket.accept());
				} catch (final IOException e) {
					e.printStackTrace();
				}

			}
		}).start();
	}
	
	
	private void startConnection(final Socket socket) {
		new Thread(() -> {
			try {
				final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				while (true) {
					try {
						out.writeChars(String.format("%s=>", ns.get()));
						final String line = in.readLine();
						final Object res = eval.invoke(readString.invoke(line));
						if (res != null) {
							out.writeChars(res.toString());
						}
					} catch (final Exception e) {
						out.writeChars(e.getMessage());
					} finally {
						out.writeChars("\n");
					}
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public static void main(final String[] args) throws IOException {
		new TcpRepl().start();
	}
	
}
