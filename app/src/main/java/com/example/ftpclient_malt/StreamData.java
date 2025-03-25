package com.example.ftpclient_malt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

public class StreamData {
	
	public static String inputStream(String FileName) {
		// set up logger so that we get some output
        Logger log = Logger.getLogger(StreamData.class);
        Logger.setLevel(Level.INFO);
        StringBuffer s = new StringBuffer();
        try {
            FileTransferClient ftpClient = FTPClientManager.getInstance();
			InputStream in = ftpClient.downloadStream(FileName);
			
			try {
                int ch = 0;
                while ((ch = in.read()) >= 0) {
                    s.append((char)ch);
                }
            }
            finally {
                in.close(); // MUST be closed to complete the transfer
            }
			
		} catch (FTPException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return s.toString();
        
	}
	
	public static void outputStream(String text,String FileName) {
			// set up logger so that we get some output
	        Logger log = Logger.getLogger(StreamData.class);
	        Logger.setLevel(Level.INFO);
	        
	        try {
	        	FileTransferClient ftpClient = FTPClientManager.getInstance();
	        	OutputStream out = ftpClient.uploadStream(FileName);
	        	 try {
	                 out.write(text.getBytes());
	             }
	             finally {
	                 out.close(); // MUST be closed to complete the transfer
	             }
	        	
	        }catch (FTPException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
