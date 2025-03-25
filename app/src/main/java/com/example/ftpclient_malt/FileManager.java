package com.example.ftpclient_malt;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;

import com.enterprisedt.net.ftp.EventAdapter;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.net.ftp.WriteMode;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

public class FileManager{

	public static void Upload (String text, String fileName) {
		// set up logger so that we get some output
		Logger log = Logger.getLogger(FileManager.class);
		Logger.setLevel(Level.INFO);

		try {
			FileTransferClient ftpClient = FTPClientManager.getInstance();
			log.info("Uploading file");
			//ftpClient.uploadFile(fileName, fileName);// this also works but not used here for testing as this method requires files to be in rrot directory
			OutputStream out = ftpClient.uploadStream(fileName);// alternatively used to get message bytes and write a file in FTP server
			try {
				out.write(text.getBytes());
			}
			finally {
				out.close(); // MUST be closed to complete the transfer
			}
		} catch (FTPException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("File uploaded");
	}
	
	public static InputStream Download (String fileName) {
		// set up logger so that we get some output
		Logger log = Logger.getLogger(FileManager.class);
		Logger.setLevel(Level.INFO);

		InputStream inputStream = null;
		try {
			FileTransferClient ftpClient = FTPClientManager.getInstance();
			log.info("Downloading  file");
			//ftpClient.downloadFile(fileName, fileName); //this only downloads locally but the inputstream method will send it by inputstream so it can written to directory
			inputStream = ftpClient.downloadStream(fileName);
			log.info("File downloaded");

		} catch (FTPException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("File downloaded");
		return inputStream;
	}

	public static FTPFile[] getFileNames (){
		// set up logger so that we get some output
		Logger log = Logger.getLogger(FileManager.class);
		Logger.setLevel(Level.INFO);
		FTPFile[] fileNames = null;
		try {
			FileTransferClient ftpClient = FTPClientManager.getInstance();
			fileNames = ftpClient.directoryList();
		} catch (FTPException | IOException| ParseException  e) {
			e.printStackTrace();
		}
		return fileNames;

	}


	public static void DownloadAllFiles(String folderName, String localFolderName) {
		Logger log = Logger.getLogger(FileManager.class);
		Logger.setLevel(Level.INFO);

		try {
			FileTransferClient ftpClient = FTPClientManager.getInstance();
			log.info("Listing files in folder: " + folderName);
			FTPFile[] fileNames = ftpClient.directoryList(folderName);


			// Create the local directory inside DIRECTORY_DOWNLOADS
			File localDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), localFolderName);
			if (!localDir.exists()) {
				localDir.mkdirs();
			}

			for (FTPFile file : fileNames) {
				if (file.isDir()) {
					log.info("Skipping directory: " + file.getName());
					continue;
				}
				log.info("Downloading file: " + file.getName());
				InputStream inputStream = ftpClient.downloadStream(file.getName());
				File localFile = new File(localDir,file.getName());
				try (OutputStream out = new FileOutputStream(localFile)) {
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer)) != -1) {
						out.write(buffer, 0, bytesRead);
					}
				}
				inputStream.close();
				log.info("File downloaded: " + file.getName());
			}
		} catch (FTPException | IOException| ParseException  e) {
			e.printStackTrace();
		}
	}


	
	public static void Delete (String FileName) {
		Logger log = Logger.getLogger(FileManager.class);
        Logger.setLevel(Level.INFO);
        
		try {
			FileTransferClient ftpClient = FTPClientManager.getInstance();
			log.info("Deleting remote file");
			ftpClient.deleteFile(FileName);
		} catch (FTPException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        log.info("Deleted remote file");
	}

	public static void DeleteDirectory(String directoryName) throws ParseException {
		Logger log = Logger.getLogger(FileManager.class);
		Logger.setLevel(Level.INFO);

		try {
			FileTransferClient ftpClient = FTPClientManager.getInstance();
			log.info("Deleting contents of directory: " + directoryName);

			// Change to the directory to be deleted
			ftpClient.changeDirectory(directoryName);

			// List all files and directories in the current directory
			FTPFile[] files = ftpClient.directoryList();

			for (FTPFile file : files) {
				String fileName = file.getName();
				// Skip the current directory (.) and parent directory (..)
				if (fileName.equals(".") || fileName.equals("..")) {
					continue;
				}

				if (file.isDir()) {
					// Recursively delete the subdirectory
					DeleteDirectory(directoryName + "/" + fileName);
				} else {
					// Delete the file
					log.info("Deleting file: " + fileName);
					ftpClient.deleteFile(fileName);
				}
			}

			// Change to parent directory before deleting the directory itself
			ftpClient.changeToParentDirectory();

			// Delete the directory
			log.info("Deleting directory: " + directoryName);
			ftpClient.deleteDirectory(directoryName);

		} catch (FTPException | IOException e) {
			e.printStackTrace();
		}
		log.info("Deleted directory: " + directoryName);
	}
	
	public static void setTransferBinary() {
		Logger log = Logger.getLogger(FileManager.class);
        Logger.setLevel(Level.INFO);
       
        try {
        	FileTransferClient ftpClient = FTPClientManager.getInstance();
			ftpClient.setContentType(FTPTransferType.BINARY);
			log.info("Successfully transferred binary mode");
		} catch (IOException | FTPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void setTransferASCII() {
		Logger log = Logger.getLogger(FileManager.class);
        Logger.setLevel(Level.INFO);
       
        try {
        	FileTransferClient ftpClient = FTPClientManager.getInstance();
			ftpClient.setContentType(FTPTransferType.ASCII);
			log.info("Successfully transferred ASCII mode");
		} catch (IOException | FTPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void Pause(String fileName) {
		//Maybe not needed.
		
	}
	
	public static void Resume(String fileName) {
		
		FileTransferClient ftpClient = FTPClientManager.getInstance();
		File file = new File(fileName);
		
		CancelListener cl = new CancelListener(ftpClient);
		ftpClient.setEventListener(cl);
		
		Logger log = Logger.getLogger(FileManager.class);
        Logger.setLevel(Level.INFO);
		log.info("Completing upload by resuming");
		
		try {
			ftpClient.uploadFile(fileName, fileName, WriteMode.RESUME);
			int len = (int) ftpClient.getSize(fileName);
	        len = (int) ftpClient.getSize(fileName);
	        // only the remaining bytes are transferred as can be seen
	        log.info("Bytes transferred=" + cl.getBytesTransferred());
	        log.info("File uploaded (localsize=" + file.length()
	                + " remotesize=" + len);
		} catch (FTPException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void Cancel() {
		//Maybe Not needed.
	}
	
	
	
	/**
	 * As soon it receives notification of bytes transferred, it cancels the
	 * transfer
	 */
	static class CancelListener extends EventAdapter {

	    private Logger log = Logger.getLogger(CancelListener.class);
	    
	    /**
	     * True if cancelled
	     */
	    private boolean cancelled = false;
	    
	    /**
	     * Keep the last reported byte count
	     */
	    private long bytesTransferred = 0;

	    /**
	     * FTPClient reference
	     */
	    private FileTransferClient ftp;
	    
	    /**
	     * Constructor
	     * 
	     * @param ftp
	     */
	    public CancelListener(FileTransferClient ftp) {
	        this.ftp = ftp;
	    }

	    public void bytesTransferred(String connId, String remoteFilename, long bytes) {
	        log.info("Bytes transferred=" + bytes);
	        if (!cancelled) {
	            ftp.cancelAllTransfers();
	            cancelled = true;
	        }
	        bytesTransferred = bytes;
	    }
	    
	    /**
	     * Will contain the total bytes transferred once the transfer is complete
	     */
	    public long getBytesTransferred() {
	        return bytesTransferred;
	    }
	    
	}


	

}
