package com.family4.app.auto

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Android Auto entry point for Family4.
 * Declared in AndroidManifest with androidx.car.app.CarAppService intent filter.
 */
class Family4CarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = Family4Session()
}
