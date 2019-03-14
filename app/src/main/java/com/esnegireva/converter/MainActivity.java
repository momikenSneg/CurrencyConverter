package com.esnegireva.converter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.esnegireva.converter.api.RetrofitProvider;
import com.esnegireva.converter.cache.Cache;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    private Cache cache = new Cache();
    private Call<String> savedCall;   //saved call to cancel the request if the data has changed before it ends
    private boolean canSend = true;   //flag to check if we can send another request to the server

    private Spinner currencyFrom;
    private Spinner currencyTo;
    private EditText editInput;
    private TextView textResult;

    //keys to save/get fields values while rotate the screen
    private static final String KEY_FROM = "FROM";
    private static final String KEY_TO = "TO";
    private static final String KEY_INPUT = "INPUT";
    private static final String KEY_OUTPUT = "OUTPUT";

    private static final int REQUEST_DELAY = 1000*60*5; // 5 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Конвертер валют");

        currencyFrom = findViewById(R.id.currencyFrom);
        currencyTo = findViewById(R.id.currencyTo);
        editInput = findViewById(R.id.input);
        textResult = findViewById(R.id.result);
        /*
        In case of screen rotation if savedInstanceState != null
        restore fields values
         */
        if (savedInstanceState != null) {
            currencyFrom.setSelection(savedInstanceState.getInt(KEY_FROM));
            currencyTo.setSelection(savedInstanceState.getInt(KEY_TO));
            editInput.setText(savedInstanceState.getString(KEY_INPUT));
            textResult.setText(savedInstanceState.getString(KEY_OUTPUT));
        }
        /*
         Timer allows to send requests not less than 5 minutes except for lack of data in the cache
         For two reasons:
         1. traffic saving (consider that the data in the cache will be relevant for 5 minutes)
         2. server does not allow frequent requests, starts sending status code 403
        */
        Timer timer = new Timer(true);
        timer.schedule(new MyTimer(), REQUEST_DELAY, REQUEST_DELAY);

        editInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {     // if changed the input field automatically change the output field
                if (currencyFrom.getSelectedItem() != null && currencyTo.getSelectedItem() != null) {
                    final String from = currencyFrom.getSelectedItem().toString();
                    final String to = currencyTo.getSelectedItem().toString();
                    changeValue(from, to);
                }
            }
        });
    }
    /*
     override the method that is called before onPause()... when the screen is rotated
     to save fields values
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_FROM, currencyFrom.getSelectedItemPosition());
        outState.putInt(KEY_TO, currencyTo.getSelectedItemPosition());
        outState.putString(KEY_INPUT, editInput.getText().toString());
        outState.putString(KEY_OUTPUT, textResult.getText().toString());
    }

    private void changeValue(String from, String to) {
        if (from.equals(to)) {                                   //If the currencies match, just duplicate the value
            EditText editInput = findViewById(R.id.input);
            TextView textResult = findViewById(R.id.result);
            textResult.setText(editInput.getText().toString());
        } else {
            String fromCache = cache.get(MainActivity.this, from + "_" + to);
            if (canSend || fromCache.equals("")) {               //If we can send next (5 minutes passed) request or there is no data in cache
                                                                 // we send next request
                canSend = false;
                sendRequest(from, to);
            } else {                                             //Otherwise just get data from cache
                String getCache = cache.get(MainActivity.this, from + "_" + to);
                outputValue(Double.valueOf(getCache));
            }

        }
    }

    private void sendRequest(final String from, final String to){
        if (savedCall != null) {    // Cancel previous request
                                    //if it fulfilled nothing will happen
            savedCall.cancel();
        }

        //Accessing the request interface
        savedCall = RetrofitProvider.getCurrencyApi().getConvert(from + "_" + to + "," + to + "_" + from, "ultra");    // FROM_TO,TO_FROM

        savedCall.enqueue(new Callback<String>() {   //asynchronous call
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.code() == 200) {
                    outputValue(parseJson(response.body()));
                } else if (response.code() == 403) {
                    showToast("СЛИШКОМ ЧАСТЫЕ ЗАПРОСЫ. ПОПРОБУЙТЕ ПОЗДНЕЕ");
                } else {
                    showToast("ОШИБКА СОЕДИНЕНИЯ");
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (!call.isCanceled()) {
                    String fromCache = cache.get(MainActivity.this, from + "_" + to);
                    if (!fromCache.equals("")) {
                        outputValue(Double.valueOf(fromCache));
                        showToast("ДАННЫЕ ВЗЯТЫ ИЗ КЭША. ДЛЯ БОЛЕЕ ТОЧНЫХ ЗНАЧЕНИЙ ПРОВЕРЬТЕ СОЕДИНЕНИЕ С ИНТЕРНЕТОМ");
                    } else {
                        showToast("ПРОВЕРЬТЕ СОЕДИНЕНИЕ С ИНТЕРНЕТОМ. ДАННЫХ В КЭШЕ НЕТ");
                    }
                }
            }
        });
    }

    //display a new value depending on the coefficient
    private void outputValue(Double coef) {
        EditText editText = findViewById(R.id.input);
        double input;

        try {
            input = Double.valueOf(editText.getText().toString());
            if(input < 0){
                showToast("НЕКОРРЕКТНЫЕ ДАННЫЕ");
            } else {
                TextView textResult = findViewById(R.id.result);
                Double result = coef * input;
                BigDecimal a = new BigDecimal(Double.toString(result));
                BigDecimal roundOff = a.setScale(2, BigDecimal.ROUND_HALF_EVEN);
                textResult.setText(roundOff.toString());
            }
        } catch (NumberFormatException e) {
            textResult.setText("0");
        }
    }

    private void showToast(String message) {
        Toast toast = Toast.makeText(getApplicationContext(),
                message, Toast.LENGTH_SHORT);
        toast.show();
    }
    /*
      Parsing of json "manually"
      Using automatic parsers seemed impractical because of the provided api
     */
    private double parseJson(String json) {
        String[] two = json.split(",");
        String[] one = two[0].split(":");
        String key = one[0].replaceAll("\"", "").replaceAll("\\{", "");
        cache.put(this, key, one[1]);
        String ret = one[1];

        one = two[1].split(":");
        key = one[0].replaceAll("\"", "");
        cache.put(this, key, one[1].replaceAll("\\}", ""));

        return Double.valueOf(ret);
    }

    //Gives permission to send a request every 5 minutes
    private class MyTimer extends TimerTask {
        @Override
        public void run() {
            canSend = true;
        }
    }
}
