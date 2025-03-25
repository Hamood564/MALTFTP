package com.example.ftpclient_malt;
import androidx.lifecycle.ViewModel;

import java.io.IOException;

public class FtpViewModel extends ViewModel {
    private FTPClientHelper ftpClientHelper;

    public FTPClientHelper getFtpClientHelper() {
        if (ftpClientHelper == null) {
            ftpClientHelper = new FTPClientHelper();
        }
        return ftpClientHelper;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Optionally disconnect the FTP client when the ViewModel is destroyed
        if (ftpClientHelper != null ) {
            try {
                ftpClientHelper.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
