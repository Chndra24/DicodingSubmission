package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ImageClassifierHelper(private val context: Context) {
    private lateinit var interpreter: Interpreter

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        try {
            val model = loadModelFile(context, "cancer_classification.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val fileInputStream = assetFileDescriptor.createInputStream()
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    fun classifyStaticImage(imageUri: Uri, callback: (String, Float) -> Unit) {
        try {
            val bitmap = getBitmapFromUri(imageUri)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)

            interpreter.run(byteBuffer, outputBuffer.buffer.rewind())

            val confidences = outputBuffer.floatArray
            val maxConfidence = confidences.maxOrNull() ?: 0f
            val result = if (confidences[1] > confidences[0]) "Cancer" else "Non-Cancer"

            Log.d("ImageClassifierHelper", "Result: $result, Confidence: $maxConfidence")
            Log.d("ImageClassifierHelper", "Confidences: ${confidences.joinToString()}")

            callback(result, maxConfidence)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageClassifierHelper", "Error classifying image: ${e.message}")
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val value = intValues[pixel++]

                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 127.5f)
            }
        }

        Log.d("ImageClassifierHelper", "ByteBuffer: ${byteBuffer.asFloatBuffer()}")
        return byteBuffer
    }
}
