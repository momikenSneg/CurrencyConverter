package com.esnegireva.converter.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CurrencyApi {
    @GET("/api/v6/convert")
    Call<String> getConvert(@Query("q") String q, @Query("compact") String compact);
}
