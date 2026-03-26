package com.eagleeye.modules.security

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.SecurityScore
import com.eagleeye.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuditState {
    object Idle : AuditState()
    object Running : AuditState()
    data class Result(val score: SecurityScore) : AuditState()
    data class Error(val message: String) : AuditState()
}

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val detector = ThreatDetector(application)
    private val dao = AppDatabase.getInstance(application).lanDeviceDao()

    private val _auditState = MutableStateFlow<AuditState>(AuditState.Idle)
    val auditState: StateFlow<AuditState> = _auditState.asStateFlow()

    init {
        runAudit()
    }

    fun runAudit() {
        viewModelScope.launch(Dispatchers.IO) {
            _auditState.value = AuditState.Running
            try {
                val unknownCount = dao.getAll().count { !it.isKnown }
                val score = detector.runFullAudit(unknownCount)
                _auditState.value = AuditState.Result(score)
            } catch (e: Exception) {
                _auditState.value = AuditState.Error(e.message ?: "Audit failed")
            }
        }
    }
}
