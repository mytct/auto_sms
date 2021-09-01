package com.mytran.myapplication.ui.main.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.SmsMessage
import android.util.Log
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mytran.myapplication.BuildConfig
import com.mytran.myapplication.api.request.CoinServices
import com.mytran.myapplication.api.response.CoinResponse
import com.mytran.myapplication.ui.main.MainViewModel
import com.mytran.myapplication.ui.main.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException


class AutoSmsService: Service() {
    private lateinit var userPreferencesFlow: Flow<UserPreferences>
    private val USER_PREFERENCES_NAME = "sms_auto_preferences"

    private val Context.dataStore by preferencesDataStore(
        name = USER_PREFERENCES_NAME
    )

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onCreate() {
        super.onCreate()
        Log.d("TEST","AutoSmsService onCreate")
        initSMSListener()
    }
    private var receiver: BroadcastReceiver? = null
    private var retry = 0
    private object PreferencesKeys {
        val api_ref = stringPreferencesKey("api_ref")
        val token_ref = stringPreferencesKey("token_ref")
    }
    private val jobCoroutine = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + jobCoroutine)
    private lateinit var service: CoinServices

    private fun initSMSListener() {
        val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        if(receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.d("MainViewModel", "sendContentSms init")
                    val sms_extra = "pdus"
                    intent?.extras?.let {
                        Log.d("MainViewModel", "sendContentSms bundle= $it")
                        (it[sms_extra] as? Array<Any>)?.run {
                            Log.d("MainViewModel", "sendContentSms sms_extra= ${indices}")
                            for (i in indices) {
                                //lệnh chuyển đổi về tin nhắn createFromPdu
                                val smsMsg: SmsMessage = SmsMessage.createFromPdu(this[i] as ByteArray)
                                //lấy nội dung tin nhắn
                                val body: String = smsMsg.messageBody
                                //lấy số điện thoại tin nhắn
                                val address: String = smsMsg.displayOriginatingAddress
                                uiScope.launch { sendToServer(address, body) }
                            }
                        }
                    }
                }
            }
        }
        registerReceiver(receiver, filter)
        userPreferencesFlow = dataStore.data
            .catch { exception ->
                // dataStore.data throws an IOException when an error is encountered when reading data
                Log.d("MainViewModel", "getDefaultValues exception= $exception")
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                // Get our show completed value, defaulting to false if not set:
                var api = preferences[PreferencesKeys.api_ref] ?: BuildConfig.BASE_URL
                var token = preferences[PreferencesKeys.token_ref] ?: "q4Sb^@^Jdp6>NEO%?7Ts@dLnJ-6KxQbU}\$+h]EhUW*g>dJOfkdnYFbN"
                Log.d("MainViewModel", "getDefaultValues api= $api token= $token")
                if(api.isBlank()) api = BuildConfig.BASE_URL
                if(token.isBlank()) token = "q4Sb^@^Jdp6>NEO%?7Ts@dLnJ-6KxQbU}\$+h]EhUW*g>dJOfkdnYFbN"
                UserPreferences(api, token)
            }
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging) // <-- this is the important line!

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build()
        service = retrofit.create(CoinServices::class.java)
    }
    private suspend fun sendToServer(address: String, body: String) {
        userPreferencesFlow.collect {
            val headerMap = mutableMapOf<String, String>()
            headerMap["token"] = it.token
            Log.d("TEST","AutoSmsService token= ${it.token} api= ${it.api}")
            val call = service.postSmsContent2(it.api, headerMap, address, body)
            call.enqueue(object : Callback<CoinResponse> {
                override fun onResponse(call: Call<CoinResponse>, response: Response<CoinResponse>) {
                    Log.d("TEST","AutoSmsService onResponse")
                    if(!response.isSuccessful || response.body()?.code != 200) {
                        Log.d("TEST","AutoSmsService onResponse not successfully")
                        if(retry < 5) {
                            uiScope.launch { sendToServer(address, body) }
                            retry++
                        }
                    } else retry = 0
                }

                override fun onFailure(call: Call<CoinResponse>, t: Throwable) {
                    Log.d("TEST","AutoSmsService onFailure")
                    retry = 0
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TEST","AutoSmsService onDestroy")
        unregisterReceiver(receiver)
    }
}