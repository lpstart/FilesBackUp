package com.onlinebackup.instruction;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;

import filesync.Instruction;

/**
 * ack CcopyBlockInstruction means the CopyBlockInstruction is OK and needn't a
 * new CopyBlockInstruction
 * 
 * @author wlp
 *
 */
public class ACKCopyBlockInstruction extends Instruction {

	private String fileName = null;
	private String hashcode = null;

	public ACKCopyBlockInstruction() {

		this.fileName = "";
		this.hashcode = "";
	}

	public ACKCopyBlockInstruction(String fileName, String hashcode) {
		this.fileName = fileName;
		this.hashcode = hashcode;
	}

	public String getFileName() {
		return new File(fileName).getName();
	}

	@Override
	public String Type() {
		return "ACKCopyBlockInStruction";
	}

	@SuppressWarnings("unchecked")
	@Override
	public String ToJSON() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("Type", Type());
			obj.put("hashcode", new String(Base64.encodeBase64((this.hashcode).getBytes()), "US-ASCII"));
			obj.put("fileName", getFileName());
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
			this.fileName = (String) obj.get("fileName");
			this.hashcode = (String) obj.get("hashcode");
		}
	}

}
