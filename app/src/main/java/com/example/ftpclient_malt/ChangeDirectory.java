package com.example.ftpclient_malt;


import java.io.IOException;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;



public class ChangeDirectory {
	
	public static void CD (String dir){
		

        // set up logger so that we get some output
        Logger log = Logger.getLogger(ChangeDirectory.class);
        Logger.setLevel(Level.INFO);

        try {
        	FileTransferClient ftpClient = FTPClientManager.getInstance();
            
            log.info("Current dir: " + ftpClient.getRemoteDirectory());
            log.info("Changing directory");
            ftpClient.changeDirectory(dir);
            log.info("Current dir: " + ftpClient.getRemoteDirectory());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	public static void CDParent() {
		
		// set up logger so that we get some output
        Logger log = Logger.getLogger(ChangeDirectory.class);
        Logger.setLevel(Level.INFO);
        try {
        	FileTransferClient ftpClient = FTPClientManager.getInstance();
        	log.info("Changing up");
			ftpClient.changeToParentDirectory();
			log.info("Current dir: " + ftpClient.getRemoteDirectory());
		} catch (FTPException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}       
		
	}
}


