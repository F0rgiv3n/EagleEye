package com.eagleeye.modules.security

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eagleeye.data.SecurityScore
import com.eagleeye.data.Threat
import com.eagleeye.data.ThreatLevel
import com.eagleeye.data.db.AppDatabase
import com.eagleeye.widget.SecurityWidgetReceiver
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
                updateWidget(score, score.threats)
            } catch (e: Exception) {
                _auditState.value = AuditState.Error(e.message ?: "Audit failed")
            }
        }
    }

    private fun updateWidget(score: SecurityScore, threats: List<Threat>) {
        val app = getApplication<Application>()
        val criticalHighCount = threats.count {
            it.level == ThreatLevel.CRITICAL || it.level == ThreatLevel.HIGH
        }
        val prefs = app.getSharedPreferences("eagle_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("security_grade", score.grade)
            .putInt("threat_count", criticalHighCount)
            .putString(
                "last_scan",
                java.text.SimpleDateFormat("HH:mm dd/MM", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )
            .apply()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.component = ComponentName(app, SecurityWidgetReceiver::class.java)
        app.sendBroadcast(intent)
    }
}
