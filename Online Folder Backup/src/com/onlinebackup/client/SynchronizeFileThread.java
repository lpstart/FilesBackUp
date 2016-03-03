package com.onlinebackup.client;

import java.io.File;
import java.io.IOException;

import filesync.SynchronisedFile;

/**
 * thread that will check the change of the SynchronisedFile periodically
 * 
 * @author wlp
 *
 */
public class SynchronizeFileThread implements Runnable {
	private SynchronisedFile syncFile = null;

	/**
	 * constructor with SynchronisedFile parameter
	 * 
	 * @param syncFile
	 *            the SynchronisedFile need check periodically
	 */
	public SynchronizeFileThread(SynchronisedFile syncFile) {
		this.syncFile = syncFile;
	}

	@Override
	public void run() {
		// get local file
		File fileLocal = new File(syncFile.getFilename());
		try {

			// judge the local file is existed or not
			while (fileLocal.exists()) {
				// if the local file is existed,
				// check the change and add changes instruction to syncifile's
				// instQ
				try {
					syncFile.CheckFileState();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				// sleep 1000ms
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}

	}
}
