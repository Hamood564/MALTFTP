package com.example.ftpclient_malt;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

public class FTPClientHelper {

    private FTPConnect ftpConnect;

    // Constructor to initialize FTPConnect
    public FTPClientHelper() {
        ftpConnect = new FTPConnect();
    }

    public void connect(String server, int port, String user, String pass) throws IOException {
       // ftpConnect.ServerConnect(server, user, pass);
        FTPClientManager.reconnect(server, user, pass);
        String s = ftpConnect.getDirectoryListing();
        System.out.println(s);
    }



    public void disconnect() throws IOException {
        if (ftpConnect != null) {
            ftpConnect.close();  // Properly close the connection
            ftpConnect = null;  // Nullify the instance to allow garbage collection
        }

    }

    public String getDirectoryListing() throws IOException {
        String s = ftpConnect.getDirectoryListing();
        return s;
    }

    public String getCurrentDirectory(){
        return ftpConnect.getCurrentDirectory();
    }


    public InputStream downloadFile(String fileName) throws IOException {
        InputStream inputStream = FileManager.Download(fileName);
        return inputStream;
    }

    public void downloadAllFiles(String folderName, String localFolderName) {
        FileManager.DownloadAllFiles(folderName,localFolderName);
    }

    public boolean isConnected(){
        return FTPClientManager.isConnected();
    }


    public void uploadFile(String text, String fileName) throws IOException {
        FileManager ftpmanager = new FileManager();
        ftpmanager.Upload(text, fileName);
    }

    public void deleteFile(String fileName) throws IOException {
        FileManager ftpmanager = new FileManager();
        ftpmanager.Delete(fileName);
    }

    public void deleteDirectory(String DirectoryName) throws IOException, ParseException {
        FileManager ftpmanager = new FileManager();
        ftpmanager.DeleteDirectory(DirectoryName);
    }

    public void changeDirectory(String dirPath) throws IOException {
        ChangeDirectory ftpcd = new ChangeDirectory();
        ftpcd.CD(dirPath);
    }

    public void changeToParentDirectory() throws IOException {
        ChangeDirectory ftpcd = new ChangeDirectory();
        ftpcd.CDParent();
    }

    public void setTransfer(String type){
        FileManager ftpmanager = new FileManager();
        if (type == "ASCII"){
            ftpmanager.setTransferASCII();
        }else{
            ftpmanager.setTransferBinary();
        }
    }

    public boolean isDirectory(String path) throws IOException, ParseException, FTPException {
        FTPFile[] files = ftpConnect.listFiles();
        for (FTPFile file : files) {
            if (file.getName().equals(path)) {
                return file.isDir();
            }
        }
        return false;
    }


    public FTPFile[] getFileNames(){

        return FileManager.getFileNames();
    }

    public void streamingOut(String text, String fileName){
        StreamData stream = new StreamData();
        stream.outputStream(text, fileName);
    }

    public String streamingIn(String fileName){
        StreamData stream = new StreamData();
        String s = stream.inputStream(fileName);
        return s;
    }

    public void setLogger(String fileName, String logLevel){
        TransferMonitoring ftptransfer = new TransferMonitoring();
        ftptransfer.Logger(fileName, logLevel);


    }
    public void setMode (String Mode){
        if(Mode == "ACTIVE"){
            ftpConnect.setActive();
        }else{
            ftpConnect.setPassive();
        }
    }

}
