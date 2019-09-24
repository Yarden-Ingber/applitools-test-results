package com.yarden.restServiceDemo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;

public class SheetDBApiService {

    private static SheetService sheetApiService = null;
    private static final String apiKey = "ux9w3vd819op9";

    public static SheetService getService(){
        if (sheetApiService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://sheetdb.io/api/v1/" + apiKey + "/")
                    .addConverterFactory(GsonConverterFactory.create(new Gson()))
                    .build();
            sheetApiService = retrofit.create(SheetService.class);
        }
        return sheetApiService;
    }

    public interface SheetService {
        @GET(".")
        Call<JsonArray> getAllSheet();

        @POST(".")
        Call<JsonObject>  updateSheet(@Body JsonObject jsonObject);

        @DELETE("all")
        Call<JsonObject> deleteEntireSheet();
    }

}
