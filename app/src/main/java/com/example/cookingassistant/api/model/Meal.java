package com.example.cookingassistant.api.model;

public class Meal {
    public String idMeal;
    public String strMeal;
    public String strCategory;
    public String strInstructions;
    public String strMealThumb;

    // Ingredients are strIngredient1..20 and strMeasure1..20 in API;
    // For brevity we’ll read instructions & image; details screen can list a few.
    public String strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5;
    public String strMeasure1, strMeasure2, strMeasure3, strMeasure4, strMeasure5;
}
