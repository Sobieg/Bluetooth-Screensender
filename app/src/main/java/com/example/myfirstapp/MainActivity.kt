package com.example.myfirstapp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import java.util.UUID.fromString
import java.util.UUID.randomUUID
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {



    private var bluetoothAdapter:BluetoothAdapter? = null
    private var devicesSet:MutableSet<BluetoothDevice>? = null
    private var deviceNameList:MutableList<String> = MutableList(/*pairedDevices?.size?:0*/0, {""})
    private var devicesNameListAdapter:ArrayAdapter<String>? = null
    private var sdpName = "SUPERNAME"
    private var bluUUID: UUID = fromString("4788b9fd-6256-40b4-91e9-a011f800cce7")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        bluetoothAdapter = checkBluetoothState()
        devicesSet = checkPairedDevices()?.toMutableSet()
        devicesSet?.forEach { device -> deviceNameList.add(device.name) }
        devicesNameListAdapter = initListView()

//        val mydev: BluetoothDevice? = pairedDevices?.find{ device -> device.name == "XMZPG" }
//        Log.d("MY_TAG", mydev?.address?:"not found")

        regReceiver()
        bluetoothAdapter?.startDiscovery()

        switch_server.setOnCheckedChangeListener { buttonView, isChecked ->
            run {//TODO: implement this
                if (isChecked) {
//                    Toast.makeText(applicationContext, "ON", Toast.LENGTH_LONG).show()
                    connectAsAServer()
                } else {
                    Toast.makeText(applicationContext, "OFF", Toast.LENGTH_LONG).show()
                }
            }

        }




    }


    private fun checkBluetoothState() : BluetoothAdapter {

        //Включаем блютусик, если выключен
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            ?: //Девайс не поддерживает блютусик
            exitProcess(1)
        //включаем блютусик, если он выключен
        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
        }
        return bluetoothAdapter
    }


    private fun checkPairedDevices() : Set<BluetoothDevice>? {
        //смотрим, есть ли уже спареные блютусик девайсы
        return bluetoothAdapter?.bondedDevices
    }

    private fun initListView() : ArrayAdapter<String> {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNameList)
        listView_devicesList.adapter = adapter

        listView_devicesList.setOnItemClickListener {
            parent, view, position, id ->
            connectAsAClient((view as TextView).text)
//            Toast.makeText(applicationContext, (view as TextView).text, Toast.LENGTH_LONG).show()
        }

        return adapter
    }

    private fun regReceiver() {
        val filterFound = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val filterStart = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        val filterFinish = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filterFound)
        registerReceiver(receiver, filterStart)
        registerReceiver(receiver, filterFinish)

    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
//                    Toast.makeText(applicationContext, "Found new device: " /*+ device.name*/, Toast.LENGTH_LONG).show()

                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    devicesSet?.add(device)
                    devicesNameListAdapter?.add(device.name)
                    Toast.makeText(applicationContext, "Found new device: " + device.name, Toast.LENGTH_LONG).show()
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Toast.makeText(applicationContext, "Discovery started", Toast.LENGTH_LONG).show()
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Toast.makeText(applicationContext, "Discovery finished", Toast.LENGTH_LONG).show()
                }

            }
        }
    }

    private inner class ClientConnectThread(device: BluetoothDevice?) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device?.createRfcommSocketToServiceRecord(bluUUID)
        }
        override fun run() {
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use {socket ->
                socket.connect()
                Toast.makeText(applicationContext, "Connected", Toast.LENGTH_LONG).show()
                //тут что-то делаем, например, ждем нажатия на кнопку, можно какой-нибудь Toast бахнуть чтобы пользователь понял че надо
            }
        }
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e : IOException) {
                Log.e("MY_TAG", "Could not close the client socket", e)
            }
        }
    }

    private inner class ServerConnectThread() : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by  lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(sdpName, bluUUID)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e("MY_TAG", "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    Toast.makeText(applicationContext, "New connection", Toast.LENGTH_LONG).show()
                    //Тут тоже обработка соединения, когда уже собственно соединились
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch(e: IOException) {
                Log.e("MY_TAG", "Could not close the connect socket", e)
            }
        }
    }

    private fun connectAsAClient(server: CharSequence) {
        val mydev: BluetoothDevice? = devicesSet?.find{ device -> device.name == server }
//        Toast.makeText(applicationContext, mydev?.address, Toast.LENGTH_LONG).show()
        val newobj = ClientConnectThread(mydev)
        newobj.run()
        newobj.cancel()
    }

    private fun connectAsAServer(){
        val newobj = ServerConnectThread()
        newobj.run()
        newobj.cancel()
    }


    override fun onDestroy() {
        super.onDestroy()

        bluetoothAdapter?.cancelDiscovery()
        unregisterReceiver(receiver)
    }




}

