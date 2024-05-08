package com.example.myapplication_literally

import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceView
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioSavePath: String
    private val handler = Handler(Looper.getMainLooper())
    var timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    lateinit var audioSaveUri: Uri
    var PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioSavePath = recordingFilePath

        setContentView(R.layout.activity_main)
        requestAudioPermission()
        val audioRecStartButton = findViewById<Button>(R.id.audstart)
        val audioRecStopButton = findViewById<Button>(R.id.audrecordstop)
        val audioRecSaveButton = findViewById<Button>(R.id.audsave)
        val audioMinute = findViewById<EditText>(R.id.audnum)


        audioRecStartButton.setOnClickListener {
            startAudioRecording()
        }

        audioRecStopButton.setOnClickListener {
            stopAudioRecording()
        }
        audioRecSaveButton.setOnClickListener {
            val minutes = audioMinute.text.toString().toIntOrNull() ?: 0
            trimAndSaveAudio(minutes)
        }
    }

    override fun onDestroy() {
        stopAudioRecording()
        super.onDestroy()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 201) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun trimAndSaveAudio(minutes: Int) {
        stopAudioRecording()

        val durationInSeconds = minutes * 3
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val trimmedFileName = "trimmed_recording_$timeStamp.mp3"

        val originalFileInputStream = contentResolver.openInputStream(audioSaveUri)
        val originalFileTempPath = "${externalCacheDir?.absolutePath}/original_recording_temp.3gp"
        val originalFileTempFile = File(originalFileTempPath)
        originalFileInputStream?.use { input ->
            FileOutputStream(originalFileTempFile).use { output ->
                input.copyTo(output)
            }
        }

        val trimmedFilePath = "${externalCacheDir?.absolutePath}/$trimmedFileName"

        val cmd = arrayOf("-i", originalFileTempPath, "-t", "$durationInSeconds", "-acodec", "libmp3lame", trimmedFilePath)
        if (FFmpeg.execute(cmd) == 0) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, trimmedFileName)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MyRecordings/")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

            try {
                uri?.let {
                    resolver.openFileDescriptor(uri, "w")?.use { parcelFileDescriptor ->
                        FileInputStream(trimmedFilePath).use { inputStream ->
                            FileOutputStream(parcelFileDescriptor.fileDescriptor).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }

                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)


                }
            } catch (e: Exception) {
                uri?.let {
                    resolver.delete(uri, null, null)
                }
                Toast.makeText(this, "오디오 파일 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                return
            } finally {
                startAudioRecording()
            }
        } else {
            Toast.makeText(this, "오디오 파일 자르기에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

        //originalFileTempFile.delete()

        //File(trimmedFilePath).delete()
    }



    private fun deleteAudio(l: String){
        File(l).delete()
    }

    private fun requestAudioPermission() {
        requestPermissions(PERMISSIONS, 201)
    }


    private val recordingFilePath: String by lazy {
        timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        val folderName = "MyRecordings"
        val recordingDirectory = File(getExternalFilesDir(null), folderName)
        if (!recordingDirectory.exists()) {
            recordingDirectory.mkdirs() // 폴더가 없다면 생성
            Log.d("FolderCreation", "Folder created at: ${recordingDirectory.absolutePath}") // 폴더 생성 확인 로그

        }

        "${recordingDirectory.absolutePath}/recording_$timeStamp.3gp"
    }

    private fun startAudioRecording() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_$timeStamp.mp3"
        audioSavePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath + "/MyRecordings/" + fileName
        Toast.makeText(this, fileName, Toast.LENGTH_SHORT).show()
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MyRecordings")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver
        audioSaveUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!

        audioSaveUri?.let { uri ->
            resolver.openFileDescriptor(uri, "w", null)?.use { pfd ->
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(pfd.fileDescriptor)
                    prepare()
                    start()
                }
            }
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    var tmp: String = ""
    private fun stopAudioRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        tmp = audioSavePath
        handler.postDelayed({
//            audioSaveUri?.let { uri ->
//                deleteAudio(uri)
//            }
            deleteAudio2(tmp)
        }, 4000)
    }

    private fun deleteAudio2(l: String){
        File(l).delete()
    }

    private fun deleteAudio(audioUri: Uri) {
        try {
            val rowsDeleted = contentResolver.delete(audioUri, null, null)
            if (rowsDeleted == 1) {
                Log.d("deleteAudio", "Audio file deleted successfully.")
            } else {
                Log.d("deleteAudio", "Failed to delete the audio file.")
            }
        } catch (e: Exception) {
            Log.e("deleteAudio", "Error deleting the audio file.", e)
        }
    }


    companion object {
        private const val REQUEST_CODE = 123
    }


}
