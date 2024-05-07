package com.example.myapplication_literally

import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceView
import android.Manifest
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioSavePath: String
    private val handler = Handler(Looper.getMainLooper())
    var timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    var PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
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
    private fun trimAndSaveAudio(minutes: Int) {
        stopAudioRecording()

        val durationInSeconds = minutes * 60
        timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val trimmedFilePath = "${externalCacheDir?.absolutePath}/trimmed_recording_$timeStamp.mp3"

        // FFmpeg를 사용하여 오디오 파일 자르기 전 파일 경로를 큰따옴표로 감싸기
        val cmd = "-i \"$audioSavePath\" -t $durationInSeconds -acodec libmp3lame \"$trimmedFilePath\""

        if (FFmpeg.execute(cmd) == 0) {

            // 성공적으로 파일을 자른 후 녹음 시작
            audioSavePath = trimmedFilePath // 업데이트된 파일 경로를 저장
            startAudioRecording()
        } else {
            // 오류 처리
            Toast.makeText(this, "오디오 파일 자르기에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

    }
    private fun deleteAudio(l: String){
        File(l).delete()
    }

    private fun requestAudioPermission() {
        requestPermissions(PERMISSIONS, 201)
    }
    private val recordingFilePath: String by lazy {
        timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        "${externalCacheDir?.absolutePath}/recording_$timeStamp.3gp"
    }
    private fun startAudioRecording() {
        mediaRecorder = MediaRecorder()
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(recordingFilePath)
            prepare()
        }
        audioSavePath = recordingFilePath
        mediaRecorder?.start()

    }

    private fun stopAudioRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        handler.postDelayed({ deleteAudio(audioSavePath) }, 10000)
        audioSavePath = recordingFilePath

    }


    companion object {
        private const val REQUEST_CODE = 123
    }


}
