package com.atuy.prefix_dialer

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_ROLE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefixInput = findViewById<EditText>(R.id.editPrefix)
        val enableSwitch = findViewById<Switch>(R.id.switchEnable)
        val saveButton = findViewById<Button>(R.id.btnSave)
        val requestRoleButton = findViewById<Button>(R.id.btnRequestRole)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // 設定の読み込み
        prefixInput.setText(prefs.getString("prefix_key", ""))
        enableSwitch.isChecked = prefs.getBoolean("is_enabled_key", false)

        // 保存ボタン
        saveButton.setOnClickListener {
            prefs.edit().apply {
                putString("prefix_key", prefixInput.text.toString())
                putBoolean("is_enabled_key", enableSwitch.isChecked)
                apply()
            }
            // this@MainActivity を指定して Activity の Context を渡す
            Toast.makeText(this@MainActivity, "設定を保存しました", Toast.LENGTH_SHORT).show()
        }

        requestRoleButton.setOnClickListener {
            requestCallRedirectionRole()
        }
    }

    private fun requestCallRedirectionRole() {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION)) {
            if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION)) {
                Toast.makeText(this, "既に通話転送アプリとして設定されています", Toast.LENGTH_SHORT).show()
            } else {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
                // Activity のメソッドを直接呼び出す
                startActivityForResult(intent, REQUEST_CODE_ROLE)
            }
        } else {
            Toast.makeText(this, "このデバイスでは通話転送機能が利用できません", Toast.LENGTH_SHORT).show()
        }
    }
}