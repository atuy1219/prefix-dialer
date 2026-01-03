package com.atuy.prefix_dialer

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atuy.prefix_dialer.ui.theme.PrefixdialerTheme

class MainActivity : ComponentActivity() {

    private val roleStatusText = mutableStateOf("")
    private val roleStatusColor = mutableStateOf(Color.Red)
    private val isRoleButtonEnabled = mutableStateOf(true)
    private val roleButtonText = mutableStateOf("通話転送アプリとして設定する")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrefixdialerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        roleStatusText = roleStatusText.value,
                        roleStatusColor = roleStatusColor.value,
                        isRoleButtonEnabled = isRoleButtonEnabled.value,
                        roleButtonText = roleButtonText.value,
                        onUpdateStatus = { updateRoleStatus() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateRoleStatus()
    }

    private fun updateRoleStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            val isHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION)

            if (isHeld) {
                roleStatusText.value = "状態：設定済み (動作可能です)"
                roleStatusColor.value = Color(0xFF00AA00)
                isRoleButtonEnabled.value = false
                roleButtonText.value = "設定完了"
            } else {
                roleStatusText.value = "状態：未設定 (動作しません)"
                roleStatusColor.value = Color.Red
                isRoleButtonEnabled.value = true
                roleButtonText.value = "通話転送アプリとして設定する"
            }
        } else {
            roleStatusText.value = "状態：非対応OS (Android 10以上が必要です)"
            roleStatusColor.value = Color.Red
            isRoleButtonEnabled.value = false
        }
    }
}

@Composable
fun MainScreen(
    roleStatusText: String,
    roleStatusColor: Color,
    isRoleButtonEnabled: Boolean,
    roleButtonText: String,
    onUpdateStatus: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    var prefix by remember { mutableStateOf(prefs.getString("prefix_key", "") ?: "") }
    var isEnabled by remember { mutableStateOf(prefs.getBoolean("is_enabled_key", false)) }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "設定されました！", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "設定がキャンセルされました", Toast.LENGTH_SHORT).show()
        }
        onUpdateStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "プレフィックス設定",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            label = { Text("例: 003769") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "機能を有効にする",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it }
            )
        }

        Button(
            onClick = {
                prefs.edit().apply {
                    putString("prefix_key", prefix)
                    putBoolean("is_enabled_key", isEnabled)
                    apply()
                }
                Toast.makeText(context, "設定を保存しました", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("設定を保存")
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 32.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Text(
            text = "システム権限設定",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = roleStatusText,
            color = roleStatusColor,
            modifier = Modifier.padding(bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
                        roleRequestLauncher.launch(intent)
                    } else {
                        Toast.makeText(context, "このデバイスでは利用できません", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = isRoleButtonEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(roleButtonText)
        }
    }
}
