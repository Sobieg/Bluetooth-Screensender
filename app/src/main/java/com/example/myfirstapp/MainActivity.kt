package com.example.myfirstapp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {


    var bluetoothAdapter:BluetoothAdapter? = null
    var devicesSet:MutableSet<BluetoothDevice>? = null
    var deviceNameList:MutableList<String> = MutableList(/*pairedDevices?.size?:0*/0, {""})
    var devicesNameListAdapter:ArrayAdapter<String>? = null

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
                    Toast.makeText(applicationContext, "ON", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "OFF", Toast.LENGTH_LONG).show()
                }
            }

        }


//        listView_devicesList.onItemClickListener = object : AdapterView.OnItemClickListener{
//            override fun onItemClick(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {// value of item that is clicked
//
//                // Toast the values
//                Toast.makeText(applicationContext,
//                    "Connect", Toast.LENGTH_LONG)
//                    .show()
//
//            }
//        }

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
        return adapter
    }

    private fun regReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    devicesSet?.add(device)
                    devicesNameListAdapter?.add(device.name)
                    Toast.makeText(applicationContext, "Found new device: " + device.name, Toast.LENGTH_LONG).show()

                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }




}
