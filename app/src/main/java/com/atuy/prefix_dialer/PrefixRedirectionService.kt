package com.atuy.prefix_dialer

import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.content.Context

class PrefixRedirectionService : CallRedirectionService() {

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        // 発信しようとしている電話番号を取得 (例: 09012345678)
        val originalNumber = handle.schemeSpecificPart

        // 設定保存されたプレフィックスを取得
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val prefix = prefs.getString("prefix_key", "") ?: ""
        val isEnabled = prefs.getBoolean("is_enabled_key", false)

        // プレフィックスが無効、または空の場合は何もしない
        if (!isEnabled || prefix.isEmpty()) {
            placeCallUnmodified()
            return
        }

        // 番号書き換えの判定ロジック
        if (shouldAddPrefix(originalNumber)) {
            // プレフィックスを付与して発信
            val newNumber = prefix + originalNumber
            val newHandle = Uri.fromParts("tel", newNumber, null)

            // 書き換えた番号で発信
            // Java メソッドのため名前付き引数 (confirmFirst =) は使用不可
            redirectCall(newHandle, initialPhoneAccount, false)
        } else {
            // 対象外の番号はそのまま発信
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