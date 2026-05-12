package com.tigerpaw.launcher.core.data.di

import com.tigerpaw.launcher.core.data.ai.AgentTool
import com.tigerpaw.launcher.core.data.ai.LocalAiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    /**
     * Declares the multibinding set of [AgentTool]s.
     * Feature modules contribute tools by using @[dagger.multibindings.IntoSet].
     */
    @Multibinds
    abstract fun agentTools(): Set<AgentTool>
}
