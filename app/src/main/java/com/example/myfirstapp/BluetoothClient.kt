package com.example.myfirstapp

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.OutputStream
import java.lang.Exception
import java.io.InputStream
import java.lang.IndexOutOfBoundsException


const val CREATE_SCREEN = 1340


class BluetoothClient(private val activity: MainActivity, private val device: BluetoothDevice, private val uri: Uri?, private var purpose: Boolean = true) : Thread() {
    //Нет идей, как причину лучше, чем просто булеаном -- классы перечисления чет не очень в тему.
    //Таким образом, 1 -- файл, 0 -- скриншот




    private lateinit var socket : BluetoothSocket
    private lateinit var outputStream : OutputStream
    private lateinit var inputStream : InputStream
    private lateinit var fileInputStream: InputStream
    lateinit var bytes : ByteArray
    lateinit var scr : Bitmap


    val TAG = "BluetoothClient"

    override fun run() {
        Log.i(TAG, "Start")

        socket = device.createRfcommSocketToServiceRecord(uuid)
        socket.connect()
    //считывание файла
        if (purpose) {
            fileInputStream = uri?.let { activity.contentResolver.openInputStream(it) }!!
            val available = fileInputStream.available()
            Log.d(TAG, "available: $available")
            bytes = ByteArray(available)
            fileInputStream.read(bytes, 0, available)
        }
        else {
            fileInputStream = socket.inputStream // заглушка, лол)
        }

            //блютузные стримы
        outputStream = socket.outputStream
        inputStream = socket.inputStream

        try {
            if (purpose) {
                Log.i(TAG, "Sending file...")
                sendFile(bytes)
            } else {
                Log.i(TAG, "Get purpose false") //загрузка скрина
                getScreenShot()
            }
        } //при эксепшене
        catch (e: Exception) {
            Log.e(TAG, "Cannot send", e)
        }
        finally {
            outputStream.close()
            inputStream.close()
            socket.close()
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

        val fileName = uri?.path?.lastIndexOf("/")?.plus(1)?.let { uri.path?.substring(it) } ?: "Filename"
        outputStream.write(fileName.toByteArray())
        outputStream.flush()
        available = 0
        while (available == 0){
            available = inputStream.available()
        }
        bytes = ByteArray(available)
        resp = String(bytes)
        Log.d(TAG, "get response: $resp")

            //размер файла строкой
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

    private fun getScreenShot() {
        outputStream.write("SCREEN".toByteArray())
        outputStream.flush()
        var available = inputStream.available()
        while (available == 0) {
            available = inputStream.available()
        }
        bytes = ByteArray(available)
        inputStream.read(bytes, 0, available)
        val resp = String(bytes)
        Log.d(TAG, "Get response: $resp")

        available = 0
        while (available == 0) {
            available = inputStream.available()
        }
        bytes = ByteArray(available)
        inputStream.read(bytes, 0, available)
        val fileSize: Int = String(bytes).toInt()
        Log.d(TAG, "Get filesize: $fileSize")
        bytes = ByteArray(fileSize)
        outputStream.write("OK".toByteArray())
        outputStream.flush()

        var received = 0
        while (received != fileSize) {
            available = 0
            while (available == 0) {
                available = inputStream.available()
            }
            inputStream.read(bytes, received, available)
            received += available
            Log.d(TAG,"Get $available bytes, total: $received of $fileSize")
        }
        outputStream.write("OK".toByteArray())
        outputStream.flush()

//        scr = BitmapFactory.decodeByteArray(bytes, 0, fileSize)

        createFile("screenshot.jpg")

    }

    private fun createFile(name: String) {

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/jpeg"
            putExtra(Intent.EXTRA_TITLE, name)
        }
        activity.startActivityForResult(intent, CREATE_SCREEN)
    }
}