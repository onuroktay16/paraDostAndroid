package com.scoreplus.app

import android.app.Application
import com.scoreplus.app.data.local.AppDatabase
import com.scoreplus.app.data.remote.NetworkClient
import com.scoreplus.app.data.remote.SyncWorker
import com.scoreplus.app.data.remote.TokenStore
import com.scoreplus.app.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ScorePlusApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val tokenStore by lazy { TokenStore(this) }
    val api by lazy { NetworkClient.create(tokenStore) }
    val repository by lazy { FinanceRepository(database, api, tokenStore) }

    override fun onCreate() {
        super.onCreate()
        SyncWorker.schedule(this)
    }
}
