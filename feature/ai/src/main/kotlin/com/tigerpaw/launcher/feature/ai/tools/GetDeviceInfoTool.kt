package com.tigerpaw.launcher.feature.ai.tools

import android.content.Context
import android.os.Build
import com.tigerpaw.launcher.core.data.ai.AgentTool
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * Agentic tool that returns basic device information.
 *
 * Useful context for the model when answering device-specific questions.
 */
class GetDeviceInfoTool @Inject constructor(
    @ApplicationContext private val context: Context,
) : AgentTool {

    override val name = "get_device_info"

    override val description =
        "Returns basic information about this Android device: model, Android version, and locale."

    override val parametersSchema = """
        {
          "type": "object",
          "properties": {},
          "required": []
        }
    """.trimIndent()

    override suspend fun execute(argumentsJson: String): String {
        return try {
            JSONObject().apply {
                put("manufacturer", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("android_version", Build.VERSION.RELEASE)
                put("api_level", Build.VERSION.SDK_INT)
                put("locale", context.resources.configuration.locales[0].toLanguageTag())
            }.toString()
        } catch (e: Exception) {
            "Error retrieving device info: ${e.message}"
        }
    }
}
