package com.atuy.prefix_dialer

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var requestRoleButton: Button

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "設定されました！", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "設定がキャンセルされました", Toast.LENGTH_SHORT).show()
        }
        updateRoleStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefixInput = findViewById<EditText>(R.id.editPrefix)
        val enableSwitch = findViewById<Switch>(R.id.switchEnable)
        val saveButton = findViewById<Button>(R.id.btnSave)

        statusTextView = findViewById(R.id.textStatus)
        requestRoleButton = findViewById(R.id.btnRequestRole)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        prefixInput.setText(prefs.getString("prefix_key", ""))
        enableSwitch.isChecked = prefs.getBoolean("is_enabled_key", false)

        saveButton.setOnClickListener {
            prefs.edit().apply {
                putString("prefix_key", prefixInput.text.toString())
                putBoolean("is_enabled_key", enableSwitch.isChecked)
                apply()
            }
            Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show()
        }

        requestRoleButton.setOnClickListener {
            requestCallRedirectionRole()
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
                statusTextView.text = "状態：設定済み (動作可能です)"
                statusTextView.setTextColor(Color.parseColor("#00AA00"))
                requestRoleButton.isEnabled = false
                requestRoleButton.text = "設定完了"
            } else {
                statusTextView.text = "状態：未設定 (動作しません)"
                statusTextView.setTextColor(Color.RED)
                requestRoleButton.isEnabled = true
                requestRoleButton.text = "通話転送アプリとして設定する"
            }
        } else {
            statusTextView.text = "状態：非対応OS (Android 10以上が必要です)"
            statusTextView.setTextColor(Color.RED)
            requestRoleButton.isEnabled = false
        }
    }

    private fun requestCallRedirectionRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
                roleRequestLauncher.launch(intent)
            } else {
                Toast.makeText(this, "このデバイスでは利用できません", Toast.LENGTH_SHORT).show()
            }
        }
    }
}