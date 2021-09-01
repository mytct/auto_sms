package com.mytran.myapplication.ui.main

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.mytran.myapplication.R
import com.mytran.myapplication.ui.base.CoreFragment
import com.mytran.myapplication.ui.base.TypeCoreAction
import com.mytran.myapplication.ui.main.services.AutoSmsService
import com.mytran.myapplication.utils.launchPeriodicAsync
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.system.exitProcess


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
        stopService()
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
        //initSMSListener()
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
        //job.cancel()
//        activity?.unregisterReceiver(receiver)
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
            startService()
            activity?.finish()
            exitProcess(0)
        }
//        val status = isMyServiceRunning(AutoSmsService::class.java)
//        if(status) btnStart?.setText(R.string.running)
//        else btnStart?.setText(R.string.start)
//
//        btnStart?.setOnClickListener {
//            if(!status) {
//                btnStart?.setText(R.string.running)
//                startService()
//            }
//            else {
//                btnStart?.setText(R.string.start)
//                activity?.stopService(Intent(activity, AutoSmsService::class.java))
//            }
//        }
    }

    private fun startService() {
        activity?.startService(Intent(activity, AutoSmsService::class.java))
    }

    private fun stopService() {
        activity?.stopService(Intent(activity, AutoSmsService::class.java))
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

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        manager?.run {
            for (service in getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }
}