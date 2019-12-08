package com.example.myfirstapp

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.OutputStream
import java.lang.Exception
import java.io.InputStream
import java.lang.IndexOutOfBoundsException
import java.lang.Integer.min


class BluetoothClient(private val activity: MainActivity, private val device: BluetoothDevice, private val uri: Uri, private var purpose: Boolean = true) : Thread() {
    //Нет идей, как причину лучше, чем просто булеаном -- классы перечисления чет не очень в тему.
    //Таким образом, 1 -- файл, 0 -- скриншот




    private lateinit var socket : BluetoothSocket
    private lateinit var outputStream : OutputStream
    private lateinit var inputStream : InputStream
    private lateinit var fileInputStream: InputStream
    private lateinit var bytes : ByteArray


    val TAG = "BluetoothClient"

    override fun run() {
        Log.i(TAG, "Start")
        if (purpose) {
            fileInputStream = activity.contentResolver.openInputStream(uri)!!
            val available = fileInputStream.available()
            Log.d(TAG, "available: $available")
            bytes = ByteArray(available)
            fileInputStream.read(bytes, 0, available)
        }
        else {
            //тоже надо все заполнить, чтобы потом освобождать (или даже использовать)
        }

        socket = device.createRfcommSocketToServiceRecord(uuid)
        socket.connect()
        outputStream = socket.outputStream
        inputStream = socket.inputStream

        try {
            if (purpose) {
                Log.i(TAG, "Sending file...")
                sendFile(bytes)
//                outputStream.write("JOPA".toByteArray())
//                outputStream.flush()

                //отправка файла
                //Надо выкидывать активити запроса файла. Очевидно, что для этого есть стандартный метод.
            } else {
                Log.i(TAG, "Get purpose false")
                //запрос скриншота.
                //отправляем какой-нибудь байтик-запрос скриншота.
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "Cannot send", e)
        }
        finally {
//            outputStream.close()
//            inputStream.close()
//            socket.close()
        }
    }

    private fun sendFile(file : ByteArray) : Boolean {
        outputStream.write("FILE".toByteArray())
        outputStream.flush()
        var available = inputStream.available()
        while (available == 0) {
            available = inputStream.available()
        }
        var bytes = ByteArray(available)
        inputStream.read(bytes, 0, available)
        var resp = String(bytes)
        Log.d(TAG, "get response: $resp")
        val fileName = uri.path?.lastIndexOf("/")?.plus(1)?.let { uri.path?.substring(it) } ?: "Filename"
        outputStream.write(fileName.toByteArray())
        outputStream.flush()
        available = 0
        while (available == 0){
            available = inputStream.available()
        }
        bytes = ByteArray(available)
        resp = String(bytes)
        Log.d(TAG, "get response: $resp")
        outputStream.write(file.size.toString().toByteArray())
        outputStream.flush()
        available = 0
        while (available == 0){
            available = inputStream.available()
        }
        bytes = ByteArray(available)
        resp = String(bytes)
        Log.d(TAG, "get response: $resp")
        var sent = 0
        val fileSize = file.size
        val sendSize = 990
        while (sent < fileSize) {
            sent += try {
                outputStream.write(file, sent, sendSize)
                outputStream.flush()
                sendSize
            } catch (e : IndexOutOfBoundsException) {
                outputStream.write(file, sent, fileSize - sent)
                outputStream.flush()
                (fileSize - sent)
            }

            Log.d(TAG, "Sent $sent bytes, there is ${fileSize - sent} more")
        }
        available = 0
        while (available == 0){
            available = inputStream.available()
        }
        bytes = ByteArray(available)
        resp = String(bytes)
        Log.d(TAG, "get response: $resp")

        return true
    }
}