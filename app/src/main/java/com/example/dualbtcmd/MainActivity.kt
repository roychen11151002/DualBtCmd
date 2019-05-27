package com.example.dualbtcmd

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.os.SystemClock.sleep
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

const val KotlinLog = "kotlinTest"
const val CmdHff = 0xff.toByte()
const val CmdHd55 = 0x55.toByte()
const val CmdDevHost = 0x80.toByte()
const val CmdDevSrc = 0x30.toByte()
const val CmdDevAg = 0x00.toByte()

enum class CmdId(val value: Byte) {
    SET_HFP_PAIR_REQ(0x02.toByte()),
    SET_HFP_PAIR_RSP(0x03.toByte()),
    SET_SRC_VOL_REQ(0x04.toByte()),
    SET_SRC_VOL_RSP(0x05.toByte()),
    SET_AG_VOL_REQ(0x06.toByte()),
    SET_AG_VOL_RSP(0x07.toByte()),
    SET_HFP_VOL_REQ(0x08.toByte()),
    SET_HFP_VOL_RSP(0x09.toByte()),
    SET_HFP_STA_REQ(0x0a.toByte()),
    SET_HFP_STA_RSP(0x0b.toByte()),
    SET_HFP_EXT_STA_REQ(0x0c.toByte()),
    SET_HFP_EXT_STA_RSP(0x0d.toByte()),
    SET_HFP_SPKEY_REQ(0x10.toByte()),
    SET_HFP_SPKEY_RSP(0x11.toByte()),
    SET_AG_SPKEY_REQ(0x12.toByte()),
    SET_AG_SPKEY_RSP(0x13.toByte()),
    SET_HFP_LOCAL_NAME_REQ(0x14.toByte()),
    SET_HFP_LOCAL_NAME_RSP(0x15.toByte()),
    SET_AG_LOCAL_NAME_REQ(0x16.toByte()),
    SET_AG_LOCAL_NAME_RSP(0x17.toByte()),
    SET_HFP_FEATURE_REQ(0x1c.toByte()),
    SET_HFP_FEATURE_RSP(0x1d.toByte()),
    SET_AG_FEATURE_REQ(0x1e.toByte()),
    SET_AG_FEATURE_RSP(0x1f.toByte()),
    SET_DFU_REQ(0x3e.toByte()),
    SETDFU_RSP(0x3f.toByte()),

    GET_HFP_PAIR_REQ(0x42.toByte()),
    GET_HFP_PAIR_RSP(0x43.toByte()),
    GET_SRC_VOL_REQ(0x44.toByte()),
    GET_SRC_VOL_RSP(0x45.toByte()),
    GET_AG_VOL_REQ(0x46.toByte()),
    GET_AG_VOL_RSP(0x47.toByte()),
    GET_HFP_VOL_REQ(0x48.toByte()),
    GET_HFP_VOL_RSP(0x49.toByte()),
    GET_HFP_STA_REQ(0x4a.toByte()),
    GET_HFP_STA_RSP(0x4b.toByte()),
    GET_HFP_EXT_STA_REQ(0x4c.toByte()),
    GET_HFP_EXT_STA_RSP(0x4d.toByte()),
    GET_SRC_DEV_NO_REQ(0x4e.toByte()),
    GET_SRC_DEV_NO_RSP(0x4f.toByte()),
    GET_HFP_SPKEY_REQ(0x50.toByte()),
    GET_HFP_SPKEY_RSP(0x51.toByte()),
    GET_AG_SPKEY_REQ(0x52.toByte()),
    GET_AG_SPKEY_RSP(0x53.toByte()),
    GET_HFP_LOCAL_NAME_REQ(0x54.toByte()),
    GET_HFP_LOCAL_NAME_RSP(0x55.toByte()),
    GET_AG_LOCAL_NAME_REQ(0x56.toByte()),
    GET_AG_LOCAL_NAME_RSP(0x57.toByte()),
    GET_HFP_VRESION_REQ(0x58.toByte()),
    GET_HFP_VRESION_RSP(0x59.toByte()),
    GET_AG_VRESION_REQ(0x5a.toByte()),
    GET_AG_VRESION_RSP(0x5b.toByte()),
    GET_HFP_FEATURE_REQ(0x5c.toByte()),
    GET_HFP_FEATURE_RSP(0x5d.toByte()),
    GET_AG_FEATURE_REQ(0x5e.toByte()),
    GET_AG_FEATURE_RSP(0x5f.toByte())
}

class MainActivity : AppCompatActivity() {
    lateinit var preferData : SharedPreferences
    val BtServiceRequestCode: Int = 3
    // var btBda =  arrayOf("00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00", "00:00:00:00:00:00")
    val maxBtDevice = 2
    var btBda = arrayOfNulls<String>(maxBtDevice)
    lateinit var adapter: BtListAdapter
    var btList = ArrayList<String>()

