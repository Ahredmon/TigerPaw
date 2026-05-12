package com.tigerpaw.launcher.feature.ai.di

import com.tigerpaw.launcher.core.data.ai.AgentTool
import com.tigerpaw.launcher.feature.ai.tools.GetDeviceInfoTool
import com.tigerpaw.launcher.feature.ai.tools.LaunchAppTool
import com.tigerpaw.launcher.feature.ai.tools.SearchAppsTool
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class AiToolsModule {

    @Binds
    @IntoSet
    abstract fun bindLaunchAppTool(tool: LaunchAppTool): AgentTool

    @Binds
    @IntoSet
    abstract fun bindSearchAppsTool(tool: SearchAppsTool): AgentTool

    @Binds
    @IntoSet
    abstract fun bindGetDeviceInfoTool(tool: GetDeviceInfoTool): AgentTool
}
