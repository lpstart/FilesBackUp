package com.onlinebackup.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;

import filesync.SynchronisedFile;

/**
 * client class
 * 
 * @author wlp
 *
 */
public class Client {

	private InetAddress serverAddr = null;
	private int port = 4444;
	private Socket socket = null;
	private String directory = null;
	private ClientSocketTool clientSocketTool = null;

	public Client() throws UnknownHostException, IOException {
		directory = "G:/ddddd/";
		serverAddr = InetAddress.getByAddress("127.0.0.1".getBytes());
		socket = new Socket(serverAddr, port);
		clientSocketTool = new ClientSocketTool(socket);
	}

	/**
	 * constructor with some necessary parameters
	 * 
	 * @param directory
	 *            local directory need to be backup
	 * @param serverAddr
	 *            IP address of server
	 * @param port
	 *            port of server
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public Client(String directory, InetAddress serverAddr, int port) throws UnknownHostException, IOException {
		this.directory = directory.endsWith(File.separator) ? directory : directory + File.separator;
		this.serverAddr = serverAddr;
		this.port = port;

		// connect to server
		socket = new Socket(serverAddr, port);
		// initialize the tool used to communicate with server
		clientSocketTool = new ClientSocketTool(socket);
	}

	/**
	 * start to run the process of synchronizing directory
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void synchronizeFile() throws IOException, InterruptedException {
		// get all files' name which are need to be synchronized
		ArrayList<String> directoryFiles = getFilesOfDirectory();

		// synchronize all files
		for (String file : directoryFiles) {
			SynchronisedFile synFile = new SynchronisedFile(directory + File.separator + file);

			// run a thread to check the change of file
			Thread checkChangeThread = new Thread(new SynchronizeFileThread(synFile));
			checkChangeThread.setDaemon(true);// set the thread daemon
			checkChangeThread.start();

			// run a thread to backup this file on server
			Thread backupThread = new Thread(new SynchronizeFileDoThread(clientSocketTool, synFile));
			backupThread.setDaemon(true);// set the thread daemon
			backupThread.start();
		}
		// add a listener to directory: when create a new file,create correspond
		// thread.
		watchService();
	}

	/**
	 * add a listener to directory: when create a new file,create correspond
	 * thread.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void watchService() throws IOException, InterruptedException {
		WatchService watchService = FileSystems.getDefault().newWatchService();
		Paths.get(directory).register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

		while (true) {
			WatchKey key = watchService.take();
			for (WatchEvent<?> event : key.pollEvents()) {

				SynchronisedFile synFile = new SynchronisedFile(directory + File.separator + event.context());

				// run a thread to check the change of file
				Thread checkChangeThread = new Thread(new SynchronizeFileThread(synFile));
				checkChangeThread.setDaemon(true);// set the thread daemon
				checkChangeThread.start();

				// run a thread to backup this file on server
				Thread backupThread = new Thread(new SynchronizeFileDoThread(clientSocketTool, synFile));
				backupThread.setDaemon(true);// set the thread daemon
				backupThread.start();
			}
			boolean valid = key.reset();
			if (!valid)
				break;
		}
	}

	/**
	 * get files in the directory
	 * 
	 * @return the list of name of files in the directory
	 */
	private ArrayList<String> getFilesOfDirectory() {

		ArrayList<String> directoryFiles = new ArrayList<String>();
		// get name of files
		String[] files = new File(directory).list();
		// add name to list
		for (String file : files) {
			directoryFiles.add(file);
		}
		return directoryFiles;
	}

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		String folder_path = null;
		String hostName = null;
		int port = 4444;
		String portStr = null;

		// process args
		for (int i = 0, len = args.length; i < len; i++) {
			if (args[i].equalsIgnoreCase("-f")) {
				folder_path = args[i + 1];
			}
			if (args[i].equalsIgnoreCase("-h")) {
				hostName = args[i + 1];
			}
			if (args[i].equalsIgnoreCase("-p")) {
				portStr = args[i + 1];
			}
		}

		if (folder_path == null || hostName == null) {
			System.err.println("useage:java -jar syncclient.jar -f folder-path -h hostname [-p port]");
			System.exit(1);
		} else {

			if (portStr != null) {
				port = Integer.parseInt(portStr);
			}

			InetAddress hostAddress = InetAddress.getByName(hostName);
			new Client(folder_path, hostAddress, port).synchronizeFile();

		}
		// ensure the main thread is running, and then daemons will run
		while (true) {

		}
	}

}
