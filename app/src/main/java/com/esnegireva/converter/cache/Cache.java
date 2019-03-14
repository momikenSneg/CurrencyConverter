package com.esnegireva.converter.cache;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class Cache {
    private SharedPreferences sPref;
    public String get(Context context,String key){
        sPref = context.getSharedPreferences("MyPref", MODE_PRIVATE);
        return sPref.getString(key, "");
    }

    public void put(Context context, String key, String value){
        sPref = context.getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(key, value);
        ed.apply();
    }
}
