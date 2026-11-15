package com.example.gentehablando

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase principal de la Aplicación anotada con @HiltAndroidApp.
 * Actúa como el contenedor raíz del árbol de Inyección de Dependencias de Hilt.
 */
@HiltAndroidApp
class GenteHablandoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Garantizar la inicialización explícita de Firebase al arrancar la app
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
