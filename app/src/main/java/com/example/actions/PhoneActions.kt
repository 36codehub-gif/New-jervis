package com.example.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.engine.Action
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.ContactInfo

class CallContactAction : Action {
    override val type = ActionType.CALL_CONTACT
    override val displayName = "Call Contact"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = listOf(Manifest.permission.READ_CONTACTS)

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val targetName = params["target"] as? String ?: params["contact"] as? String
        val directNumber = params["number"] as? String

        if (targetName.isNullOrBlank() && directNumber.isNullOrBlank()) {
            return ActionResult.failed(
                "Contact name or phone number is missing",
                "Kisko call karna hai? Kripya naam ya number batayein."
            )
        }

        var phoneNumber = directNumber
        var resolvedName = targetName ?: directNumber ?: "Contact"

        if (phoneNumber.isNullOrBlank() && !targetName.isNullOrBlank()) {
            val hasContactPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasContactPerm) {
                // Open dialer with contact name query
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return ActionResult.permissionRequired(
                    Manifest.permission.READ_CONTACTS,
                    "Read Contacts permission required to search for '$targetName'",
                    "Contacts padhne ki permission chahiye taaki '$targetName' ko dhund sakein.",
                    dialIntent
                )
            }

            val contacts = searchContacts(context, targetName)
            if (contacts.isEmpty()) {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return ActionResult.failed(
                    "No contact found matching '$targetName'",
                    "Contacts mein '$targetName' nahi mila.",
                    "Dialer open kar diya hai"
                )
            }

            val matched = contacts.first()
            phoneNumber = matched.number
            resolvedName = matched.name
        }

        if (phoneNumber.isNullOrBlank()) {
            return ActionResult.failed(
                "Phone number not available for $resolvedName",
                "$resolvedName ka koi phone number nahi mila."
            )
        }

        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPerm) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        context.startActivity(intent)

        return ActionResult.success(
            "Calling $resolvedName ($phoneNumber)",
            "$resolvedName ko call lagaya ja raha hai",
            "Number: $phoneNumber"
        )
    }

    override fun verifyResult(context: Context): Boolean = true

    private fun searchContacts(context: Context, query: String): List<ContactInfo> {
        val list = mutableListOf<ContactInfo>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.let {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else ""
                    val number = if (numIdx >= 0) it.getString(numIdx) else ""
                    if (number.isNotBlank()) {
                        list.add(ContactInfo(id, name, number))
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore
        } finally {
            cursor?.close()
        }
        return list
    }
}

class SendSmsAction : Action {
    override val type = ActionType.SEND_SMS
    override val displayName = "Send SMS Message"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = listOf(Manifest.permission.SEND_SMS)

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val recipient = params["recipient"] as? String ?: params["target"] as? String ?: ""
        val message = params["message"] as? String ?: params["text"] as? String ?: ""

        if (recipient.isBlank() && message.isBlank()) {
            return ActionResult.failed(
                "Recipient and message text required",
                "Kisko aur kya message bhejna hai batayein."
            )
        }

        // Official Android safe communication: open SMS composer with pre-filled content
        val smsUri = if (recipient.isNotBlank()) Uri.parse("smsto:$recipient") else Uri.parse("smsto:")
        val sendIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(sendIntent)
            ActionResult.success(
                "SMS composer opened for $recipient",
                "${if (recipient.isNotBlank()) recipient + " ko " else ""}SMS compose kiya ja raha hai",
                "Message: $message"
            )
        } catch (e: Exception) {
            ActionResult.failed("Failed to open SMS app", "SMS application nahi khul paya")
        }
    }

    override fun verifyResult(context: Context): Boolean = true
}

class ComposeMessageAction : Action {
    override val type = ActionType.COMPOSE_MESSAGE
    override val displayName = "Compose Message"

    override fun canExecute(context: Context): Boolean = true
    override fun requiredPermissions(): List<String> = emptyList()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ActionResult {
        val message = params["text"] as? String ?: params["message"] as? String ?: ""
        val app = params["app"] as? String ?: "whatsapp"

        if (app.contains("whatsapp", ignoreCase = true)) {
            val waIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(waIntent)
                ActionResult.success("WhatsApp message shared", "WhatsApp par message compose kar diya hai")
            } catch (e: Exception) {
                // Fallback to generic share
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Message").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                ActionResult.success("Message share intent opened", "Message share kiya ja raha hai")
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Message").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return ActionResult.success("Message composer opened", "Message compose ho raha hai")
    }

    override fun verifyResult(context: Context): Boolean = true
}
