package com.tigerpaw.launcher.feature.ai.tools

import android.content.Context
import android.content.pm.PackageManager
import com.tigerpaw.launcher.core.data.ai.AgentTool
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * Agentic tool that searches the list of installed apps by label or package name.
 *
 * Returns a JSON array of matches so the model can pick the right package for [LaunchAppTool].
 */
class SearchAppsTool @Inject constructor(
    @ApplicationContext private val context: Context,
) : AgentTool {

    override val name = "search_apps"

    override val description =
        "Search installed Android apps by name or package name. " +
        "Returns up to 10 matching apps with their labels and package names."

    override val parametersSchema = """
        {
          "type": "object",
          "properties": {
            "query": {
              "type": "string",
              "description": "A partial app name or package name to search for"
            }
          },
          "required": ["query"]
        }
    """.trimIndent()

    override suspend fun execute(argumentsJson: String): String {
        return try {
            val args = JSONObject(argumentsJson)
            val query = args.getString("query").lowercase()

            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val matches = installed
                .filter { app ->
                    val label = pm.getApplicationLabel(app).toString().lowercase()
                    label.contains(query) || app.packageName.lowercase().contains(query)
                }
                .take(10)
                .map { app ->
                    JSONObject().apply {
                        put("label", pm.getApplicationLabel(app).toString())
                        put("package_name", app.packageName)
                    }
                }

            if (matches.isEmpty()) {
                "No apps found matching '$query'."
            } else {
                org.json.JSONArray(matches).toString()
            }
        } catch (e: Exception) {
            "Error searching apps: ${e.message}"
        }
    }
}
