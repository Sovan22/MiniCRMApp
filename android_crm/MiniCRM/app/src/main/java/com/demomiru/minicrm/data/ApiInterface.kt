package com.demomiru.minicrm.data

import com.demomiru.minicrm.data.models.CustomerEntity
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface CustomerApi {
    @GET("customers")
    suspend fun getCustomers(): List<CustomerEntity>
}

val retrofit = Retrofit.Builder()
    .baseUrl("http://127.0.0.1:8080/") // local mock server
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(CustomerApi::class.java)
