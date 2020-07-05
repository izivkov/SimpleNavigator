package org.avmedia.simplenavigator

import android.accounts.AccountManager
import android.content.Context
import androidx.core.content.ContextCompat


object User {

    var userName: String = ""

    fun getUser(context: Context): String {
        if (userName.isNotBlank()) {
            return userName
        }

        val manager =
            ContextCompat.getSystemService(context, AccountManager::class.java)

        val list = manager!!.accounts

        for (account in list) {
            if (account.type.equals("com.google", ignoreCase = true)) {
                userName = account.name.split('@')[0]
                return userName
            }
        }
        return ""
    }
}