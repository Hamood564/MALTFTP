package com.example.ftpclient_malt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MainActivity extends AppCompatActivity{
    private FTPClientHelper ftpClientHelper;
    private EditText etServer, etPort, etUser, etPassword, etDirPath, etFileName;
    private TextView tvProjectNameValue, tvIpAddressValue, tvDirectoryPathValue;
    private String ipAddress = "", port = "", user = "", password = "",currentProject="",currentDirectoryPath= "";
    private ListView lvDirectoryListing;
    //private ArrayAdapter<String> directoryAdapter;
    private CustomAdapter directoryAdapter; // Change this to CustomAdapter
    //private List<String> directories;
    private List<FileItem> directories;
    private ToggleButton toggleSelectMode;
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_CODE_PICK_FILE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set background color programmatically
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundColor(Color.parseColor("#7a96ea"));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Get the ViewModel associated with this activity.. keep connection when rotating
        FtpViewModel viewModel = new ViewModelProvider(this).get(FtpViewModel.class);

        // Retrieve the FTPClientHelper from the ViewModel
        ftpClientHelper = viewModel.getFtpClientHelper();

        tvProjectNameValue = findViewById(R.id.tvProjectNameValue);
        tvIpAddressValue = findViewById(R.id.tvIpAddressValue);
        tvDirectoryPathValue = findViewById(R.id.tvDirectoryPathValue);
        lvDirectoryListing = findViewById(R.id.lvDirectoryListing);

        Button btnUploadFile = findViewById(R.id.btnUploadFile);
        ImageButton btnDownloadSelected = findViewById(R.id.btnDownloadSelected);
        ImageButton btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        ImageButton btnSelectProject = findViewById(R.id.btnChangeProject);
        ImageButton helpButton = findViewById(R.id.btnHelpFTP);
        toggleSelectMode = findViewById(R.id.toggleSelectMode);


        // Initialize the list of directories
        directories = new ArrayList<>();
        //directoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, directories);
        directoryAdapter = new CustomAdapter(this, directories); // Initialize with FileItem
        lvDirectoryListing.setAdapter(directoryAdapter);

        // Request storage permissions
        requestStoragePermission();

        if (hasStoragePermission()) {
            loadConfigurationAndConnect();
        } else {
            requestStoragePermission();
        }

        //Button behviours...

        //Upload behaviour
        btnUploadFile.setVisibility(View.INVISIBLE); //set to invisible programmatically.Make it usable aby removing this line..
        btnUploadFile.setOnClickListener(v -> openFilePicker());
        //download selected behaviour
        btnDownloadSelected.setOnClickListener(v -> {
            List<FileItem> selectedItems = directoryAdapter.getSelectedItems();

            if (selectedItems.isEmpty()) {
                Toast.makeText(MainActivity.this, "No items selected for download", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create the main local directory once
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String dateString = sdf.format(new Date());
            File mainLocalDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), currentProject + "_" + dateString);

            if (!mainLocalDir.exists() && !mainLocalDir.mkdirs()) {
                Toast.makeText(MainActivity.this, "Failed to create main directory", Toast.LENGTH_SHORT).show();
                return;
            }

            for (FileItem item : selectedItems) {
                if (item.isDirectory()) {
                    new DownloadAllFilesTask(mainLocalDir).execute(item.getName()); // Use the existing download logic
                } else {
                    // Create the subdirectory inside the main directory with the name of the remote folder
                    String[] pathParts = currentDirectoryPath.split("/");
                    String lastDirectory = pathParts[pathParts.length - 1];
                    File subLocalDir = new File(mainLocalDir, lastDirectory);
                    if (!subLocalDir.exists() && !subLocalDir.mkdirs()) {
                        Toast.makeText(MainActivity.this, "Failed to create subdirectory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new DownloadFileTask(subLocalDir).execute(item.getName());
                }
            }
            directoryAdapter.clearSelection(); // Clear selection after action
        });
        //delete selected behaviour
        btnDeleteSelected.setOnClickListener(v -> {
            List<FileItem> selectedItems = directoryAdapter.getSelectedItems();
            for (FileItem item : selectedItems) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Do you want to delete " + item.getName() + "?")
                        .setItems(new String[]{"Confirm","Cancel"},(dialog_confirm,which_confirm) -> {
                            switch (which_confirm) {
                                case 0: //Confirm Delete
                                    new DeleteFileTask().execute(item.getName());
                                    break;
                                case 1: //Cancel
                                    dialog_confirm.dismiss();
                                    break;
                            }
                        }).show();
            }
            directoryAdapter.clearSelection(); // Clear selection after action
        });
        //toggle button behvaiour
        btnDownloadSelected.setEnabled(false);
        btnDownloadSelected.setBackgroundColor(Color.TRANSPARENT);
        btnDeleteSelected.setEnabled(false);
        btnDeleteSelected.setBackgroundColor(Color.TRANSPARENT);

        toggleSelectMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {

                // Switch to selection mode
                btnDownloadSelected.setEnabled(true);
                btnDownloadSelected.setBackgroundColor(Color.parseColor("#f6f8ff"));
                btnDeleteSelected.setEnabled(true);
                btnDeleteSelected.setBackgroundColor(Color.parseColor("#f6f8ff"));
                Toast.makeText(MainActivity.this, "Selection mode activated", Toast.LENGTH_SHORT).show();
            } else {
                // Switch back to normal mode
                directoryAdapter.clearSelection();  // Clear selection
                btnDownloadSelected.setEnabled(false);
                btnDownloadSelected.setBackgroundColor(Color.TRANSPARENT);
                btnDeleteSelected.setEnabled(false);
                btnDeleteSelected.setBackgroundColor(Color.TRANSPARENT);
                Toast.makeText(MainActivity.this, "Normal mode activated", Toast.LENGTH_SHORT).show();
            }
        });
        //change project behaviour
        btnSelectProject.setBackgroundColor(Color.parseColor("#f6f8ff"));
        btnSelectProject.setOnClickListener(v -> {
                    directories.clear();
                    directoryAdapter.notifyDataSetChanged();
                    loadConfigurationAndConnect();
                });
        // help button behaviour
        helpButton.setBackgroundColor(Color.parseColor("#f6f8ff"));
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertHelpdialog();
            }
        });


        // Set up the ListView item click listener..directory listing
        lvDirectoryListing.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileItem selectedItem = directories.get(position);
                String selectedDirectory = selectedItem.getName(); // Get the item name

                if (toggleSelectMode.isChecked()) {
                    // Check if the item is not ".." or "." before toggling selection
                    if (!selectedDirectory.equals("..") && !selectedDirectory.equals(".")) {
                        directoryAdapter.toggleSelection(position);
                    } else {
                        Toast.makeText(MainActivity.this, "Cannot select this directory", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Change directory if not in selection mode
                    new ChangeDirectoryTask().execute(selectedDirectory);
                }

                }
        });

        // Set up the ListView item long click listener..long click on item
        lvDirectoryListing.setOnItemLongClickListener((parent, view, position, id) -> {
            FileItem selectedItem = directories.get(position);
            String selectedDirectory = selectedItem.getName(); // Get the item name
            showFileOptions(selectedItem.getName());
            return true;
        });

    }

    private void showFileOptions(String itemName) {
        new CheckIfDirectoryTask().execute(itemName);
    }

    private class CheckIfDirectoryTask extends AsyncTask<String, Void, Boolean> {
        private String itemName;

        @Override
        protected Boolean doInBackground(String... params) {
            itemName = params[0];
            try {
                return ftpClientHelper.isDirectory(itemName);
            } catch (IOException | ParseException | FTPException e) {
                e.printStackTrace();
                return false; // Assume it's a file if there's an error
            }
        }

        @Override
        protected void onPostExecute(Boolean isDirectory) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select an option for " + itemName)
                    .setItems(new String[]{"Download", "Delete", "Cancel"}, (dialog, which) -> {
                        switch (which) {
                            case 0: // Download
                                // Create the main local directory once
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                                String dateString = sdf.format(new Date());
                                File mainLocalDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), currentProject + "_" + dateString);

                                if (!mainLocalDir.exists() && !mainLocalDir.mkdirs()) {
                                    Toast.makeText(MainActivity.this, "Failed to create main directory", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (isDirectory) {
                                    new DownloadAllFilesTask(mainLocalDir).execute(itemName);
                                } else {
                                    // Create the subdirectory inside the main directory with the name of the remote folder
                                    String[] pathParts = currentDirectoryPath.split("/");
                                    String lastDirectory = pathParts[pathParts.length - 1];
                                    System.out.println("the subdirectory is >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>!!>>>"+ lastDirectory+ "& "+itemName + "&"  + mainLocalDir + "&" );
                                    File subLocalDir = new File(mainLocalDir, lastDirectory);
                                    if (!subLocalDir.exists() && !subLocalDir.mkdirs()) {
                                        Toast.makeText(MainActivity.this, "Failed to create subdirectory", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    new DownloadFileTask(subLocalDir).execute(itemName);

                                }
                                break;
                            case 1: // Delete
                                builder.setTitle("Do you want to delete " + itemName + (isDirectory ? " and its contents?" : "?"))
                                        .setItems(new String[]{"Confirm","Cancel"},(dialog_confirm,which_confirm) -> {
                                            switch (which_confirm) {
                                                case 0: //Confirm Delete
                                                    new DeleteFileTask().execute(itemName);
                                                    break;
                                                case 1: //Cancel
                                                    dialog_confirm.dismiss();
                                                    break;
                                            }
                                }).show();
                                break;
                            case 2: // Cancel
                                dialog.dismiss();
                                break;
                        }
                    })
                    .show();
        }
    }


    // methods responsible for getting storage permission and values from config.txt

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_STORAGE_PERMISSION);
            } else {
                // Load configuration if permission is already granted
                loadConfigurationAndConnect();
            }
        } else {
            if (!hasStoragePermission()) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                // Load configuration if permission is already granted
                loadConfigurationAndConnect();
            }
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadConfigurationAndConnect();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_STORAGE_PERMISSION && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadConfigurationAndConnect();
                    new ConnectTask().execute(ipAddress, port, user, password);
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData(); // Get the URI of the selected file
                new UploadFileTask().execute(fileUri); // Start upload task with the file URI
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    private Map<String, FtpConfig> loadConfiguration() {
        File configFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "config.txt");

        if (!configFile.exists()) {
            Toast.makeText(this, "Configuration file not found", Toast.LENGTH_SHORT).show();
            return Collections.emptyMap();
        }

        Map<String, FtpConfig> configMap = new HashMap<>();
        String currentProject = null;
        FtpConfig currentConfig = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if (key.equals("project_name")) {
                        if (currentProject != null && currentConfig != null) {
                            configMap.put(currentProject, currentConfig);
                        }
                        currentProject = value;
                        currentConfig = new FtpConfig(); // Create a new config for this project
                    } else if (currentConfig != null) {
                        switch (key) {
                            case "server":
                                currentConfig.server = value;
                                break;
                            case "port":
                                currentConfig.port = 21; //ftp is always port 21
                                break;
                            case "user":
                                currentConfig.user = "Anonymous"; //uses for login, parse as value if not. Need to define in config.txt (IMPORTANT!)
                                break;
                            case "password":
                                currentConfig.password = "";//uses for login, parse as value if not. Need to define in config.txt (IMPORTANT!)
                                break;
                        }
                    }
                }
            }
            // Add the last project
            if (currentProject != null && currentConfig != null) {
                configMap.put(currentProject, currentConfig);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading configuration file", Toast.LENGTH_SHORT).show();
        }
        return configMap;
    }



    //Task methods for executing actions
    private void loadConfigurationAndConnect() {
        selectProjectAndConnect(); // Show project selection dialog
    }

    private void selectProjectAndConnect() {
        Map<String, FtpConfig> configMap = loadConfiguration(); // Load configurations
        if (configMap.isEmpty()) {
            return; // No configurations to load
        }

        String[] projectNames = configMap.keySet().toArray(new String[0]);
        final String[] selectedProject = {projectNames[0]}; // Use an array to hold the selected project

        // Create the dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Project");
        builder.setSingleChoiceItems(projectNames, 0, (dialog, which) -> {
            selectedProject[0] = projectNames[which]; // Update selected project
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Get the selected config
            FtpConfig selectedConfig = configMap.get(selectedProject[0]);
            if (selectedConfig != null) {

                // Update UI with selected project details
                tvProjectNameValue.setText(selectedProject[0]);
                tvIpAddressValue.setText(selectedConfig.server);

                currentProject = selectedProject[0]; //updating for usage in downloads


                // Show a loading indicator
                ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Connecting to the FTP server...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                // Execute the connection task
                new ConnectTask() {
                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                        progressDialog.dismiss();  // Dismiss the loading indicator

                        // If connection is successful, dismiss the dialog
                        if (result.contains("Connected")) {
                            dialog.dismiss();
                        }
                        Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                    }
                }.execute(selectedConfig.server, String.valueOf(selectedConfig.port), selectedConfig.user, selectedConfig.password);

            } else {
                Toast.makeText(MainActivity.this, "Selected project configuration is invalid", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());// Dismiss the dialog on cancel

        // Show the dialog
        builder.create().show();
    }


    //help popup
    private void alertHelpdialog() {

        // Create an AlertDialog to show help information
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // Set the title of the dialog
        builder.setTitle("Help Information");

        // Set the detailed help text
        builder.setMessage("Welcome to MALT FTP! \n\n" +
                "Here’s how to use the app:\n\n" +
                "1. \"Project Name\": Displays the current project name.\n" +
                "2. \"IP Address\": Shows the IP address of the FTP server.\n" +
                "3. \"Directory Listing\": Lists all files and folders in the connected directory.\n" +
                "4. \"Download Selected\": Download the selected file(s).\n" +
                "5. \"Delete Selected\": Remove the selected file(s) from the server.\n" +
                "6. \"Change Project\": Change to a different project.\n\n" +
                "Additional Features:\n\n" +
                "7. \"Long-Click on Files\": \n" +
                "   - Action: Long-pressing a file or folder in the directory listing will allow you to select item.\n" +
                "   - Usage: After selecting item, you can choose to download, delete, or cancel.\n\n" +
                "8. \"Click on Files with Toggle ON\": \n"+
                "   - Action: Pressing a file or folder or both in the directory listing will allow you to select multiple items.\n" +
                "   - Usage: After selecting items, you can choose to download, or delete selected items.\n\n" +
                "9. \"Navigating Between Directories\": \n" +
                "   - Action: Tap on a folder to enter it. \n" +
                "   - Navigation: Use the '..' directory to go back to the previous directory.\n\n" +
                "For more detailed help, please refer to the documentation or contact support.");

        // Set the positive button and its click listener
        builder.setPositiveButton("OK", null);

        // Show the dialog
        builder.show();
    }


    //connecting to FTP server
    private class ConnectTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String server = params[0];
                String portString = params[1]; // Get the port string
                String user = params[2];
                String password = params[3];

                // Check if the port is empty and handle accordingly
                int port = 21; // Default port
                if (!portString.isEmpty()) {
                    port = Integer.parseInt(portString); // Parse only if it's not empty
                }

                System.out.println("HERE!!!!!!!!!>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+ server + ":" + port + "," + user + "," + password);

                ftpClientHelper.connect(server, port, user, password);
                return "Connected and directory listing obtained";
            } catch (IOException e) {
                e.printStackTrace();
                return "Failed to connect due to IOException: " + e.getMessage();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return "Failed to connect due to IllegalStateException: " + e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
                return "Failed to connect due to an unexpected error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            ImageView connectionStatusIndicator = findViewById(R.id.connectionStatusIndicator);
            if (result.contains("Connected")) {
                // Connection successful
                connectionStatusIndicator.setImageResource(R.drawable.circle_green);
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                new updateDirectoryPath().execute(); // Update the directory path
                updateDirectoryListing();
            } else {
                // show an alert dialog with the same message
                connectionStatusIndicator.setImageResource(R.drawable.circle_red);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Connection Failed")
                        .setMessage("MALT not found - Check Config settings and Ethernet connection")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }
    }

    //Changing directory
    private class ChangeDirectoryTask extends AsyncTask<String, Void, String> {
        private String itemName;
        @Override
        protected String doInBackground(String... params) {
            itemName = params[0];
            try {

                if(ftpClientHelper.isDirectory(itemName) == true){
                    ftpClientHelper.changeDirectory(params[0]);
                    return "Directory changed to " + params[0];
                }else{
                    return "Long click for options for " + params[0];
                }

            } catch (IOException | FTPException | ParseException e) {
                e.printStackTrace();
                return "Failed to change directory";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            new updateDirectoryPath().execute(); // Update the directory path
            updateDirectoryListing();
        }
    }

    //moving to parent directory
    private class ParentDirectoryTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                ftpClientHelper.changeToParentDirectory();
                return "Directory changed to Parent";
            } catch (IOException e) {
                e.printStackTrace();
                return "Failed to change directory";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            updateDirectoryListing();
        }
    }

    //download file after selecting
    private class DownloadFileTask extends AsyncTask<String, Void, String> {
        private File subLocalDir;

        public DownloadFileTask(File subLocalDir){
            this.subLocalDir = subLocalDir;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                InputStream inputStream = ftpClientHelper.downloadFile(params[0]);
                // Get the Documents directory
                File documentsDir = new File(subLocalDir, params[0]);

                // Save the file
                try (FileOutputStream outputStream = new FileOutputStream(documentsDir)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } finally {
                    inputStream.close();
                }

                return "File downloaded: " + params[0];
            } catch (IOException e) {
                e.printStackTrace();
                return "Failed to download file";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    //download files in a folder into a local directory
    private class DownloadAllFilesTask extends AsyncTask<String, Void, String> {
        private File mainLocalDir;

        public DownloadAllFilesTask(File mainLocalDir) {
            this.mainLocalDir = mainLocalDir;
        }

        @Override
        protected String doInBackground(String... params) {
            String localFolderName = params[0];
            try {

                // Create the subdirectory inside the main directory with the name of the remote folder
                File sublocalDir = new File(mainLocalDir, localFolderName);
                if (!sublocalDir.exists() && !sublocalDir.mkdirs()) {
                    return "Failed to create subdirectory";
                }

                // Change to the remote directory
                ftpClientHelper.changeDirectory(localFolderName);

                // Get all file names
                FTPFile[] fileNames = ftpClientHelper.getFileNames();

                for (FTPFile file : fileNames) {
                    if (file.isDir()) {
                        System.out.println("Skipping directory: " + file.getName());
                        continue;
                    }

                    try {
                        InputStream inputStream = ftpClientHelper.downloadFile(file.getName());
                        File localFile = new File(sublocalDir, file.getName());

                        // Save the file
                        try (FileOutputStream outputStream = new FileOutputStream(localFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        } finally {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Change to the parent directory
                ftpClientHelper.changeToParentDirectory();

                return "All files downloaded";
            } catch (IOException e) {
                e.printStackTrace();
                return "Failed to download files";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    //uploading file into the present directory
    private class UploadFileTask extends AsyncTask<Uri, Void, String> {
        @Override
        protected String doInBackground(Uri... params) {
            Uri fileUri = params[0];


            try {
                // Get the file name from the URI
                String fileName = getFileName(fileUri);



                // Open an InputStream for the file
                InputStream inputStream = getContentResolver().openInputStream(fileUri);

                if (inputStream == null) {
                    return "Failed to open file";
                }

                // Read the file content into a String
                StringBuilder stringBuilder = new StringBuilder();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    stringBuilder.append(new String(buffer, 0, bytesRead));
                }
                inputStream.close();

                // Convert the StringBuilder to a String
                String fileContent = stringBuilder.toString();

                // Use FTPClientHelper to upload the file
                ftpClientHelper.uploadFile(fileContent, fileName);

                return "File uploaded: " + fileName;
            } catch (IOException e) {
                e.printStackTrace();
                return "Failed to upload file: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            updateDirectoryListing();
        }
    }

    //getting a list of files to process in other methods
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    //deleting files after selecting
    private class DeleteFileTask extends AsyncTask<String, Void, String> {
        private String itemName;
        private boolean isDirectory;

        @Override
        protected String doInBackground(String... params) {
            itemName = params[0];
            try {

                // Check if it's a directory
                isDirectory = ftpClientHelper.isDirectory(itemName);
                if (isDirectory) {
                    // Implement logic to delete the directory
                    deleteDirectory(itemName);
                } else {
                    // Delete the file
                    ftpClientHelper.deleteFile(itemName);
                }
                //ftpClientHelper.deleteFile(params[0]);
                return "File deleted: " + params[0];
            } catch (IOException |ParseException | FTPException e) {
                e.printStackTrace();
                return "Failed to delete file";
            }
        }

        private void deleteDirectory(String directoryName) throws IOException, ParseException {
           ftpClientHelper.deleteDirectory(directoryName);
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            updateDirectoryListing();
        }
    }

    //updating directory list after any task method has been performed
    private void updateDirectoryListing() {
        new UpdateDirectoryListingTask().execute();
    }

    private class UpdateDirectoryListingTask extends AsyncTask<Void, Void, List<FileItem>> {

        @Override
        protected List<FileItem> doInBackground(Void... voids) {
            List<FileItem> fileItems = new ArrayList<>();
            try {
                String directoryListing = ftpClientHelper.getDirectoryListing();
                BufferedReader reader = new BufferedReader(new StringReader(directoryListing));
                String line;

                while ((line = reader.readLine()) != null) {
                    boolean isDirectory = ftpClientHelper.isDirectory(line); // Check if it's a directory
                    fileItems.add(new FileItem(line, isDirectory)); // Create FileItem
                }
            } catch (IOException | ParseException | FTPException e) {
                e.printStackTrace();
            }
            return fileItems;
        }

        @Override
        protected void onPostExecute(List<FileItem> result) {
            directories.clear(); // Clear previous items
            directories.addAll(result); // Add new FileItem list
            directoryAdapter.notifyDataSetChanged(); // Notify adapter of data change
        }


        private List<String> parseDirectoryListing(String listing) {
            List<String> directories = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new StringReader(listing));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    directories.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return directories;
        }
    }

    private class updateDirectoryPath extends AsyncTask <Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String currentDirectory = ftpClientHelper.getCurrentDirectory(); // Get the current directory from FTPClientHelper
            return currentDirectory;


        }
        @Override
        protected void onPostExecute(String currentDirectory) {
            tvDirectoryPathValue.setText(currentDirectory); // Set the current directory path
            currentDirectoryPath = currentDirectory;
            System.out.println("The Directory is !!!!!!!!!!!!>>>>>>>>>>>>Gshuhd"+ ">>>>"+ currentDirectory);
        }


    }


}
