package com.mytran.myapplication.ui.main

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mytran.myapplication.api.repository.CoinRepository
import com.mytran.myapplication.api.response.CoinResponse
import com.mytran.myapplication.ui.main.data.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class MainViewModel(private val dataStore: DataStore<Preferences>, private val coinRepository: CoinRepository) : ViewModel() {
    private val defaultToken = "q4Sb^@^Jdp6>NEO%?7Ts@dLnJ-6KxQbU}\$+h]EhUW*g>dJOfkdnYFbN"
    private val defaulHost = "http://job.delivn.vn/api/sms_content"
    var response = MutableLiveData<CoinResponse>()
    lateinit var userPreferencesFlow: Flow<UserPreferences>
    private object PreferencesKeys {
        val api_ref = stringPreferencesKey("api_ref")
        val token_ref = stringPreferencesKey("token_ref")
    }

    suspend fun getDefaultValues() {
        Log.d("MainViewModel", "getDefaultValues init")
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
                var api = preferences[PreferencesKeys.api_ref] ?: defaulHost
                var token = preferences[PreferencesKeys.token_ref] ?: defaultToken
                Log.d("MainViewModel", "getDefaultValues api= $api token= $token")
                if(api.isBlank()) api = defaulHost
                if(token.isBlank()) token = defaultToken
                UserPreferences(api, token)
            }
    }

    suspend fun saveNewData(api: String, token: String) {
        dataStore.edit {
            Log.d("MainViewModel", "saveNewData api= $api token= $token")
            it[PreferencesKeys.api_ref] = api
            it[PreferencesKeys.token_ref] = token
        }
    }

    suspend fun sendContentSms(url: String, token: String, phone: String, content: String, retry: ()-> Unit) {
        try {
            Log.d("MainViewModel", "sendContentSms url= $url token= $token phone= $phone content= $content")

            val headerMap = mutableMapOf<String, String>()
            headerMap["token"] = token
//            Log.d("MainViewModel", "sendContentSms headerMap ${headerMap}")
            val result = coinRepository.postSmsContent(url, headerMap, phone, content)
            Log.d("MainViewModel", "sendContentSms ${result.code}")
            if(result.code != 200) {
                retry.invoke()
            } else response.postValue(result)
        } catch (throwable: Throwable) {
            Log.d("MainViewModel", "sendContentSms throwable ${throwable.message}")
            //init empty values
            retry.invoke()
        }
    }

    private fun getHeaderMap(callback: (headerMap: Map<String, String>)-> Unit) {
        dataStore.data
            .map { preferences ->
                // Get our show completed value, defaulting to false if not set:
                val headerMap = mutableMapOf<String, String>()
                headerMap["token"] = preferences[PreferencesKeys.token_ref] ?: defaultToken
                callback(headerMap)
            }
    }
}