    var isBtBind = false
    lateinit var btService: Messenger
    private var clientMsgHandler = Messenger(ClientMsgHandler())
    inner class ClientMsgHandler : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            var clientMsg: Message
            var clientBundle: Bundle

            if(msg != null)
            {
                // Log.d(KotlinLog, "client message: ${msg.what}, arg1: ${msg.arg1}, arg2: ${msg.arg2}")
                clientBundle = msg.data
                when(msg.what)
                {
                    0 -> {
                        clientMsg = Message.obtain(null, 1, 0, 0)
                        clientBundle.putStringArray("remoteBda", btBda)
                        clientMsg.data = clientBundle
                        sendMsg(btService, clientMsg)
                        clientMsg = Message.obtain(null, 1, 1, 0)
                        clientMsg.data = clientBundle
                        sendMsg(btService, clientMsg)
                        txvBtState0.text = "Bluetooth connecting"
                        txvBtState1.text = "Bluetooth connecting"
                    }
                    1 -> {
                        if(msg.arg2 == 0) {
                        }
                        when(msg.arg2) {
                            0 -> {
                                when(msg.arg1) {
                                    0 -> {
                                        txvBtState0.text = "Bluetooth connected"
                                        val getDevIdCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevSrc, CmdId.GET_SRC_DEV_NO_REQ.value , 1, 0x00, 0x00)
                                        // sleep(10000);
                                        btCmdSend(getDevIdCmd, 0)
                                        txvFwVer0.text = "none"
                                    }
                                    1 -> txvBtState1.text = "Bluetooth connected"
                                }
                            }
                            1 -> {
                                when(msg.arg1) {
                                    0 -> txvBtState0.text = "Bluetooth disconnect"
                                    1 -> txvBtState1.text = "Bluetooth disconnect"
                                }
                            }
                            2 -> {
                                // val pairedStr = StringBuilder()

                                for(device in clientBundle.getStringArrayList("pairedList"))
                                    btList.add(device)
                                recycleViewDiscovery.layoutManager!!.scrollToPosition(0)
                            }
                            3 -> {
                                val str = clientBundle.getString("discoveryStr")
                                Log.d(KotlinLog, "$str")
                                btList.add(str)
                                recycleViewDiscovery.layoutManager!!.scrollToPosition(0)
                            }
                            4 -> {
                                btnPairedList.isEnabled = true
                                txvDiscoveryStr.text = "find device end"
                            }
                            else -> {}
                        }
                    }
                    2 -> {
                        clientBundle = msg.data
                        val rfcCmd = clientBundle.getByteArray("rfcCmd")
                        rfcCmdParse(rfcCmd, msg.arg1)
                    }
                    else -> Log.d(KotlinLog, "client message other")
                }
            }
        }
    }

    val serviceConn = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val msg = Message.obtain(null, 0, 0, maxBtDevice)

            Log.d(KotlinLog, "onServiceConnected")
            btService = Messenger(service)
            isBtBind = true

            msg.replyTo = clientMsgHandler
            sendMsg(btService, msg)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBtBind = false
            Log.d(KotlinLog, "onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(KotlinLog, "onCreate")
        btInit()
        preferData = getSharedPreferences("iMageBda", Context.MODE_PRIVATE)
        for(i: Int in 0 until  maxBtDevice) {
            btBda[i] = preferData.getString("btBda${i.toString()}", "00:00:00:00:00:00")
            Log.d(KotlinLog, "bluetooth bda$i: ${btBda[i]}")
        }


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        btnGetFwVer0.setOnClickListener {
            // val getFwCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevSrc, CmdId.GET_HFP_PAIR_REQ.value , 0, 0x00)
            val getFwCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevSrc, CmdId.GET_HFP_VRESION_REQ.value , 0, 0x00)

            btCmdSend(getFwCmd, 0)
            txvFwVer0.text = "none"
        }

        btnGetFwVer1.setOnClickListener {
            // val getFwCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevAg, 0x04, 2, 0x04, 0xff.toByte(), 0x00)
            // val getFwCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevAg, CmdId.GET_HFP_PAIR_REQ.value , 0, 0x00)
            val getFwCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevAg, CmdId.GET_AG_VRESION_REQ.value , 0, 0x00)

            // btCmdSend(getFwCmd, 1)
            btCmdSend(getFwCmd, 0)
            txvFwVer1.text = "none"
        }

        btnGetName0.setOnClickListener {
            val getNameCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevSrc, CmdId.GET_HFP_LOCAL_NAME_REQ.value, 0, 0x00)

            btCmdSend(getNameCmd, 0)
            txvGetName0.text = "none"
        }

        btnGetName1.setOnClickListener {
            // val getNameCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevAg, 0x04, 2, 0x04, 0x88.toByte(), 0x00)
            val getNameCmd = byteArrayOf(CmdHff, CmdHd55, CmdDevHost, CmdDevAg, CmdId.GET_AG_LOCAL_NAME_REQ.value, 0, 0x00)

            //btCmdSend(getNameCmd, 1)
            btCmdSend(getNameCmd, 0)
            txvGetName1.text = "none"
        }

        btnPairedList.setOnClickListener {
            val msg = Message.obtain(null, 1, 0, 2)

            sendMsg(btService, msg)
            btList.removeAll(btList)
            recycleViewDiscovery.adapter!!.notifyDataSetChanged()
            recycleViewDiscovery.layoutManager
            txvDiscoveryStr.text = "paired list"
        }

        btnBtDiscovery.setOnClickListener {
            if(btnPairedList.isEnabled) {
                val msg = Message.obtain(null, 1, 0, 3)
                btnPairedList.isEnabled = false
                sendMsg(btService, msg)
                btList.removeAll(btList)
                recycleViewDiscovery.adapter!!.notifyDataSetChanged()
                recycleViewDiscovery.layoutManager
                txvDiscoveryStr.text = "find device ..."
            }
            else {
                val msg = Message.obtain(null, 1, 0, 4)
                sendMsg(btService, msg)
            }
        }

        btncon0.setOnClickListener {
            val msg = Message.obtain(null, 1, 0, 0)
            val bundle = Bundle()

            bundle.putStringArray("remoteBda", btBda)
            msg.data = bundle
            sendMsg(btService, msg)
            txvBtState0.text = "Bluetooth connecting"
        }

        btncon1.setOnClickListener {
            val msg = Message.obtain(null, 1, 0, 1)

            sendMsg(btService, msg)
            txvBtState1.text = "Bluetooth connecting"
        }

        adapter = BtListAdapter(btList)
        recycleViewDiscovery.layoutManager = LinearLayoutManager(applicationContext)
        recycleViewDiscovery.adapter = adapter
        adapter.setOnItemClickListener(object : BtListAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int, bda: String) {
                val msg = Message.obtain(null, 1, 0, 0)
                val bundle = Bundle()
                val preferDataEdit = preferData.edit()

                Log.d(KotlinLog, "\tonItemClick")
                preferDataEdit.putString("btBda0", bda)
                preferDataEdit.apply()

                btBda[0] = bda
                bundle.putStringArray("remoteBda", btBda)
                msg.data = bundle
                sendMsg(btService, msg)
                txvBtState0.text = "Bluetooth connecting"
                Log.d(KotlinLog, "discovery address:$btBda[0]")
            }
        })

        adapter.setOnItemLongClickListener(object : BtListAdapter.OnItemLongClickListener {
            override fun onItemLongClick(view: View, position: Int, bda: String): Boolean {
                val msg = Message.obtain(null, 1, 1, 0)
                val bundle = Bundle()
                val preferDataEdit = preferData.edit()

                Log.d(KotlinLog, "\tonItemLongClick")
                preferDataEdit.putString("btBda1", bda)
                preferDataEdit.apply()

                btBda[1] = bda
                bundle.putStringArray("remoteBda", btBda)
                msg.data = bundle
                sendMsg(btService, msg)
                txvBtState1.text = "Bluetooth connecting"
                Log.d(KotlinLog, "discovery address:$btBda[1]")
                return true
            }
        })
    }

    override fun onStart() {
        super.onStart()
        Log.d(KotlinLog, "onStart")
        // startService(Intent(this, BtService::class.java))
        // sendMessage()
    }

    override fun onResume() {
        super.onResume()
        Log.d(KotlinLog, "onResume")
        // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
/*
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
*/
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(KotlinLog, "onRestart")
    }

    override fun onPause() {
        super.onPause()
        Log.d(KotlinLog, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(KotlinLog, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(KotlinLog, "onDestroy")
        // isBtBind = false
        // unbindService(serviceConn)
        // stopService(Intent(this, BtService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btInit()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(KotlinLog, "requestCode $requestCode resultCode $resultCode")
        when(resultCode) {
            101 -> {
                val msg = Message.obtain(null, 100, 0, 0)

                msg.replyTo = clientMsgHandler
                sendMsg(btService, msg)
            }

        }

        when (requestCode) {
            BtServiceRequestCode -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(KotlinLog, "Bluetooth result OK")
                    txvBtState0.text = "Bluetooth enable"
                    btInit()
                    // if(!isBtBind)
                    //    bindService(Intent(this, BtService::class.java), serviceConn, Context.BIND_AUTO_CREATE)
                }
                else {
                    Log.d(KotlinLog, "Bluetooth result cancel")
                    finish()
                }
            }
/*
            (BtServiceRequestCode + 1) -> {
                if(resultCode == Activity.RESULT_OK) {
                    Log.d(KotlinLog, "Bluetooth discoverable")
                }
            }
            (BtServiceRequestCode + 2) -> {
                    Log.d(KotlinLog, " bluetooth result code is $resultCode")
            }
*/
        }
    }

    fun btCmdSend(cmd: ByteArray, btDevice: Int)
    {
        val msg = Message.obtain(null, 2, btDevice, 0)
        val bundle = Bundle()

        Log.d(KotlinLog, "btCmdSend")
        bundle.putByteArray("rfcCmd", cmd)
        msg.data = bundle
        sendMsg(btService, msg)
    }

    fun sendMsg(messenger: Messenger, msg: Message) {
        if(!isBtBind) return
        // msg.replyTo = clientMsgHandler
        try {
            messenger.send(msg)
        } catch (e: IOException) {
            Log.d(KotlinLog, "service send message exception")
            e.printStackTrace()
        }
    }

    fun btInit() {
        Log.d(KotlinLog, "btInit")
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) &&
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            Log.d(KotlinLog, "Bluetooth request permission")
        } else if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Log.d(KotlinLog, "Bluetooth enable")
            txvBtState0.text = "Bluetooth enable"
            txvBtState1.text = "Bluetooth enable"
            // startActivityForResult(Intent(this, BluetoothActivity::class.java), BtServiceRequestCode + 2)
            // onActivityResult(BtServiceRequestCode + 2, 0, null)
            if(!isBtBind)
                bindService(Intent(this, BtService::class.java), serviceConn, Context.BIND_AUTO_CREATE)
        } else {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BtServiceRequestCode)
            Log.d(KotlinLog, "Bluetooth disable")
            txvBtState0.text = "Bluetooth disable"
            txvBtState1.text = "Bluetooth disable"
        }
    }

    fun rfcCmdParse(cmdBuf: ByteArray, btDevice: Int) {
        // val len = cmdBuf[1]
        // for(i: Int in 0..len + 1)
        // Log.d(ktLog, "${rfcRecData[i]}")
        // Log.d(KotlinLog, "command src ${cmdBuf[2].toString(16)} id ${cmdBuf[4].toString(16)}")
        when(cmdBuf[4]) {
            CmdId.SET_AG_VOL_RSP.value -> Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} AG volume set")
            CmdId.SET_HFP_VOL_RSP.value -> Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} HFP volume set")
            CmdId.GET_HFP_STA_RSP.value -> Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} destination ${cmdBuf[3].toString(16)} HFP status get")
            CmdId.GET_HFP_EXT_STA_RSP.value -> Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} HFP extra status get")
            CmdId.GET_HFP_LOCAL_NAME_RSP.value -> {
                Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} local name")
                txvGetName0.text = String(cmdBuf, 6, cmdBuf[5].toInt())
            }
            CmdId.GET_AG_LOCAL_NAME_RSP.value -> {
                Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} local name")
                txvGetName1.text = String(cmdBuf, 6, cmdBuf[5].toInt())
            }
            CmdId.GET_HFP_VRESION_RSP.value -> {
                Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} firmware version")
                txvFwVer0.text = String(cmdBuf, 6, cmdBuf[5].toInt())
            }
            CmdId.GET_AG_VRESION_RSP.value -> {
                Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} firmware version")
                txvFwVer1.text = String(cmdBuf, 6, cmdBuf[5].toInt())
            }
            CmdId.GET_SRC_DEV_NO_RSP.value -> {
                Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} source device number ${cmdBuf[6].toString(16)}")
            }
            CmdId.GET_HFP_VOL_RSP.value -> {
                Log.d(KotlinLog, " src ${cmdBuf[2].toString(16)} volume ${cmdBuf[6].toString(16)}")
            }
            CmdId.GET_HFP_PAIR_RSP.value -> {
                var bdaLap = cmdBuf[6].toUInt().and(0xff.toUInt()).shl(24) + cmdBuf[7].toUInt().and(0xff.toUInt()).shl(16) + cmdBuf[8].toUInt().and(0xff.toUInt()).shl(8) + cmdBuf[9].toUInt().and(0xff.toUInt())
                var bdaUap = cmdBuf[10].toUByte()
                var bdaNap = cmdBuf[11].toUInt().and(0xff.toUInt()).shl(8) + cmdBuf[12].toUInt().and(0xff.toUInt())

                Log.d(KotlinLog, "BDA ${bdaNap.toString(16)} : ${bdaUap.toString(16)} : ${bdaLap.toString(16)}")
            }
            else -> Log.d(KotlinLog, "other command data: ${cmdBuf[2].toString(16)} ${cmdBuf[3].toString(16)} ${cmdBuf[4].toString(16)} ${cmdBuf[5].toString(16)}")
        }
    }
}
