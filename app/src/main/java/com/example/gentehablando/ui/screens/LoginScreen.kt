package com.example.gentehablando.ui.screens

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentehablando.R
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.loginState.collectAsState()
    val context = LocalContext.current

    // 1. Efecto al loguearse con éxito
    LaunchedEffect(uiState) {
        if (uiState is Resource.Success) {
            val successState = uiState as Resource.Success
            if (successState.data.isNotBlank()) {
                onLoginSuccess()
            }
        }
    }

    // 2. Preparar el cliente de Google Sign-In estándar
    val webClientId = stringResource(R.string.default_web_client_id)
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    // 3. Launcher para atrapar el resultado de la ventana pop-up de Google
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                viewModel.setError("Error: El Token de Google vino vacío.")
            }
        } catch (e: ApiException) {
            Log.e("Auth", "Google sign in failed", e)
            val errorMessage = when (e.statusCode) {
                10 -> "Error 10: Huella SHA-1 de Firebase incorrecta o faltante."
                12501 -> "Cancelado por el usuario."
                else -> "Error en el pop-up de Google: Código ${e.statusCode}"
            }
            viewModel.setError(errorMessage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GenteHablando",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Para mantener un espacio seguro y empático, es necesario iniciar sesión.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (uiState is Resource.Loading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Iniciando sesión...")
        } else {
            Button(
                onClick = {
                    viewModel.setLoading()
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "Continuar con Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (uiState is Resource.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as Resource.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
