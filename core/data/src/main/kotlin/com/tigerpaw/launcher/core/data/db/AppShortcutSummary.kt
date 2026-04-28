package com.tigerpaw.launcher.core.data.db

data class AppShortcutSummary(
    val packageName: String,
    val shortcutId: String,
    val launchCount: Int,
)
