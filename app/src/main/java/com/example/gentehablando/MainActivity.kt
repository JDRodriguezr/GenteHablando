package com.example.gentehablando

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.gentehablando.domain.repository.AuthRepository
import com.example.gentehablando.ui.navigation.AppNavigation
import com.example.gentehablando.ui.theme.GenteHablandoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Autenticación anónima transparente al iniciar la aplicación de forma segura
        lifecycleScope.launch {
            try {
                if (authRepository.getCurrentUserId() == null) {
                    authRepository.signInAnonymously()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            GenteHablandoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
