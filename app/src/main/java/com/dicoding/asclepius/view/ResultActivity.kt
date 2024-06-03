package com.dicoding.asclepius.view

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.asclepius.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val result = intent.getStringExtra("RESULT")
        val confidence = intent.getFloatExtra("CONFIDENCE", 0f)
        val imageUri = Uri.parse(intent.getStringExtra("IMAGE_URI"))

        binding.resultImage.setImageURI(imageUri)
        binding.resultText.text = "Result: $result"
        binding.confidenceText.text = "Confidence: ${"%.2f".format(confidence * 100)}%"
    }
}