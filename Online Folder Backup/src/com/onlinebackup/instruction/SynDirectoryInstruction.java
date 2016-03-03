package com.onlinebackup.instruction;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import filesync.Instruction;

/**
 * this instruction is used to delivering the directory files
 * 
 * @author wlp
 *
 */
public class SynDirectoryInstruction extends Instruction {
	private List<String> directoryFiles = null;

	public SynDirectoryInstruction(ArrayList<String> directoryFiles) {
		this.directoryFiles = directoryFiles;
	}

	public SynDirectoryInstruction() {
	}

	public List<String> getDirectoryFiles() {
		return directoryFiles;
	}

	@Override
	public String Type() {
		return "SynDirectory";
	}

	@SuppressWarnings("unchecked")
	@Override
	public String ToJSON() {
		StringBuilder files = new StringBuilder();
		for (int i = 0, len = directoryFiles.size(); i < len; i++) {
			files.append(directoryFiles.get(i) + ":");
		}
		files.deleteCharAt(files.length() - 1);
		JSONObject obj = new JSONObject();
		obj.put("Files", files.toString());
		obj.put("Type", Type());
		return obj.toJSONString();
	}

	@Override
	public void FromJSON(String jst) {
		JSONObject obj = null;
		directoryFiles = new ArrayList<String>();
		try {
			obj = (JSONObject) parser.parse(jst);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (obj != null) {
			String[] files = ((String) obj.get("Files")).split(":");
			for (int i = 0; i < files.length; i++) {
				String file = files[i];
				directoryFiles.add(file);
			}
		}
	}
}
