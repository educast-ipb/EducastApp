package com.pt.fccn.educast

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.result.success
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    val REQUEST_VIDEO_CAPTURE = 1
    val apiUrl = "https://0e99af253c4e.ngrok.io/api/FileController"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button = findViewById<ImageButton>(R.id.video_icon)
        button.setOnClickListener {
            if (!checkPermissions()) return@setOnClickListener
            Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                takeVideoIntent.resolveActivity(packageManager)?.also {
                    startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1010) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) return
            }
            Toast.makeText(this, "Permissões garantidas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return if (checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(permissions[1]) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(permissions, 1010)
            false
        } else {
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        progress_bar.visibility = View.VISIBLE
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_VIDEO_CAPTURE && intent!!.data != null) {
            val uri = intent.data!!
            val projection = arrayOf(MediaStore.Video.Media.DATA)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            try {
                val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(columnIndex)
                    val file = File(path)
                    Fuel.upload(apiUrl, Method.POST)
                        .add { FileDataPart(file, filename = file.name) }
                        .response { result ->
                            runOnUiThread {
                                findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                                result.success {
                                    Toast.makeText(
                                        this,
                                        "Vídeo criado com sucesso",
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                cursor?.close()
            }
        }

    }
}
