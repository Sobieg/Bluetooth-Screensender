package com.example.myfirstapp

import android.app.IntentService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.util.Log
import java.io.IOException
import kotlin.system.exitProcess




class BluetoothServerController(private val activity: MainActivity) : Thread() {
    private lateinit var serverSocket: BluetoothServerSocket


    lateinit var server: BluetoothServer

    private val TAG = "BluetoothServerController"
    var isCancelled  = false


    override fun run() {
        val adapter = getBluetoothAdapter()
        serverSocket = adapter.listenUsingRfcommWithServiceRecord(sdpName, uuid)
        waitConnection()
    }

    private fun waitConnection(){
        var socket: BluetoothSocket


        while (!isCancelled) {
            try {
                socket = serverSocket.accept()
            }
            catch (e: IOException) {
                Log.e(TAG, "server socket's accept() method failed", e)
                isCancelled = true
                break
            }
            if (!isCancelled) {
                Log.d(TAG, "Connecting")
                server = BluetoothServer(activity, socket)
                server.start()

            }
//            enableDiscovery()
            /*
            TODO: сделать подписку на оповещения
            ACTION_SCAN_MODE_CHANGED -- возникает в случае когда изменяется состояние сканирования
            В EXTRA_SCAN_MODE будут лежать всякие разные значения в зависимости от нового мода.
            Было бы классно в режиме сервера в отдельном потоке чекать и обновлять режим видимости
            https://developer.android.com/reference/kotlin/android/bluetooth/BluetoothAdapter.html#EXTRA_SCAN_MODE:kotlin.String
            ИЛИ вообще делать большой промежуток дескаверабилити, а потом руками его выключать, но это тупо
            На самом деле 5 минут дефолтных хватает.
             */
            //TODO: сделать, чтобы тред слушал оповещения и после окончания дискаверинга завершался бы.
        }
    }

    fun cancel() {
        isCancelled = true
        this.serverSocket.close()
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
}