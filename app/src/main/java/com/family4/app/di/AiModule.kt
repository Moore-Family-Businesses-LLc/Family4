package com.family4.app.di

import com.family4.app.ai.AppAgentActionDispatcher
import com.family4.app.ai.FamilyAIAssistant
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    /**
     * After both singletons are created, wire the dispatcher into the assistant.
     * This is called once at app startup.
     */
    @Provides
    @Singleton
    fun wireDispatcher(
        assistant:  FamilyAIAssistant,
        dispatcher: AppAgentActionDispatcher
    ): FamilyAIAssistant {
        assistant.actionDispatcher = dispatcher
        return assistant
    }
}
