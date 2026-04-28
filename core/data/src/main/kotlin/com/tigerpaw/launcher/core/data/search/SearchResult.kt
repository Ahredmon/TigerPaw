package com.tigerpaw.launcher.core.data.search

import android.graphics.drawable.Drawable
import android.net.Uri
import com.tigerpaw.launcher.core.data.apps.AppInfo

enum class ResultType { APP, FILE, APP_ACTION, SUGGESTION }

sealed class SearchResult {
    abstract val type: ResultType
    abstract val label: String
    abstract val subtitle: String

    data class App(
        val app: AppInfo,
        override val label: String = app.label,
        override val subtitle: String = app.packageName,
    ) : SearchResult() {
        override val type = ResultType.APP
    }

    data class File(
        val uri: Uri,
        val mimeType: String,
        override val label: String,
        override val subtitle: String,
    ) : SearchResult() {
        override val type = ResultType.FILE
    }

    data class AppAction(
        val shortcutId: String,
        val packageName: String,
        val shortcutLabel: String,
        val appLabel: String,
        val icon: Drawable?,
        override val label: String = shortcutLabel,
        override val subtitle: String = appLabel,
    ) : SearchResult() {
        override val type = ResultType.APP_ACTION
    }

    data class Suggestion(
        val app: AppInfo,
        val launchCount: Int,
        override val label: String = app.label,
        override val subtitle: String = app.packageName,
    ) : SearchResult() {
        override val type = ResultType.SUGGESTION
    }
}
