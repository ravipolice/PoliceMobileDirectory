package com.example.policemobiledirectory.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object IntentUtils {

    fun dial(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSms(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open messaging app", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")
            }
            context.startActivity(Intent.createChooser(intent, "Open with WhatsApp"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Share contact Info"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share info", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(context: Context, email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open email app", Toast.LENGTH_SHORT).show()
        }
    }

    fun addToContacts(context: Context, name: String, phone: String?, email: String?, company: String?) {
        try {
            val intent = Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
                type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(android.provider.ContactsContract.Intents.Insert.NAME, name)
                if (phone != null) putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, phone)
                if (email != null) putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, email)
                if (company != null) putExtra(android.provider.ContactsContract.Intents.Insert.COMPANY, company)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open contacts app", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not copy to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
}
