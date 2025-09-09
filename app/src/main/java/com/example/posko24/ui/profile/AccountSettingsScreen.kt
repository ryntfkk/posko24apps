package com.example.posko24.ui.profile


import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    Scaffold(
        topBar = { TopAppBar(title = { Text("Pengaturan Akun") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = viewModel.fullName.value,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.phoneNumber.value,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Nomor Telepon") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.profilePictureUrl.value,
                onValueChange = viewModel::onPhotoUrlChange,
                label = { Text("URL Foto Profil") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.saveProfile { success ->
                        val msg = if (success) "Profil diperbarui" else "Gagal memperbarui profil"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Profil")
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = viewModel.newPassword.value,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password Baru") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.updatePassword { success ->
                        val msg = if (success) "Password diperbarui" else "Gagal memperbarui password"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ubah Password")
            }
        }
    }
}