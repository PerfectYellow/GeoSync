package com.example.geosync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.geosync.network.androidContext
import okhttp3.OkHttpClient
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.module.http.HttpRequestUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        androidContext = applicationContext
        AndroidGraphicFactory.createInstance(application)

        // Initialize MapLibre FIRST with dummy key and MapLibre tile server
        // This satisfies the internal validation check
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)

        // Add Map.ir API Key to all requests via OkHttpClient interceptor
        val apiKey = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImp0aSI6IjhlZDM2MDA0ZDdkNjEwZjgzZGUxNjE1ZGYxZjY2OWU0OTczMjc2NTA3YjU2Mjk2YzY3ZGRiZTE1ZmI3NDY4ZmNkZjgwOGRhOTRlN2FlYTg1In0.eyJhdWQiOiI0Mjg0OCIsImp0aSI6IjhlZDM2MDA0ZDdkNjEwZjgzZGUxNjE1ZGYxZjY2OWU0OTczMjc2NTA3YjU2Mjk2YzY3ZGRiZTE1ZmI3NDY4ZmNkZjgwOGRhOTRlN2FlYTg1IiwiaWF0IjoxNzg0MzExMzE5LCJuYmYiOjE3ODQzMTEzMTksImV4cCI6MTc4NjkwMzMxOSwic3ViIjoiIiwic2NvcGVzIjpbImJhc2ljIl19.bhCD89v-9X2jid8LPTzMMfXQr_xvz14KoovAZ7n_bIeoNoFB1wrup9xKH9ohEP-27U10KwPGftlqr_ZD9IYd2nN--nwywh21LFHYq8_o5jOueZADG011X4KoZQKLIWQalJvr3DdPnbuBeHBO5G_Xy-siHZmAvlI05fD0qXncl1coY8f-i7bG2sibu5mx2fGYYvLglXPgDfXK4afLwFNGUlYa7kiPF8BzvRzNT_Ila87oZLoOAOhoSayyD3XGk8CZnYLKvqIpnorwWMvvnviZFqYbWW0NCewRc2r7JuCl-k653f2J6pcq54OIhAEtmz0WPzA5jd53CYnKA5d_s4Ur1w"
        
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("x-api-key", apiKey)
                    .header("Mapir-SDK", "android")
                    .method(original.method, original.body)
                chain.proceed(requestBuilder.build())
            }
            .build()

        HttpRequestUtil.setOkHttpClient(client)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}