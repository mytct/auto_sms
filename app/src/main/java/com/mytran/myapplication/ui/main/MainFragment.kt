package com.mytran.myapplication.ui.main

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import com.mytran.myapplication.R
import com.mytran.myapplication.api.response.CoinResponse
import com.mytran.myapplication.ui.base.CoreFragment
import com.mytran.myapplication.ui.base.TypeCoreAction
import com.mytran.myapplication.utils.launchPeriodicAsync
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainFragment : CoreFragment() {
    private lateinit var token: String
    private lateinit var url: String
    private val jobCoroutine = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + jobCoroutine)
    companion object {
        fun newInstance() = MainFragment()
    }
    private lateinit var job: Deferred<Unit>
    private var receiver: BroadcastReceiver? = null
    private val homeViewModel: MainViewModel by viewModel()
    override fun getLayoutId(): Int = R.layout.main_fragment
    override fun defaultData() {
    }

    override fun initObserver() {
        uiScope.launch {
            homeViewModel.getDefaultValues()
            homeViewModel.userPreferencesFlow.collect {
                Log.d("MainViewModel", "getDefaultValues collect api= ${it.api} token= ${it.token}")
                edtApi.setText(it.api)
                edtToken.setText(it.token)
                token = it.token
                url = it.api
            }
        }
        homeViewModel.response.observe(viewLifecycleOwner, {
            //Toast.makeText(context, R.string.lb_send_success, Toast.LENGTH_SHORT).show()
        })
        initSMSListener()
    }

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
                                uiScope.launch { homeViewModel.sendContentSms(url, token, address, body) {
                                    uiScope.launch {
                                        homeViewModel.sendContentSms(url, token, address, body){}
                                    }
                                }}
                            }
                        }
                    }
                }
            }
        }
        activity?.registerReceiver(receiver, filter)
    }

    private fun initIntervalFetch() {
        Log.v("MainFragment", "initIntervalFetch resume")
        job = CoroutineScope(Dispatchers.IO).launchPeriodicAsync(1000) {
            Log.v("MainFragment", "initIntervalFetch start")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jobCoroutine.cancel()
        job.cancel()
        activity?.unregisterReceiver(receiver)
    }

    override fun onPause() {
        Log.v("MainFragment", "initIntervalFetch stop")
        super.onPause()
        //job.cancel()
    }

    override fun refreshLayout() {
        btnClear?.setOnClickListener {
            edtApi?.setText("")
        }
        btnClearToken?.setOnClickListener {
            edtToken?.setText("")
        }
        btnSave?.setOnClickListener {
            token = edtToken.text.toString()
            url = edtApi.text.toString()
            uiScope.launch { homeViewModel.saveNewData(edtApi.text.toString(), edtToken.text.toString()) }
            Toast.makeText(context, R.string.lb_save_success, Toast.LENGTH_SHORT).show()
        }
//        btnStart?.setOnClickListener {
//            initIntervalFetch()
//        }
    }

    override fun doingActionClick(data: Any?, action: String) {
        data?.apply {
            when(action) {
                TypeCoreAction.ADD_COIN-> {

                }
                TypeCoreAction.REMOVE_COIN-> {

                }
            }
        }
    }
}