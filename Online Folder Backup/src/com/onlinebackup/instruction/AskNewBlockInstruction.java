package com.onlinebackup.instruction;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;

import filesync.Instruction;

/**
 * this instruction means the server need a new Block
 * 
 * @author wlp
 *
 */

public class AskNewBlockInstruction extends Instruction {
	private String fileName = null;
	private String hashCode = null;

	public AskNewBlockInstruction() {
		this.fileName = "";
		this.hashCode = "";
	}

	public AskNewBlockInstruction(String fileName, String hashCode) {
		this.fileName = fileName;
		this.hashCode = hashCode;
	}

	/*
	 * Getters and Setters
	 */

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = new File(fileName).getName();
	}

	public String getHashCode() {
		return hashCode;
	}

	public void setHashCode(String hashCode) {
		this.hashCode = hashCode;
	}

	@Override
	public String Type() {
		// TODO Auto-generated method stub
		return "AskNewBlock";
	}

	@SuppressWarnings("unchecked")
	@Override
	public String ToJSON() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("fileName", getFileName());
			obj.put("Type", Type());
			obj.put("hash", new String(Base64.encodeBase64(getHashCode().getBytes()), "US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return obj.toJSONString();
	}

	@Override
	public void FromJSON(String jst) {
		JSONObject obj = null;

		try {
			obj = (JSONObject) parser.parse(jst);
		} catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if (obj != null) {
			fileName = (String) obj.get("fileName");
			hashCode = new String(Base64.decodeBase64((String) obj.get("hash")));
		}
	}
}
