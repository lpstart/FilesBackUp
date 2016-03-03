package com.onlinebackup.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.onlinebackup.instruction.ACKCopyBlockInstruction;
import com.onlinebackup.instruction.AskNewBlockInstruction;
import com.onlinebackup.instruction.SynDirectoryInstruction;

import filesync.BlockUnavailableException;
import filesync.CopyBlockInstruction;
import filesync.EndUpdateInstruction;
import filesync.Instruction;
import filesync.InstructionFactory;
import filesync.NewBlockInstruction;
import filesync.StartUpdateInstruction;
import filesync.SynchronisedFile;

/**
 * 
 * @author wlp
 *
 */
public class BackUpThread implements Runnable {
	private Socket socket = null;
	private String directory = null;
	private Scanner scanner = null;
	private PrintStream printStream = null;

	// the map which the key means the file name and the value means
	// thesynchroniseFile of this file
	private Map<String, SynchronisedFile> filesMap = null;

	public BackUpThread(Socket socket, String directory) throws IOException {
		this.socket = socket;
		this.scanner = new Scanner(socket.getInputStream());
		this.printStream = new PrintStream(socket.getOutputStream());
		this.directory = directory;
		initializeLocalFiles();
	}

	/**
	 * recreate the directory which will backup files,and initialize the map
	 * 
	 * @throws IOException
	 */
	private void initializeLocalFiles() {
		filesMap = new HashMap<String, SynchronisedFile>();
		File file = new File(directory);
		if (!file.exists()) {
			System.out.println(file.getName() + " create " + file.mkdirs());
		} else {
			deleteDir(file);
			System.out.println(file.getName() + " create " + file.mkdirs());
		}
	}

	/**
	 * remove the directory
	 * 
	 * @param dir
	 */
	private void deleteDir(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File file : files)
				if (file.isFile())
					file.delete();
				else
					deleteDir(file);
			dir.delete();
		}
	}

	/**
	 * send the message to client
	 * 
	 * @param msg
	 *            the message need send
	 */
	private void printMsg(String msg) {
		try {
			printStream.write((msg + "\n").getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		printStream.flush();
	}

	/**
	 * parse JSONString and return the field attribute
	 * 
	 * @param msg
	 *            the JSONString
	 * @param field
	 *            the name of field
	 * @return the value of field in JSONString
	 */
	private String getFromJSONString(String msg, String field) {
		JSONObject jsonObject = null;
		JSONParser parser = new JSONParser();
		try {
			jsonObject = (JSONObject) parser.parse(msg);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return (String) jsonObject.get(field);
	}

	/**
	 * process instruction in currentSynFile
	 * 
	 * @param currentSynFile
	 *            the currentSynFile processes instruction
	 * @param inst
	 *            the instruction need process
	 */
	private void processInstruction(SynchronisedFile currentSynFile, Instruction inst) {
		try {
			currentSynFile.ProcessInstruction(inst);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BlockUnavailableException e) {
			// we needn't to process that,because the instruction is a
			// StartUpdateInstruction or EndUpdateInstruction,
			// and there won't be a exception.
		}
	}

	@Override
	public void run() {
		SynchronisedFile currentSyn = null;// record the current synchronizeFile
		// to process instruction
		InstructionFactory instFactory = new InstructionFactory();

		String msg = null;// record the message from client
		while ((msg = scanner.nextLine()) != null) {
			String type = getFromJSONString(msg, "Type");

			// if the msg is a update directory Instruction
			if (type.equals(new SynDirectoryInstruction().Type())) {
				// get the files name from msg
				String filesStr = getFromJSONString(msg, "Files");
				List<String> filesList = Arrays.asList(filesStr.split(":"));
				// update the map and server directory based on files list

				// add all new files
				for (String file : filesList) {

					if (this.filesMap.containsKey(file)) {
						// if the file is contained in map

					} else {
						// if the file is not contained in map
						String fileTempName = directory + file;
						// create a file
						File fileTemp = new File(fileTempName);
						try {
							fileTemp.createNewFile();

							// and a key-value to map
							this.filesMap.put(file, new SynchronisedFile(fileTempName));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				// delete the file not in the filesList

				// find the file name which is contained in map and not
				// contained in filesList,
				// and add it to deletFilesList
				ArrayList<String> deletFilesList = new ArrayList<String>();
				Set<String> filesServer = filesMap.keySet();
				for (String file : filesServer) {
					if (!filesList.contains(file)) {
						deletFilesList.add(file);
					}
				}
				// delete the file and remove it from map
				for (String file : deletFilesList) {
					String deletFile = directory + file;
					System.err.println("server: delet file " + deletFile);
					System.out.println("delete " + (new File(deletFile)).delete());
					filesMap.remove(file);
				}
				// respond a message to inform client finishing of update
				// directory
				ACKCopyBlockInstruction ack = new ACKCopyBlockInstruction();
				printMsg(ack.ToJSON());

			} else {
				// get the instruction form message
				Instruction inst = instFactory.FromJSON(msg);

				if (inst instanceof StartUpdateInstruction) {
					// if the instruction is a startupInstruction,
					// find the correspond synchronizeFile to process
					// instruction based on fileName

					String fileName = ((StartUpdateInstruction) inst).getFileName();
					System.err.println("Server:start update file " + fileName);

					// get the synchronizeFile from map based on fileName
					currentSyn = filesMap.get(fileName);
					// process the startupInstruction
					processInstruction(currentSyn, inst);

				} else if (inst instanceof CopyBlockInstruction) {
					// if the instruction is a copyblockInstruction,
					try {
						currentSyn.ProcessInstruction(inst);

						// respond a ACKCopyBlockInstruction means the
						// CopyBlockInstruction is OK
						ACKCopyBlockInstruction ack = new ACKCopyBlockInstruction();
						printMsg(ack.ToJSON());
					} catch (IOException e) {
						e.printStackTrace();
					} catch (BlockUnavailableException e) {
						// respond a AskNewBlockInstruction means the
						// CopyBlockInstruction can't use
						// and ask a new block
						AskNewBlockInstruction askNewBlock = new AskNewBlockInstruction();
						System.err.println("Server:request for new block " + askNewBlock.ToJSON());
						printMsg(askNewBlock.ToJSON());
					}

				} else if (inst instanceof NewBlockInstruction) {
					// if the instruction is a NewBlockInstruction
					System.err.println("Server: got new block " + inst.ToJSON());

					// process the startupInstruction
					processInstruction(currentSyn, inst);

				} else if (inst instanceof EndUpdateInstruction) {
					// if the instruction is a endInstruction

					// process the startupInstruction
					processInstruction(currentSyn, inst);

					System.err.println("Server:finish updating " + currentSyn.getFilename());

					// set the currentSyn null
					currentSyn = null;
				}
			}
		}
	}

}
