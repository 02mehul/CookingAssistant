package com.example.cookingassistant.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static MealApi API;
    public static MealApi get() {
        if (API == null) {
            API = new Retrofit.Builder()
                    .baseUrl("https://www.themealdb.com/api/json/v1/1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(MealApi.class);
        }
        return API;
    }
}
