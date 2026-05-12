package com.tigerpaw.launcher.feature.ai.tools

import android.content.Context
import android.content.Intent
import com.tigerpaw.launcher.core.data.ai.AgentTool
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * Agentic tool that launches an installed app by its package name.
 *
 * The model should call this when the user asks to open or launch an application.
 */
class LaunchAppTool @Inject constructor(
    @ApplicationContext private val context: Context,
) : AgentTool {

    override val name = "launch_app"

    override val description =
        "Launch an installed Android app by its package name. " +
        "Use search_apps first if you don't know the exact package name."

    override val parametersSchema = """
        {
          "type": "object",
          "properties": {
            "package_name": {
              "type": "string",
              "description": "The fully-qualified Android package name, e.g. com.android.settings"
            }
          },
          "required": ["package_name"]
        }
    """.trimIndent()

    override suspend fun execute(argumentsJson: String): String {
        return try {
            val args = JSONObject(argumentsJson)
            val pkg = args.getString("package_name")
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                ?: return "Error: no launch intent found for package '$pkg'."
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Launched $pkg successfully."
        } catch (e: Exception) {
            "Error launching app: ${e.message}"
        }
    }
}
