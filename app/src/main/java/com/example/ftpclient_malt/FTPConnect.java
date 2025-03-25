package com.example.ftpclient_malt;

import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.util.debug.Level;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.util.debug.Logger;

public class FTPConnect {
	
	
	public static void ServerConnect (String host, String username, String password) {
		Logger log = Logger.getLogger(FTPConnect.class);
		Logger.setLevel(Level.INFO);
		try {
			// Check if FTP client is initialized and connected
			FileTransferClient ftpClient = FTPClientManager.getInstance();
			if (!ftpClient.isConnected()) {
				log.info("FTP client is not connected. Reconnecting...");
				FTPClientManager.reconnect(host, username, password);
				log.info("Reconnected to the FTP server.");
			} else {
				log.info("FTP client is already connected.");
			}
		} catch (IllegalStateException e) {
			// This will catch the exception if the client is not initialized
			FTPClientManager.reconnect(host, username, password);
			log.info("FTP client was not initialized, now initialized and connected.");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


    public static String getDirectoryListing() {

		// set up logger so that we get some output
        Logger log = Logger.getLogger(FTPConnect.class);
        Logger.setLevel(Level.INFO);

		 log.info("Getting current directory listing");
         FTPFile[] files;
		StringBuilder fileList = new StringBuilder();
		try {

			FileTransferClient ftpClient = FTPClientManager.getInstance();
			files = ftpClient.directoryList(".");
//			for (int i = 0; i < files.length; i++) {
//	             log.info(files[i].toString());
//	         }
			for (FTPFile file : files) {
				log.info(file.toString());
				fileList.append(file.getName()).append("\n");
			}
		} catch (FTPException | IOException | ParseException e) {
			log.error("Error while getting directory listing", e);
			return "Error while getting directory listing: " + e.getMessage();
		}
		return fileList.toString();

	}

	public static String getCurrentDirectory() {
		String currentDir = null;
		try {
			FileTransferClient ftpClient = FTPClientManager.getInstance();
			currentDir = ftpClient.getRemoteDirectory();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return currentDir;
	}
	
	public static void close() {
		
		// set up logger so that we get some output
        Logger log = Logger.getLogger(FTPConnect.class);
        Logger.setLevel(Level.INFO);
        
        try {
        	FileTransferClient ftpClient = FTPClientManager.getInstance();
    		// Shut down client
            log.info("Quitting client!");
			ftpClient.disconnect();
		} catch (FTPException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public FTPFile[] listFiles() throws IOException, ParseException, FTPException {
		FileTransferClient ftpClient = FTPClientManager.getInstance();
		return ftpClient.directoryList();
	}
	
	public static void setActive() {
		// set up logger so that we get some output
        Logger log = Logger.getLogger(FTPConnect.class);
        Logger.setLevel(Level.INFO);
        
        try {
        	FileTransferClient ftpClient = FTPClientManager.getInstance();
			ftpClient.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.ACTIVE);
			log.info("Successfully transferred active mode");
		} catch (FTPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
            
	}
	
	public static void setPassive() {
		// set up logger so that we get some output
        Logger log = Logger.getLogger(FTPConnect.class);
        Logger.setLevel(Level.INFO);
        
        try {
        	FileTransferClient ftpClient = FTPClientManager.getInstance();
			ftpClient.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
			log.info("Successfully transferred passive mode");
		} catch (FTPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}
	
	
	public static void main(String[] args) {
		ServerConnect("192.168.115.176", "Anonymous", "");
		getDirectoryListing();
	}

}
