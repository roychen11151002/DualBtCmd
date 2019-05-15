package com.example.dualbtcmd

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class BtService : Service() {
    private val btHandler = Messenger(BtMsgHandle())
    private lateinit var clientMsgHandler: Messenger
    var isBtBind = false
    var maxBtDevice = 0

    val btAdapter = BluetoothAdapter.getDefaultAdapter()
    // var myBtDevice = arrayOfNulls<MyBtDevice>(maxBtDevice) as Array<MyBtDevice>
    lateinit var myBtDevice: Array<MyBtDevice>

    inner class BtMsgHandle : Handler() {
        var serviceMsg = Message()
        var serviceBundle = Bundle()
        val discoveryList = ArrayList<BluetoothDevice>()
        val discoveryStr = ArrayList<String>()

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg != null) {
                Log.d(KotlinLog, "service message: ${msg.what}, arg1: ${msg.arg1}, arg2: ${msg.arg2}")
                // clientMsgHandler = msg.replyTo
                when(msg.what) {
                    100 -> {
                        clientMsgHandler = msg.replyTo
                        for(i in 0 until maxBtDevice) {
                            Log.d(KotlinLog, "class init $i")
                            myBtDevice[i].btMessenger = clientMsgHandler
                        }
                    }
                    0 -> {
                        val intentFilter = IntentFilter()

                        maxBtDevice = msg.arg2
                        serviceMsg = Message.obtain(null, 0, 0, 0)
                        clientMsgHandler = msg.replyTo
                        //Log.d(KotlinLog, "${myBtDevice.size}")
                        myBtDevice = arrayOfNulls<MyBtDevice>(maxBtDevice) as Array<MyBtDevice>
                        for(i in 0 until maxBtDevice) {
                            Log.d(KotlinLog, "class init $i")
                            myBtDevice[i] = MyBtDevice(clientMsgHandler, "00:00:00:00:00:00", i)
                        }
                        myBtDevice[0].sendMsgToClient(serviceMsg)
                        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
                        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                        registerReceiver(btReceiver(), intentFilter)
                    }
                    1 -> {
                        when(msg.arg2) {
                            0 -> {
                                val btBda = msg.data.getStringArray("remoteBda")

                                Log.d(KotlinLog, "service receive device: ${msg.arg1} bda0: ${btBda[0]}, bda1: ${btBda[1]}")
                                myBtDevice[msg.arg1].btBda = btBda[msg.arg1]
                                if(myBtDevice[msg.arg1].isConnected())
                                    myBtDevice[msg.arg1].close()
                                SystemClock.sleep(3000)
                                if(!myBtDevice[msg.arg1].isConnected())
                                    myBtDevice[msg.arg1].connect()
                            }
                            1 -> {
                                Log.d(KotlinLog, "bluetooth disconnect device: ${msg.arg1}")
                                myBtDevice[msg.arg1].close()
                            }
                            2 -> {
                                val paired = btAdapter.bondedDevices
                                val pairedStr = ArrayList<String>()

                                Log.d(KotlinLog, "pair command")
                                pairedStr.clear()
                                if(paired.size > 0) {
                                    for(device: BluetoothDevice in paired) {
                                        // pairedList.add(device)
                                        pairedStr.add(device.name + " + " + device.address)
                                        Log.d(KotlinLog, "${device.name} + ${device.address}")
                                    }
                                }
                                serviceBundle.putStringArrayList("pairedList", pairedStr)
                                serviceMsg = Message.obtain(null, 1, 0, 2)
                                serviceMsg.data = serviceBundle
                                myBtDevice[0].sendMsgToClient(serviceMsg)
                            }
                            3 -> {
                                if(!btAdapter.isDiscovering) {
                                    discoveryList.clear()
                                    discoveryStr.clear()
                                    btAdapter.startDiscovery()
                                }
                            }
                            4 -> {
                                if(btAdapter.isDiscovering)
                                    btAdapter.cancelDiscovery()
                            }
                            else -> {
                                Log.d(KotlinLog, "unknown command")
                            }
                        }
                    }
                    2 -> {
                        serviceBundle = msg.data
                        val cmd = serviceBundle.getByteArray("rfcCmd")
                        myBtDevice[msg.arg1].rfcCmdSend((cmd))
                    }
                    else -> Log.d(KotlinLog, "service message other")
                }
            }
        }

        inner class btReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Log.d(KotlinLog, "bluetooth discovery receiver")
                if(intent != null) {
                    when(intent.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                            Log.d(KotlinLog, "bluetooth receiver ACTION_FOUND")
                            if(!discoveryList.contains(device)) {
                                discoveryList.add(device)
                                discoveryStr.add(device.name + " + " + device.address)

                                Log.d(KotlinLog, "${device.name}, ${device.address}")
                                serviceBundle.putString("discoveryStr", device.name + " + " + device.address)
                                serviceMsg = Message.obtain(null, 1, 0, 3)
                                serviceMsg.data = serviceBundle
                                myBtDevice[0].sendMsgToClient(serviceMsg)
                            }
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                            Log.d(KotlinLog, "bluetooth receiver ACTION_DISCOVERY_FINISHED")
                            serviceBundle.putStringArrayList("discoveryList", discoveryStr)
                            serviceMsg = Message.obtain(null, 1, 0, 4)
                            serviceMsg.data = serviceBundle
                            myBtDevice[0].sendMsgToClient(serviceMsg)
                        }
                        BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                            Log.d(KotlinLog, "bluetooth receiver ACTION_BOND_STATE_CHANGED")
                        }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            Log.d(KotlinLog, "bluetooth receiver ACTION_ACL_DISCONNECTED")
                            for(i in 0 until maxBtDevice) {
                                if(!myBtDevice[i].isConnected()) {
                                    serviceMsg = Message.obtain(null, 1, i, 1)
                                    myBtDevice[0].sendMsgToClient(serviceMsg)
                                }
                            }
                        }
                        else -> {
                            Log.d(KotlinLog, "bluetooth receiver other action")
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        isBtBind = true
        return btHandler.binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(KotlinLog, "BtService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(KotlinLog, "BtService onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(KotlinLog, "bluetooth service onDestroy")
        for(i in 0 until maxBtDevice) {
            if(myBtDevice[i].isConnected())
                myBtDevice[i].close()
        }
        isBtBind = false
    }
}

