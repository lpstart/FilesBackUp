package com.onlinebackup.client;

import java.io.File;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.onlinebackup.instruction.ACKCopyBlockInstruction;
import com.onlinebackup.instruction.AskNewBlockInstruction;
import com.onlinebackup.instruction.SynDirectoryInstruction;

import filesync.CopyBlockInstruction;
import filesync.EndUpdateInstruction;
import filesync.Instruction;
import filesync.NewBlockInstruction;
import filesync.StartUpdateInstruction;
import filesync.SynchronisedFile;

/**
 * thread that is used to backup this file on server
 * 
 * @author wlp
 *
 */
public class SynchronizeFileDoThread implements Runnable {

	private ClientSocketTool clientSocketTool = null;// tool used to communicate
														// with server
	private SynchronisedFile synFile = null;// the file need to backup

	/**
	 * constructor with the tool used to communicate with server and the file
	 * need to backup
	 * 
	 * @param clientSocketTool
	 *            the tool used to communicate with server
	 * @param synFile
	 *            the file need to backup
	 */
	public SynchronizeFileDoThread(ClientSocketTool clientSocketTool, SynchronisedFile synFile) {
		this.clientSocketTool = clientSocketTool;
		this.synFile = synFile;
	}

	/**
	 * get files in the directory
	 * 
	 * @param directory
	 *            certain directory
	 * @return the list of name of files in the directory
	 */
	private ArrayList<String> getFilesOfDirectory(String directory) {
		ArrayList<String> directoryFiles = new ArrayList<String>();
		// get name of files
		String[] files = new File(directory).list();
		// add name to list
		for (String file : files) {
			directoryFiles.add(file);
		}
		return directoryFiles;
	}

	/**
	 * parse JSONString and return the type attribute
	 * 
	 * @param msg
	 *            the JSONString
	 * @return the type
	 */
	private String getTypeFromJSONString(String msg) {
		JSONObject jsonObject = null;
		JSONParser parser = new JSONParser();
		try {
			jsonObject = (JSONObject) parser.parse(msg);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return (String) jsonObject.get("Type");
	}

	@Override
	public void run() {
		Instruction inst = null; // record the change instruction from synFile
									// need send to server
		String msg = null;// record the message to be send
		String msgFromServer = null;// record the respond from server
		File fileLocal = new File(synFile.getFilename());// get the local file
															// which is
															// represented by
															// synfile

		// judge the local file is existed or not
		// and judge there is a change instruction from synFile or not
		while (fileLocal.exists() && (inst = synFile.NextInstruction()) != null) {
			// if the local file is existed and there is a change instruction
			// from synFile

			// judge the type of the change instruction
			if (inst instanceof StartUpdateInstruction) {
				// if the change instruction is a StartUpdateInstruction
				// lock the tool and communicate with server until the
				// instruction from synFile belongs to an EndUpdateInstruction
				synchronized (clientSocketTool) {
					// record directory where the local file locate
					String directory = fileLocal.getParent();
					// get the files in directory, and create a synchronize
					// directory instruction
					msg = (new SynDirectoryInstruction(getFilesOfDirectory(directory))).ToJSON();
					// print in local screen the message to be send
					System.out.println("Send:" + msg);
					// send the message to server
					clientSocketTool.printMsg(msg);

					msgFromServer = clientSocketTool.readMsg();
					System.out.println("the message after update directory on server£º" + msgFromServer);

					// transform the StartUpdateInstruction to String, and
					// send it to server
					msg = inst.ToJSON();
					System.err.println("Sending: " + msg);
					clientSocketTool.printMsg(msg);

					// get the next change instruction from synFile
					while ((inst = synFile.NextInstruction()) != null) {
						// transform the instruction to String, and send it
						// to server
						msg = inst.ToJSON();
						System.err.println("Sending: " + msg);
						clientSocketTool.printMsg(msg);

						if (inst instanceof CopyBlockInstruction) {
							// if the instruction belongs to
							// CopyBlockInstruction
							// wait a respond from server

							msgFromServer = clientSocketTool.readMsg();

							String type = getTypeFromJSONString(msgFromServer);

							// judge the type of respond from server
							if (type.equals(new ACKCopyBlockInstruction().Type())) {
								// if the type is ACKCopyBlockInstruction

							} else if (type.equals(new AskNewBlockInstruction().Type())) {
								// if the type is AskNewBlockInstruction
								// create a NewBlockInstruction based on inst
								Instruction upgraded = new NewBlockInstruction((CopyBlockInstruction) inst);
								// transform the instruction to String, and send
								// it
								// to server
								msg = upgraded.ToJSON();
								System.err.println("Sending: " + msg);
								clientSocketTool.printMsg(msg);
							}
						} else if (inst instanceof EndUpdateInstruction) {
							// if the change instruction belongs to
							// EndUpdateInstruction, then break
							clientSocketTool.notify();
							break;
						}
					} // end while ((inst = synFile.NextInstruction()) != null)
				} // end synchronized (clientSocketTool)
			} // end if (inst instanceof StartUpdateInstruction)
		}
	}

}
