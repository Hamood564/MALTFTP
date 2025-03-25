package com.example.ftpclient_malt;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class AdvancedSettingsActivity extends AppCompatActivity {

    private FTPClientHelper ftpClientHelper;
    private EditText etLogFileName, etLogLevel, etStreamText, etStreamFileName;
    private Spinner spinnerTransferType, spinnerMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_settings);

        ftpClientHelper = new FTPClientHelper();

        // Initialize views
        etLogFileName = findViewById(R.id.etLogFileName);
        etLogLevel = findViewById(R.id.etLogLevel);
        etStreamText = findViewById(R.id.etStreamText);
        etStreamFileName = findViewById(R.id.etStreamFileName);
        spinnerTransferType = findViewById(R.id.spinnerTransferType);
        spinnerMode = findViewById(R.id.spinnerMode);

        // Set up spinners
        ArrayAdapter<CharSequence> transferAdapter = ArrayAdapter.createFromResource(
                this, R.array.transfer_types, android.R.layout.simple_spinner_item);
        transferAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransferType.setAdapter(transferAdapter);

        ArrayAdapter<CharSequence> modeAdapter = ArrayAdapter.createFromResource(
                this, R.array.mode_types, android.R.layout.simple_spinner_item);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(modeAdapter);

        // Set up buttons
        setUpButtons();
    }

    private void setUpButtons() {
        findViewById(R.id.btnSetLogger).setOnClickListener(v -> new SetLoggerTask().execute(
                etLogFileName.getText().toString(), etLogLevel.getText().toString()));
        findViewById(R.id.btnStreamOut).setOnClickListener(v -> new StreamOutTask().execute(
                etStreamText.getText().toString(), etStreamFileName.getText().toString()));
        findViewById(R.id.btnStreamIn).setOnClickListener(v -> new StreamInTask().execute(
                etStreamFileName.getText().toString()));
        findViewById(R.id.btnSetTransfer).setOnClickListener(v -> new SetTransferTask().execute(
                spinnerTransferType.getSelectedItem().toString()));
        findViewById(R.id.btnSetMode).setOnClickListener(v -> new SetModeTask().execute(
                spinnerMode.getSelectedItem().toString()));
    }

    private class SetLoggerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            ftpClientHelper.setLogger(params[0], params[1]);
            return "Logger set";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(AdvancedSettingsActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    private class StreamOutTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            ftpClientHelper.streamingOut(params[0], params[1]);
            return "Streamed out data";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(AdvancedSettingsActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    private class StreamInTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            return ftpClientHelper.streamingIn(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            TextView tvResult = findViewById(R.id.tvResult);
            tvResult.setText(result);
        }
    }

    private class SetTransferTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            ftpClientHelper.setTransfer(params[0]);
            return "Transfer mode set to " + params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(AdvancedSettingsActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    private class SetModeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            ftpClientHelper.setMode(params[0]);
            return "Mode set to " + params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(AdvancedSettingsActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }
}