class MyBtDevice(var btMessenger: Messenger, var btBda: String, var btDevice: Int ) {
    val SppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    val btAdapter = BluetoothAdapter.getDefaultAdapter()
    var rfcSocket: BluetoothSocket = btAdapter.getRemoteDevice(btBda).createRfcommSocketToServiceRecord(SppUuid)
    var isBtConnected = false

    var thread = Thread()
    val btRead = Runnable {
        var btReadMsg: Message
        var btReadBundle = Bundle()
        val rfcRecDataMaxLen = 1024
        var rfcRecDataLen = 0
        var isRfcRecCmd = false
        var isRfcRecHead = false
        val rfcRecData = ByteArray(rfcRecDataMaxLen)

        Log.d(KotlinLog, "thread running")
        while(rfcSocket.isConnected) {
            if(!isRfcRecCmd) {
                try {
                    Log.d(KotlinLog, "bluetooth read data $btDevice")
                    rfcRecDataLen += rfcSocket.inputStream.read(rfcRecData, rfcRecDataLen, rfcRecDataMaxLen - rfcRecDataLen)
                } catch (e: IOException) {
                    Log.d(KotlinLog, "bluetooth read fail")
                    break
                }
            }
            // Log.d(ktLog, "rfc receive data length: ${rfcRecDataLen}")
            isRfcRecCmd = false
            var i = 0
            while(i < rfcRecDataLen) {
                //for(i: Int in 0 until rfcRecDataLen) {
                // Log.d(ktLog, "check command header $i ${rfcReceiverCmd[i]} ${rfcReceiverCmd[i + 1]}")
                if((rfcRecData[i] == 0xff.toByte()) && (rfcRecData[i + 1] == 0x55.toByte())) {
                    isRfcRecHead = true;
                    // Log.d(ktLog, "rfc header is detected")
                    if(rfcRecDataLen >= i + rfcRecData[i + 5] + 7) {
                        val rfcCmd = ByteArray(rfcRecData[i + 5] + 7)
                        // Log.d(ktLog, "rfc command is detected ${i}")
                        System.arraycopy(rfcRecData, i, rfcCmd, 0, rfcRecData[i + 5] + 7)
                        rfcRecDataLen -= i + rfcRecData[i + 5] + 7
                        System.arraycopy(rfcRecData, i + 7 + rfcRecData[i + 5], rfcRecData, 0, rfcRecDataMaxLen - (i + 7 + rfcRecData[i + 5]))
                        if(myBtCheckSum(rfcCmd)) {
                            isRfcRecCmd = true
                            btReadMsg = Message.obtain(null, 2, btDevice, 2)
                            btReadBundle.putByteArray("rfcCmd", rfcCmd)
                            btReadMsg.data = btReadBundle
                            sendMsgToClient(btReadMsg)
                            isRfcRecHead = false
                            i = 0
                            SystemClock.sleep(1)
                            continue
                        }
                    }
                    break
                }
                else
                    i++
            }
            if(!isRfcRecHead)
            {
                rfcRecDataLen = 0
            }
        }
        Log.d(KotlinLog, "bluetooth $btDevice disconnect and read thread free")
        isBtConnected = false
        btReadMsg = Message.obtain(null, 1, btDevice, 1)
        sendMsgToClient(btReadMsg)
    }

