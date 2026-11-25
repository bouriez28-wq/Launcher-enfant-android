package com.example.launchertlphoneenfant.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.launchertlphoneenfant.data.database.AppDatabase
import com.example.launchertlphoneenfant.data.model.AppRestriction
import com.example.launchertlphoneenfant.data.model.Routine
import com.example.launchertlphoneenfant.data.preferences.ParentPreferencesRepository
import com.example.launchertlphoneenfant.data.remote.FirebaseSyncRepository
import com.example.launchertlphoneenfant.domain.AppModel
import com.example.launchertlphoneenfant.domain.InstalledAppsRepository
import com.example.launchertlphoneenfant.domain.UsageStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LauncherUiState(
    val visibleApps: List<AppModel> = emptyList(),
    val usedMinutesToday: Int = 0,
    val dailyLimitMinutes: Int = 60,
    val bonusMinutesToday: Int = 0,
    val routines: List<Routine> = emptyList(),
    val currentTheme: String = "SHONEN",
    val isTimeUp: Boolean = false,
    val familyCode: String = "FAM-8492"
)

class ChildLauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appsRepository = InstalledAppsRepository(application)
    private val usageStatsRepository = UsageStatsRepository(application)
    private val preferencesRepository = ParentPreferencesRepository(application)
    private val firebaseSyncRepository = FirebaseSyncRepository()
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        refreshData()
        startCloudSync()
    }

    private fun startCloudSync() {
        viewModelScope.launch {
            val familyCode = preferencesRepository.familyCode.first()
            _uiState.value = _uiState.value.copy(familyCode = familyCode)

            firebaseSyncRepository.observeFamilyState(familyCode).collect { remote ->
                if (remote != null) {
                    preferencesRepository.setDailyTimeLimit(remote.dailyLimitMinutes)
                    preferencesRepository.setSelectedTheme(remote.selectedTheme)
                    preferencesRepository.setParentPin(remote.parentPin)

                    remote.hiddenPackages.forEach { pkg ->
                        db.appRestrictionDao().insertOrUpdate(
                            AppRestriction(packageName = pkg, appName = pkg, isHidden = true)
                        )
                    }

                    refreshData()
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val allApps = appsRepository.getInstalledApps()

            combine(
                db.appRestrictionDao().getAllRestrictions(),
                preferencesRepository.dailyTimeLimitMinutes,
                preferencesRepository.bonusTimeMinutes,
                preferencesRepository.selectedTheme
            ) { restrictions, limit, bonus, theme ->

                val hiddenPackages = restrictions.filter { it.isHidden }.map { it.packageName }.toSet()
                val visibleApps = allApps.filter { it.packageName !in hiddenPackages }

                val used = usageStatsRepository.getTodayUsageTimeMinutes()
                val totalAllowed = limit + bonus
                val isTimeUp = used >= totalAllowed && totalAllowed > 0

                val familyCode = preferencesRepository.familyCode.first()
                firebaseSyncRepository.reportUsageToCloud(familyCode, used)

                _uiState.value = _uiState.value.copy(
                    visibleApps = visibleApps,
                    usedMinutesToday = used,
                    dailyLimitMinutes = limit,
                    bonusMinutesToday = bonus,
                    currentTheme = theme,
                    isTimeUp = isTimeUp
                )
            }.collect {}
        }

        viewModelScope.launch {
            db.routineDao().getAllRoutines().collect { routines ->
                _uiState.value = _uiState.value.copy(routines = routines)
            }
        }
    }

    fun completeRoutine(routine: Routine) {
        viewModelScope.launch {
            if (!routine.isCompleted) {
                db.routineDao().update(routine.copy(isCompleted = true))
                preferencesRepository.addBonusTime(routine.rewardMinutes)
                refreshData()
            }
        }
    }

    fun launchApp(packageName: String) {
        appsRepository.launchApp(packageName)
    }
}
