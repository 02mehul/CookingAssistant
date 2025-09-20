package com.example.cookingassistant.api;

import com.example.cookingassistant.api.model.MealResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MealApi {
    // Search by name, e.g., s=chicken
    @GET("search.php")
    Call<MealResponse> search(@Query("s") String query);

    // Lookup by id
//    @GET("lookup.php")
//    Call<MealResponse> lookup(@Query("i") String id);

    @GET("random.php")
    Call<MealResponse> random();

    @GET("lookup.php")
    Call<MealResponse> lookup(@Query("i") String idMeal);
}
