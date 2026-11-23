package com.voicenotes.app

import android.app.Application
import com.voicenotes.app.data.ApiClient
import com.voicenotes.app.data.NotesRepository
import com.voicenotes.app.data.SessionStore

/** Общие объекты приложения создаются один раз при запуске. */
class AppContainer(application: Application) {
    val session = SessionStore(application)
    val repository = NotesRepository(ApiClient.create(session), session, application.cacheDir)
}

class VoiceNotesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
