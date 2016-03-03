package com.onlinebackup.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * the tool of inputStream and outputStream associating with server
 * 
 * @author wlp
 *
 */
public class ClientSocketTool {
	private Socket socket = null;
	private Scanner sc = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;
	private PrintStream ps = null;

	/**
	 * constructor with a socket parameter
	 * 
	 * @param socket
	 *            the connection between client and server
	 * @throws IOException
	 */
	public ClientSocketTool(Socket socket) throws IOException {
		this.socket = socket;

		// initialize the inputStream and outputStream of this tool which we
		// will use to get or send messages
		this.inputStream = socket.getInputStream();
		this.outputStream = socket.getOutputStream();
	}

	/**
	 * get this tool's inputStream
	 * 
	 * @return this tool's inputStream
	 * @throws IOException
	 */
	public InputStream getInputStream() {
		return this.inputStream;
	}

	/**
	 * get this tool's outputStream
	 * 
	 * @return this tool's outputStream
	 * @throws IOException
	 */
	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	/**
	 * read a message from the inputStream.(this is a synchronized method).
	 * 
	 * @return the message from inputStream which maybe meaning server
	 * @throws IOException
	 */
	public synchronized String readMsg() {
		InputStream inputStream = getInputStream();
		// read a message from inputStream
		sc = new Scanner(inputStream);
		return sc.nextLine();
	}

	/**
	 * send a message to outputSteam.(this is a synchronized method).
	 * 
	 * @param msg
	 *            the message need to send to outputStream which maybe meaning
	 *            server
	 * @throws IOException
	 */
	public synchronized void printMsg(String msg) {
		OutputStream outputStream = getOutputStream();
		// send a message out
		ps = new PrintStream(outputStream);
		try {
			ps.write((msg + "\n").getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		ps.flush();
	}
}
