package com.example.myfirstapp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.UUID.fromString
import kotlin.system.exitProcess
import android.Manifest
import android.app.Activity
import android.content.*
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat


/*
TODO: 1) Включать обнаружение, когда переходим в режим сервера -- Done
TODO: 2) Сервер в отдельном треде -- Done
TODO: 3) Разобраться с тем, что обнаруженных девайсов нет. -- DONE
TODO: 4) Сделать отправку хоть чего-нибудь
TODO: 5) Сделать скриншотилку
TODO: 6) Сделать отправку файла
TODO: 7) Подписаться на обновления чего-нибудь (желательно кастомные), чтобы понимать, когда в сервере появилось новое подключение
TODO: 8) Соответственно, если пришло оповещение о том, что режим сервера выключен, необходимо переставать получать подключения ???
TODO: 9) Выводить в списке устройств сопряженные устройства если они есть в области действия
 */


const val sdpName = "SUPERNAME"
val uuid:UUID = fromString("4788b9fd-6256-40b4-91e9-a011f800cce7")
const val _MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1337 //Переменная для хранения ответа от функции, которая запрашивает рантайм-разрешение на доступ к геолокации (для включения обнаружения bluetooth)
const val GET_FILE_REQUEST = 1338


const val TAG = "BluetoothMainThread"

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter : BluetoothAdapter
    private var devicesSet:MutableSet<BluetoothDevice> = hashSetOf()
    private var deviceNameList:MutableList<String> = MutableList(/*pairedDevices?.size?:0*/0) {""}
    private lateinit var devicesNameListAdapter:ArrayAdapter<String>

    private lateinit var cliDevice : BluetoothDevice
    private var cliPurpose : Boolean = true

    private lateinit var fileWriter: FileWrite

    private lateinit var serverController: BluetoothServerController


    private val mContext:MainActivity = this

    fun getContext() : MainActivity {
        return mContext
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(TAG, "START")


        bluetoothAdapter = getBluetoothAdapter()
        devicesNameListAdapter = initListView()
        regReceiver()
        bluetoothAdapter.startDiscovery()

        switch_server.setOnCheckedChangeListener { buttonView, isChecked ->
            run {
                if (isChecked) {
                    bluetoothAdapter.cancelDiscovery()
                    enableDiscoverability()
                    devicesNameListAdapter.clear()
                    serverController = BluetoothServerController(this)
                    serverController.start()
                } else {
                    if (!serverController.isCancelled) {
                        serverController.cancel()
                    }
                    bluetoothAdapter.startDiscovery()
                }
            }
        }
    }

    private fun enableDiscoverability() {
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(discoverableIntent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == GET_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data.also { uri ->
                BluetoothClient(
                    activity = this,
                    device = cliDevice,
                    uri = uri!!,
                    purpose = cliPurpose
                ).start()
            }
        }
        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data.also { uri ->
                FileWrite(
                    activity = this,
                    uri = uri!!,
                    bytes = serverController.server.bytes
                ).start()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getBluetoothAdapter() : BluetoothAdapter {
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


    private fun bluetoothClientPrepare(purpose :Boolean) {
        Log.i(TAG, "Start preparing client")
        cliPurpose = purpose
        if (purpose) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, GET_FILE_REQUEST)
        }
        else {
//            BluetoothClient(this, device, false, null)
        }
    }

    private fun showDialog(device: BluetoothDevice) {
        val builder = AlertDialog.Builder(this)

        cliDevice = device

        builder.setTitle("Choose action")
//        builder.setMessage("TEST TEST")
        builder.setPositiveButton("Send file") { _, _ -> bluetoothClientPrepare(true)}
        builder.setNegativeButton("Get screenshot") { _, _ -> bluetoothClientPrepare(false)}
        builder.show()
    }

    private fun initListView() : ArrayAdapter<String> {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNameList)
        listView_devicesList.adapter = adapter

        listView_devicesList.setOnItemClickListener {
            parent, view, position, id ->
//            connectAsAClient((view as TextView).text)
            // Новое активити? Диалог?
            bluetoothAdapter.cancelDiscovery()
            showDialog(devicesSet.find{ device -> device.name == ((view as TextView).text) }!!)
//            Toast.makeText(applicationContext, (view as TextView).text, Toast.LENGTH_LONG).show()
        }
        return adapter
    }

    private fun regReceiver() {

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) { // если не получена привилегия на доступ к геолокации
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), _MY_PERMISSIONS_ACCESS_FINE_LOCATION) //получить
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)


        val fil = IntentFilter() //TODO: добавить фильтр для получения нового подключения.
//        registerReceiver(newConnReceiver, fil)

    }


//    private val newConnReceiver = NewBluetoothConnection()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
//                    Toast.makeText(applicationContext, "Found new device: " /*+ device.name*/, Toast.LENGTH_LONG).show()

                    val device: BluetoothDevice = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    var name = device.name
                    if (device.name == null){
                        name = device.address
                    }
                    devicesSet?.add(device)
                    devicesNameListAdapter?.add(name)
                    Toast.makeText(applicationContext, "Found new device: $name", Toast.LENGTH_LONG).show()
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Toast.makeText(applicationContext, "Discovery started", Toast.LENGTH_LONG).show()
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Toast.makeText(applicationContext, "Discovery finished", Toast.LENGTH_LONG).show()
                }

                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    Toast.makeText(applicationContext, "KEKEKEK", Toast.LENGTH_LONG).show()
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        bluetoothAdapter.cancelDiscovery()
        unregisterReceiver(receiver)
//        unregisterReceiver(newConnReceiver)
//        unregisterReceiver(newConnReceiver)
    }

}