    fun connect() {
        Thread(Runnable {
            var btConMsg: Message

            Log.d(KotlinLog, "\tconnect remote bluetooth address: ${btBda}")
            rfcSocket = btAdapter.getRemoteDevice(btBda).createRfcommSocketToServiceRecord(SppUuid)
            if(!rfcSocket.isConnected) {
                for(i in 0 until 1) {
                    try {
                        Log.d(KotlinLog, "bluetooth connecting $btDevice")
                        rfcSocket.connect()
                    } catch (e: IOException) {
                        Log.d(KotlinLog, "bluetooth connect fail $btDevice")
                    }
                    if(rfcSocket.isConnected) {
                        thread = Thread(btRead)
                        thread.start()
                        break
                    } else
                        SystemClock.sleep(3000)
                }
            }

            if(rfcSocket.isConnected) {
                Log.d(KotlinLog, "bluetooth connected $btDevice")
                isBtConnected = true
                btConMsg = Message.obtain(null, 1, btDevice, 0)
            } else {
                Log.d(KotlinLog, "bluetooth disconnect $btDevice")
                isBtConnected = false
                btConMsg = Message.obtain(null, 1, btDevice, 1)
            }
            sendMsgToClient(btConMsg)
        }).start()
    }

    fun close()
    {
        Log.d(KotlinLog, "bluetooth $btDevice socket close")
        rfcSocket.close()
        isBtConnected = false
    }

    fun isConnected() = isBtConnected

    fun myBtCheckSum(cmdBuf: ByteArray): Boolean {
        var chksum = 0
        for(i in 2 until cmdBuf[5] + 6)
            chksum += cmdBuf[i].toInt().and(0xff)
        chksum = chksum.inv().and(0xff)
        if(chksum == (cmdBuf[cmdBuf[5] + 6]).toInt().and(0xff))
            return true
        else {
            (cmdBuf[cmdBuf[5] + 6]) = chksum.toByte()
            return false
        }
    }

    fun rfcCmdSend(cmdBuf: ByteArray) {
        if(isBtConnected) {
            myBtCheckSum(cmdBuf)
            rfcSocket.outputStream.write(cmdBuf)
        }
    }

    fun sendMsgToClient(msg: Message) {
        // if(!isBtBind) return
        try {
            btMessenger.send(msg)
        } catch (e: IOException) {
            Log.d(KotlinLog, "service send message exception")
            e.printStackTrace()
        }
    }
}
