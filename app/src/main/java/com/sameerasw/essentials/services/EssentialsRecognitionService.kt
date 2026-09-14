package com.sameerasw.essentials.services

import android.content.Intent
import android.speech.RecognitionService

class EssentialsRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
    }

    override fun onStopListening(listener: Callback?) {
    }

    override fun onCancel(listener: Callback?) {
    }
}
