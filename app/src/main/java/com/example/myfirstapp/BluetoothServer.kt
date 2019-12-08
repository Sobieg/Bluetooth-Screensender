package com.example.myfirstapp

import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Bitmap

import android.util.Log
import android.view.View
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

const val CREATE_FILE = 1339

class BluetoothServer(private val activity: MainActivity, private val socket: BluetoothSocket) : Thread() {
    private var outputStream : OutputStream = socket.outputStream
    private var inputStream : InputStream = socket.inputStream
    private lateinit var fileInputStream: InputStream
    private lateinit var fileOutputStream: OutputStream
    lateinit var bytes : ByteArray


    private val TAG = "BluetoothServer"

    override fun run() {
        Log.i(TAG, "Start thread Server")
        try {
            var available = inputStream.available()
            while (available == 0) {
                available = inputStream.available()
            }
//            Log.d(TAG, "available: $available")
            val bytes = ByteArray(available)
            inputStream.read(bytes, 0, available)
            if (String(bytes) == "FILE") {
                Log.d(TAG, "Get FILE")
                outputStream.write("OK".toByteArray())
                outputStream.flush()
                receiveFile()
            }
            else if (String(bytes) == "SCREEN") {
                Log.d(TAG, "Get SCREEN")
                outputStream.write("OK".toByteArray())
                outputStream.flush()
                sendScreen()
            }



            /*вот тут автор получает количество доступных байт, потом их считывает и соответственно обрабатывает
            Нам, соответственно, тоже надо тут обрабатывать входящие данные. Это могут быть:
            1) Запрос скриншота.
            2) Передача файла.

            Наверное было бы умно передавать один битик данных (если так можно без танцев с бубном)
            чтобы понять, какое действие требуется. Ну можно и байтик передавать, не страшно

            Затем, в соответствии от этого битика уже обмениваться данными, НАПРИМЕР:
            В случае скриншота можно отправить байт подтверждения, затем размер файла
            затем сам скриншот, затем байт завершения. Не уверен, что нужно передавать размеры
            и байты завершения, но мало ли...

            Если хотят передать файл, надо отправить байт подтверждения, дождаться размера
            и затем начать получение, затем сохранить в файл.

            Сохранение файла лучше бы сделать в отдельном классе
            Скриншотилку хз, возможно тоже.

            Надо понять, в каком видепроисходит работа функции скриншота -- возвращает байты или
            сохраняет файл. Если да, то с каким именем. Если сохраняет, то надо использовать тот же класс
            что и для отправки файлов, но мы будем знать имя. Может быть функция сохранения файлов
            возвращает имя?


            */
        } catch (e: Exception) {
            Log.e(TAG, "Cannot get information from client", e)
        }
        finally {
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }

    private fun createFile(name: String) {

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, name)
        }
        activity.startActivityForResult(intent, CREATE_FILE)
    }

    private fun receiveFile() {
        var available = inputStream.available()
        while (available == 0) {
            available = inputStream.available()
        }
        bytes = ByteArray(available)
        inputStream.read(bytes, 0, available)
        val fileName = String(bytes)
        Log.d(TAG, "get filename: $fileName")

        outputStream.write("OK".toByteArray())
        outputStream.flush()
        available = inputStream.available()
        while (available == 0) {
            available = inputStream.available()
        }
        var fileSize = 0
        bytes = ByteArray(available)
        inputStream.read(bytes, 0, available)
        fileSize = String(bytes).toInt()
        var received = 0
        Log.d(TAG, "Get filesize: $fileSize")
        bytes = ByteArray(fileSize)

        outputStream.write("OK".toByteArray())
        outputStream.flush()
        while (received != fileSize) {
            available = 0
            while (available == 0) {
                available = inputStream.available()
            }
            inputStream.read(bytes, received, available)
            received += available
            Log.d(TAG,"Get $available bytes, total: $received of $fileSize")
        }


        createFile(fileName)
    }

    private fun sendScreen(): Boolean {

        val bitmap = takeScreenshot(activity.window.decorView.rootView)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 98, baos)
        val scr = baos.toByteArray()
//        val bb = ByteBuffer.allocate(bitmap.byteCount)
//        bitmap.copyPixelsToBuffer(bb)
//        bb.rewind()
//        val scr = bb.array()
        val fileSize = scr.size
        outputStream.write(fileSize.toString().toByteArray())
        outputStream.flush()
        var available = 0
        while (available == 0){
            available = inputStream.available()
        }
        bytes = ByteArray(available)
        var resp = String(bytes)
        Log.d(TAG, "get response: $resp")
        var sent = 0
        val sendSize = 990

//        bitmap.compress(Bitmap.CompressFormat.JPEG, 98, outputStream)
        while (sent < fileSize) {
            sent += try {
                outputStream.write(scr, sent, sendSize)
                outputStream.flush()
                sendSize
            } catch (e : IndexOutOfBoundsException) {
                outputStream.write(scr, sent, fileSize - sent)
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

    private fun takeScreenshot(view: View) : Bitmap {
//        val displayMetrics = DisplayMetrics()
//        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
//        val width : Int = displayMetrics.widthPixels
//        val height : Int = displayMetrics.heightPixels
//        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        val bgDrawable = view.background
//        if (bgDrawable != null) {
//            bgDrawable.draw(canvas)
//        }
//        else {
//            canvas.drawColor(Color.WHITE)
//        }


        return Screenshoter.takeScreenshotOfRootView(view)
    }

}