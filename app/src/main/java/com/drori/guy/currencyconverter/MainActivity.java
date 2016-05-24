package com.drori.guy.currencyconverter;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private Spinner source_spinner;
    private Spinner dest_spinner;
    private List<String> options;
    private List<String> optionsCopy;
    private ArrayAdapter<String> destListAdapter;
    private String option1;
    private String option2;
    private HashMap ratesMap = new HashMap<>();
    private IOManager file;
    static MainActivity main;
    private String setCurrencyString(String choice) {
        //Method to ensure consistency with strings for later checking and printing
        String outcome= "";
        if (choice.contains("EUR")) {
            outcome = "EUR";
        } else if (choice.contains("PLN")) {
            outcome = "PLN";
        } else if (choice.contains("USD")) {
            outcome = "USD";
        } else if (choice.contains("GBP")){
            outcome = "GBP";
        } else if (choice.contains("ILS")) {
            outcome = "ILS";
        } else if (choice.contains("CNY")) {
            outcome = "CNY";
        }

        return outcome;
    }
    public void buttonClick (View view) {
        EditText inputField = (EditText)findViewById(R.id.value_input);
        try {
            //Prepping for calculation and display
            double input = Double.parseDouble(inputField.getText().toString());
            double result = convertCurrency(input, option1, option2);
            DecimalFormat formatter = new DecimalFormat("#0.00");
            EditText resultField = (EditText) findViewById(R.id.result_field);
            resultField.setText(formatter.format(result) + " " + option2);
        } catch (Exception e) {
            Toast.makeText(this.getApplicationContext(), R.string.noInput, Toast.LENGTH_LONG).show();
        }
    }
    private double convertCurrency(double input, String option1, String option2) {
        //Calculation method:
        if (option1.equals(option2)) {
            //Unnecessary at the moment but kept because of the possibility that it might happen if something goes wrong.
            Toast.makeText(getApplicationContext(), R.string.currencyCollision,Toast.LENGTH_LONG).show();
            return input;
       } else {
           if (option1.equals("EUR")) {
               double rate = getCurrencyRate(option2);
               return input * rate;
           } else {
               if (option2.equals("EUR")) {
                   double rate = getCurrencyRate(option1);
                   return input / rate;
               } else {
                   double sourceEUR = input / getCurrencyRate(option1);
                   return sourceEUR * getCurrencyRate(option2);
               }
           }
       }
    }
    private class DownloadTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection;
            try {
                url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                Log.i("Data", "Read successfully");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.dlRatesSuccess,Toast.LENGTH_LONG).show();
                    }
                });
                return result;
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.dlRatesFail, Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }
        @Override
        protected void onPostExecute (String result) {
            super.onPostExecute(result);
            try {
                JSONObject jsonObject = new JSONObject(result);
                String rates= jsonObject.getString("rates");
                Log.i("JSON", "String accepted");
                JSONObject data = jsonObject.getJSONObject("rates");
                ratesMap.put("USD", data.getDouble("USD"));
                ratesMap.put("GBP", data.getDouble("GBP"));
                ratesMap.put("ILS", data.getDouble("ILS"));
                ratesMap.put("CNY", data.getDouble("CNY"));
                ratesMap.put("PLN", data.getDouble("PLN"));
                Log.i("JSON", "Rates processed successfully");
                file.rewriteAndSave(ratesMap);
                Log.i("I/O", "Map saved succesfully");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(
                                getApplicationContext(), R.string.updateRatesSuccess, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.processRatesFailed, Toast.LENGTH_SHORT).show();
                    }
                });
                readFile(file);
            }
        }
    }
    @Override
    public void onSaveInstanceState (Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("ran", true);
        savedInstanceState.putSerializable("rates", ratesMap);
        savedInstanceState.putSerializable("optionList", (Serializable) options);
    }
    private Bundle saveState() {
        //Allows rotation and task switching.
        Bundle save = new Bundle();
        save.putBoolean("ran", true);
        save.putSerializable("rates", ratesMap);
        save.putSerializable("optionList", (Serializable) options);
        return save;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        main = MainActivity.this;
        file = new IOManager("data.dat");
        Resources res = getResources();
        String [] currencyNames = res.getStringArray(R.array.currencies);
        if (savedInstanceState == null) {
            if (checkInternetConnectivity()) {
                DownloadTask task = new DownloadTask();
                task.execute("http://api.fixer.io/latest");
            }else {
                readFile(file);
            }
            options = new ArrayList<String>();
            options.add(currencyNames[0]);
            options.add(currencyNames[1]);
            options.add(currencyNames[2]);
            options.add(currencyNames[3]);
            options.add(currencyNames[4]);
            options.add(currencyNames[5]);
            optionsCopy = new ArrayList<String>();
            savedInstanceState = saveState();
            onSaveInstanceState(savedInstanceState);
        } else {
            options = (ArrayList<String>)savedInstanceState.getSerializable("optionList");
            ratesMap = (HashMap) savedInstanceState.getSerializable("rates");
            optionsCopy = new ArrayList<String>();
        }
        source_spinner = (Spinner) findViewById(R.id.source_chooser);
        dest_spinner = (Spinner) findViewById(R.id.destination_chooser);
        ArrayAdapter<String> srcListAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, options);
        srcListAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        OnItemSelectedListener srcSpinnerListener = new OnItemSelectedListener() { //Listener for the source currency spinner
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                optionsCopy.clear();
                optionsCopy.addAll(options);
                destListAdapter.notifyDataSetChanged();
                optionsCopy.remove(position);
                destListAdapter.notifyDataSetChanged();
                option1 = setCurrencyString(options.get(position));
                dest_spinner.setSelection(0);
                option2 = setCurrencyString(optionsCopy.get(0));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Just a required function. Not useful at the moment.
            }
        };
        source_spinner.setAdapter(srcListAdapter);
        source_spinner.setOnItemSelectedListener(srcSpinnerListener);
        destListAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, optionsCopy);
        destListAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        OnItemSelectedListener destSpinnerListener = new OnItemSelectedListener() { //Listener for the destination currency spinner
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                option2= setCurrencyString(optionsCopy.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Just a required function. Not useful at the moment.
            }
        };
        dest_spinner.setAdapter(destListAdapter);
        dest_spinner.setOnItemSelectedListener(destSpinnerListener);

        /*
            Different listeners are necessary for the implementation of changing lists (e.g. when you select a source currency remove that currency from destinations)
         */



    }
    private boolean checkInternetConnectivity() {
        ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    private void loadDefaultRates() {
        //This only occurs if there is no Internet connection and if no cached rates are avilable.
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setMessage(R.string.dialogContent);
        dialogBuilder.setPositiveButton(R.string.dialogPositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Do nothing...
            }
        });
        dialogBuilder.setNegativeButton(R.string.dialogNegative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        dialogBuilder.create().show();
        ratesMap.put("USD", 1.1); ratesMap.put("GBP", 0.79); ratesMap.put("ILS",4.3);
        ratesMap.put("CNY", 7.2); ratesMap.put("PLN", 4.4);
        Toast.makeText(getApplicationContext(),R.string.defaultRates,Toast.LENGTH_LONG).show();
    }
    private void readFile(IOManager file) {
        if (file.readable()) {
            try {
                file.readFile();
                Date updateDate = file.getDate();
                ratesMap = file.getRatesMap();
                String toastStatement = getString(R.string.fileReadSuccess);
                String downloaded = getString(R.string.downloaded);
                Toast.makeText(getApplicationContext(),toastStatement + "\n" + downloaded + "\n" + updateDate,Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                loadDefaultRates();
            }
        } else {
            loadDefaultRates();
        }
    }
    private double getCurrencyRate (String option) {
        double rate;
        if (ratesMap.get(option) == null) {
            rate = 0;
            Toast.makeText(getApplicationContext(), R.string.calcFail, Toast.LENGTH_LONG).show();
        } else {
            rate = (double) ratesMap.get(option);
        }
        return rate;
    }



}
