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

            // telスキーム以外（sipなど）はそのまま発信させる
            if (handle.scheme != "tel") {
                Log.d(TAG, "Not a tel scheme, passing through.")
                placeCallUnmodified()
                return
            }

            // 発信しようとしている電話番号を取得 (例: 09012345678)
            val originalNumber = handle.schemeSpecificPart
            Log.d(TAG, "Original number: $originalNumber")

            // 設定保存されたプレフィックスを取得
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val prefix = prefs.getString("prefix_key", "") ?: ""
            val isEnabled = prefs.getBoolean("is_enabled_key", false)

            Log.d(TAG, "Settings - Enabled: $isEnabled, Prefix: $prefix")

            // プレフィックスが無効、または空の場合は何もしない
            if (!isEnabled || prefix.isEmpty()) {
                Log.d(TAG, "Prefix disabled or empty.")
                placeCallUnmodified()
                return
            }

            // 番号書き換えの判定ロジック
            if (shouldAddPrefix(originalNumber)) {
                // プレフィックスを付与して発信
                val newNumber = prefix + originalNumber
                val newHandle = Uri.fromParts("tel", newNumber, null)

                Log.d(TAG, "Redirecting to: $newNumber")

                // 書き換えた番号で発信
                // 確認画面を出さずにリダイレクト (confirmFirst = false)
                redirectCall(newHandle, initialPhoneAccount, false)
            } else {
                Log.d(TAG, "No prefix needed.")
                placeCallUnmodified()
            }
        } catch (e: Exception) {
            // ここでエラーをキャッチしないと、電話発信画面が固まる可能性がある
            Log.e(TAG, "Error in onPlaceCall", e)
            placeCallUnmodified()
        }
    }

    private fun shouldAddPrefix(number: String): Boolean {
        // すでにプレフィックスがついている、または短い番号（緊急通報など）は除外
        if (number.length < 4) return false // 110, 119など

        // 特番やフリーダイヤルを除外 (0120, 0800, 0570, #, *)
        if (number.startsWith("0120") ||
            number.startsWith("0800") ||
            number.startsWith("0570") ||
            number.startsWith("#") ||
            number.startsWith("*")) {
            return false
        }

        // 携帯番号(090,080,070)や固定電話(03,06など)を対象とする
        // ※必要に応じて条件を調整してください
        return number.startsWith("0")
    }
}