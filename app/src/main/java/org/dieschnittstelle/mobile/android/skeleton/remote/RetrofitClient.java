package org.dieschnittstelle.mobile.android.skeleton.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance = null;
    private ITodoAPIService apiService;

    private RetrofitClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080") // Ersetze durch die URL deiner Webanwendung
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ITodoAPIService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null)
            instance = new RetrofitClient();
        return instance;
    }

    public ITodoAPIService getApiService() {
        return apiService;
    }
}