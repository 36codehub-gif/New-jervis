package com.example.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.model.InstalledAppInfo
import java.util.Locale

class InstalledAppManager(private val context: Context) {

    private val appCache = mutableListOf<InstalledAppInfo>()

    init {
        refreshIndex()
    }

    fun refreshIndex() {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val list = mutableListOf<InstalledAppInfo>()

        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            val aliases = generateAliases(label, packageName)
            list.add(InstalledAppInfo(label, packageName, aliases))
        }

        synchronized(appCache) {
            appCache.clear()
            appCache.addAll(list)
        }
    }

    private fun generateAliases(label: String, packageName: String): List<String> {
        val aliases = mutableSetOf<String>()
        val lowerLabel = label.lowercase(Locale.ROOT).trim()
        val lowerPkg = packageName.lowercase(Locale.ROOT)

        aliases.add(lowerLabel)
        aliases.add(lowerPkg)

        // Custom mappings for common apps in Indian ecosystem
        when {
            lowerPkg.contains("whatsapp") || lowerLabel.contains("whatsapp") -> {
                aliases.addAll(listOf("whatsapp", "whats app", "whatapp", "wa"))
            }
            lowerPkg.contains("youtube") || lowerLabel.contains("youtube") -> {
                aliases.addAll(listOf("youtube", "yt", "you tube", "video"))
            }
            lowerPkg.contains("instagram") || lowerLabel.contains("instagram") -> {
                aliases.addAll(listOf("instagram", "insta", "ig"))
            }
            lowerPkg.contains("camera") || lowerLabel.contains("camera") -> {
                aliases.addAll(listOf("camera", "cam", "photo", "tasveer", "photo khicho"))
            }
            lowerPkg.contains("chrome") || lowerLabel.contains("chrome") -> {
                aliases.addAll(listOf("chrome", "browser", "google chrome", "internet"))
            }
            lowerPkg.contains("calculator") || lowerLabel.contains("calculator") -> {
                aliases.addAll(listOf("calculator", "calc", "hisab", "ganit"))
            }
            lowerPkg.contains("settings") || lowerLabel.contains("setting") -> {
                aliases.addAll(listOf("settings", "setting", "phone settings", "system setting"))
            }
            lowerPkg.contains("maps") || lowerLabel.contains("maps") -> {
                aliases.addAll(listOf("maps", "map", "google maps", "naksha", "route"))
            }
            lowerPkg.contains("music") || lowerPkg.contains("spotify") || lowerLabel.contains("music") -> {
                aliases.addAll(listOf("music", "gana", "song", "spotify", "audio", "player"))
            }
            lowerPkg.contains("gallery") || lowerPkg.contains("photos") || lowerLabel.contains("gallery") -> {
                aliases.addAll(listOf("gallery", "photos", "photo gallery", "tasveerein"))
            }
            lowerPkg.contains("clock") || lowerPkg.contains("deskclock") || lowerLabel.contains("clock") -> {
                aliases.addAll(listOf("clock", "alarm", "ghadi", "samay", "timer"))
            }
            lowerPkg.contains("mms") || lowerPkg.contains("messaging") || lowerLabel.contains("messages") -> {
                aliases.addAll(listOf("messages", "sms", "message", "sandesh"))
            }
            lowerPkg.contains("dialer") || lowerPkg.contains("phone") || lowerLabel.contains("phone") -> {
                aliases.addAll(listOf("phone", "dialer", "call", "contacts", "sampark"))
            }
            lowerPkg.contains("telegram") || lowerLabel.contains("telegram") -> {
                aliases.addAll(listOf("telegram", "tg"))
            }
            lowerPkg.contains("twitter") || lowerPkg.contains("x") || lowerLabel.contains("twitter") -> {
                aliases.addAll(listOf("twitter", "x"))
            }
        }
        return aliases.toList()
    }

    fun findApp(query: String): InstalledAppInfo? {
        val q = query.lowercase(Locale.ROOT).trim()
        synchronized(appCache) {
            // 1. Exact match on alias
            val exact = appCache.firstOrNull { it.aliases.any { alias -> alias == q } }
            if (exact != null) return exact

            // 2. Starts with query
            val prefix = appCache.firstOrNull { it.label.lowercase(Locale.ROOT).startsWith(q) }
            if (prefix != null) return prefix

            // 3. Contains query
            val contains = appCache.firstOrNull {
                it.aliases.any { alias -> alias.contains(q) || q.contains(alias) }
            }
            if (contains != null) return contains

            // 4. Fallback search by category
            if (q.contains("music") || q.contains("gana")) {
                return appCache.firstOrNull { it.aliases.contains("music") || it.packageName.contains("music") || it.packageName.contains("spotify") }
            }
            if (q.contains("camera") || q.contains("photo")) {
                return appCache.firstOrNull { it.aliases.contains("camera") }
            }
            if (q.contains("browser") || q.contains("net")) {
                return appCache.firstOrNull { it.aliases.contains("chrome") || it.packageName.contains("browser") }
            }

            return null
        }
    }

    fun getAllApps(): List<InstalledAppInfo> {
        synchronized(appCache) {
            return appCache.toList()
        }
    }
}
