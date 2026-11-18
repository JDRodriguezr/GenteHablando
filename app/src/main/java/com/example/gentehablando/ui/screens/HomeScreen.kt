package com.example.gentehablando.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentehablando.model.UserRole

import androidx.compose.ui.graphics.Color
import com.example.gentehablando.viewmodel.AuthViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.collectAsState
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.viewmodel.ProfileViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    onConnectClick: (UserRole) -> Unit = {},
    onLoginClick: (UserRole) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onGuidesClick: () -> Unit = {}
) {
    var selectedRole by remember { mutableStateOf(UserRole.SPEAKER) }
    val userProfile by profileViewModel.userProfile.collectAsState()
    var showBannedDialog by remember { mutableStateOf(false) }
    var showAppealDialog by remember { mutableStateOf(false) }
    var appealText by remember { mutableStateOf("") }
    
    val profileActionState by profileViewModel.actionState.collectAsState()

    if (showBannedDialog) {
        AlertDialog(
            onDismissRequest = { showBannedDialog = false },
            title = { Text("Cuenta suspendida", color = MaterialTheme.colorScheme.error) },
            text = { Text("Tu cuenta ha sido bloqueada temporalmente debido a que acumulaste múltiples reportes de conducta indebida en los últimos 30 días. Actualmente no puedes iniciar nuevas conversaciones.") },
            confirmButton = {
                Button(onClick = { showBannedDialog = false }) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showBannedDialog = false
                    showAppealDialog = true 
                }) {
                    Text("¿Crees que es injusto? Apelar")
                }
            }
        )
    }

    if (showAppealDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAppealDialog = false 
                profileViewModel.resetActionState()
            },
            title = { Text("Apelar Suspensión") },
            text = {
                Column {
                    Text("Cuéntanos qué sucedió. Nuestro equipo de soporte revisará manualmente tu caso utilizando el registro histórico de las salas reportadas.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = appealText,
                        onValueChange = { appealText = it },
                        placeholder = { Text("Escribe tu explicación...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (profileActionState is Resource.Loading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                    if (profileActionState is Resource.Success) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Apelación enviada correctamente.", color = Color(0xFF4CAF50))
                    }
                    if (profileActionState is Resource.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text((profileActionState as Resource.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.submitAppeal(appealText, userProfile.getBannedRoomIds())
                    },
                    enabled = appealText.isNotBlank() && profileActionState !is Resource.Loading && profileActionState !is Resource.Success
                ) {
                    Text("Enviar Apelación")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAppealDialog = false 
                    profileViewModel.resetActionState()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Encabezado
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GenteHablando",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Conexiones humanas sin juicios.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = {
                        if (authViewModel.isUserLoggedIn()) {
                            onProfileClick()
                        } else {
                            // Le pasamos SPEAKER por defecto solo como 'intendedRole' para el router, 
                            // aunque en realidad la redirección final (tras login) se sobreescribirá 
                            // o te devolverá al Home.
                            onLoginClick(UserRole.SPEAKER) 
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Mi Perfil",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "¿Cuál es tu intención hoy?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas de selección de rol
            UserRole.values().forEach { role ->
                RoleOptionCard(
                    role = role,
                    isSelected = role == selectedRole,
                    onSelect = { selectedRole = role }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Academia",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nueva Card de Guías
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGuidesClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Academia",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Aprende a conectar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lecciones para mejores conversaciones.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Acciones inferiores
        Column {
            // Botón de acción principal
            Button(
                onClick = {
                    if (authViewModel.isUserLoggedIn()) {
                        // Sistema Avanzado de Moderación:
                        // Comprobamos si tiene 3 o más reportes efectivos en los últimos 30 días
                        if (userProfile.isBanned()) {
                            showBannedDialog = true
                        } else {
                            onConnectClick(selectedRole)
                        }
                    } else {
                        onLoginClick(selectedRole) // Pasamos el rol que el usuario intentaba elegir
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (authViewModel.isUserLoggedIn()) "Buscar coincidencia" else "Iniciar sesión para continuar",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RoleOptionCard(
    role: UserRole,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(width = if (isSelected) 2.dp else 1.dp, color = borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onSelect() }
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = role.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = role.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}