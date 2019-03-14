package com.esnegireva.converter.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

//creates and provides a request interface
public class RetrofitProvider {

    private static CurrencyApi currencyApi;

    static {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://free.currencyconverterapi.com")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        currencyApi = retrofit.create(CurrencyApi.class);
    }

    public static CurrencyApi getCurrencyApi() {
        return currencyApi;
    }
}
