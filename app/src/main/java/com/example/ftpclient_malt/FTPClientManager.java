package com.example.ftpclient_malt;

import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.util.debug.Logger;

public class FTPClientManager {
	
	private static FileTransferClient ftpClient;
    private static String currentHost;
    private static String currentUsername;
    private static String currentPassword;


    public static FileTransferClient getInstance() {
        if (ftpClient == null) {
            throw new IllegalStateException("FTP client not initialized. Call ServerConnect first.");
        }

        // Check if the client is still connected
        try {
            if (!ftpClient.isConnected()) {
                Logger log = Logger.getLogger(FTPConnect.class);
                log.info("FTP client disconnected. Reconnecting...");
                ftpClient.connect();
                log.info("Reconnected to the server.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to reconnect the FTP client.");
        }
        return ftpClient;
    }
	
	public static void initialize(String host, String username, String password) {
        // Check if reinitialization is required
        if (ftpClient == null || !host.equals(currentHost) || !username.equals(currentUsername) || !password.equals(currentPassword)) {
            if (ftpClient != null && ftpClient.isConnected()) {
                disconnect();
            }
            ftpClient = new FileTransferClient();
            currentHost = host;
            currentUsername = username;
            currentPassword = password;
            try {
                Logger log = Logger.getLogger(FTPConnect.class);
                log.info("Creating FTP client");

                log.info("Setting remote host");
                ftpClient.setRemoteHost(host);
                ftpClient.setUserName(username);
                ftpClient.setPassword(password);

                log.info("Connecting to server " + host);
                ftpClient.connect();

                log.info("Connected and logged in to server " + host);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isConnected() {
        return ftpClient.isConnected();
    }

    public static void reconnect(String host, String username, String password) {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                Logger log = Logger.getLogger(FTPConnect.class);
                log.info("Disconnecting from the current server...");
                ftpClient.disconnect();
            }
            ftpClient = null;  // Reset the client
            initialize(host, username, password);  // Reinitialize with new parameters
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to reconnect the FTP client.");
        }
    }

    public static void disconnect() {
        if (ftpClient != null) {
            try {
                Logger log = Logger.getLogger(FTPConnect.class);
                log.info("Disconnecting from the server...");
                ftpClient.disconnect();
                log.info("Disconnected from the server.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
