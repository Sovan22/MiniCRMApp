package com.demomiru.minicrm.data

import android.content.Context
import fi.iki.elonen.NanoHTTPD

class MockApiServer(private val context: Context) : NanoHTTPD(8080) {

    override fun serve(session: IHTTPSession?): Response {
        return when (session?.uri) {
            "/customers" -> {
                val json = context.assets.open("customers.json")
                    .bufferedReader()
                    .use { it.readText() }

                newFixedLengthResponse(Response.Status.OK, "application/json", json)
            }
            else -> {
                newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
            }
        }
    }
}
