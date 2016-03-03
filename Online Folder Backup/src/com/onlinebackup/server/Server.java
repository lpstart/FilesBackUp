package com.onlinebackup.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * server
 * 
 * @author wlp
 *
 */
public class Server {
	private ServerSocket serverSocket = null;
	private int port = 4444;
	private String directory = null;

	public Server() throws IOException {
		this.directory = "G:/fffff/";
	}

	/**
	 * constructor with port and directory parameter
	 * 
	 * @param port
	 *            the port to listen
	 * @param directory
	 *            the directory to backup files
	 * @throws IOException
	 */
	public Server(String directory, int port) {
		this.port = port;
		this.directory = directory.endsWith(File.separator) ? directory : directory + File.separator;
	}

	/**
	 * start to listen the port and process the accept
	 * 
	 * @throws IOException
	 */
	public void start() throws IOException {
		serverSocket = new ServerSocket(port);
		// ensure the main thread is running, and then daemons will run
		while (true) {
			Socket socket = serverSocket.accept();
			// backup client directory
			Thread backupThread = new Thread(new BackUpThread(socket, directory));
			backupThread.setDaemon(true);
			backupThread.start();
		}
	}

	public static void main(String[] args) throws IOException {
		String folder_path = null;
		int port = 4444;
		String portStr = null;

		// process args
		for (int i = 0, len = args.length; i < len; i++) {
			if (args[i].equalsIgnoreCase("-f")) {
				folder_path = args[i + 1];
			}
			if (args[i].equalsIgnoreCase("-p")) {
				portStr = args[i + 1];
			}
		}

		if (folder_path == null) {
			System.err.println("usage: java -jar syncserver.jar -f folder-path [-p port]");
			System.exit(1);
		} else {
			if (portStr != null) {
				port = Integer.parseInt(portStr);
			}
			new Server(folder_path, port).start();
		}

	}

}
