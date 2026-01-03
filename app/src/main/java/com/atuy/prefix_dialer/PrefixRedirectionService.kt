package com.atuy.prefix_dialer

import android.content.Context
import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log

class PrefixRedirectionService : CallRedirectionService() {

    companion object {
        private const val TAG = "PrefixRedirection"
    }

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        try {
            Log.d(TAG, "onPlaceCall: handle=$handle")

            if (handle.scheme != "tel") {
                Log.d(TAG, "Not a tel scheme, passing through.")
                placeCallUnmodified()
                return
            }

            val originalNumber = handle.schemeSpecificPart
            Log.d(TAG, "Original number: $originalNumber")

            val normalizedNumber = if (originalNumber.startsWith("+81")) {
                "0${originalNumber.removePrefix("+81")}"
            } else {
                originalNumber
            }

            Log.d(TAG, "Normalized number: $normalizedNumber")

            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val prefix = prefs.getString("prefix_key", "") ?: ""
            val isEnabled = prefs.getBoolean("is_enabled_key", false)

            Log.d(TAG, "Settings - Enabled: $isEnabled, Prefix: $prefix")

            if (!isEnabled || prefix.isEmpty()) {
                Log.d(TAG, "Prefix disabled or empty.")
                placeCallUnmodified()
                return
            }

            if (shouldAddPrefix(normalizedNumber, prefix)) {
                val newNumber = prefix + normalizedNumber
                val newHandle = Uri.fromParts("tel", newNumber, null)

                Log.d(TAG, "Redirecting to: $newNumber")

                redirectCall(newHandle, initialPhoneAccount, false)
            } else {
                Log.d(TAG, "No prefix needed.")
                placeCallUnmodified()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPlaceCall", e)
            placeCallUnmodified()
        }
    }

    private fun shouldAddPrefix(number: String, prefix: String): Boolean {
        val noPrefixPrefixes = listOf(
            "104", // 番号案内
            "110", // 警察
            "115", // 電報
            "117", // 時報
            "118", // 海上保安庁
            "119", // 消防署
            "171", // 災害用伝言ダイヤル
            "188", // 消費者ホットライン
            "189", // 児童相談所全国共通ダイヤル
            "0120", // 着信課金系サービス
            "0800",  // 着信課金系サービス
            "0570", // ナビダイヤル
            "0180", // 67コール
            "0067", // テレドーム
            "010" // 国際通話発信用プレフィックス
        )

        if (number.length < 3) return false

        if (number.startsWith("+")) return false

        if (prefix.isNotEmpty() && number.startsWith(prefix)) return false

        if (noPrefixPrefixes.any { number.startsWith(it) }) return false

        if (number.startsWith("#") || number.startsWith("*")) return false

        return number.startsWith("0")
    }
}