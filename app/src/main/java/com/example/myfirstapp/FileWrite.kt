package com.example.myfirstapp

import android.net.Uri
import android.util.Log
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception


class FileWrite(private val activity: MainActivity, private val uri: Uri, private val bytes : ByteArray) : Thread() {

    val TAG = "BluetoothFileWrite"

    override fun run() {
        try {
            activity.contentResolver.openFileDescriptor(uri, "w").use {
                FileOutputStream(it?.fileDescriptor).use {
                    it.write(bytes)
                }
            }
        }
        catch (e : FileNotFoundException) {
            Log.e(TAG, "File not found: ", e)
        }
        catch (e : IOException) {
            Log.e(TAG, "IO exception")
        }
    }
}