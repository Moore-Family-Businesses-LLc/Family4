package com.family4.app.di

// AiModule removed — wiring FamilyAIAssistant.actionDispatcher is done at runtime
// in MainActivity after both singletons are injected, avoiding a Hilt dependency cycle.
// See MainActivity.kt: agentDispatcher is @Inject-ed and set on the assistant after onCreate.
