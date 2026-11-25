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
import com.example.launchertlphoneenfant.domain.KioskManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ParentUiState(
    val allApps: List<AppModel> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
    val parentPin: String = "0000",
    val dailyLimitMinutes: Int = 60,
    val bonusMinutesToday: Int = 0,
    val routines: List<Routine> = emptyList(),
    val selectedTheme: String = "SHONEN",
    val familyCode: String = "FAM-8492"
)

class ParentViewModel(application: Application) : AndroidViewModel(application) {

    private val appsRepository = InstalledAppsRepository(application)
    private val preferencesRepository = ParentPreferencesRepository(application)
    private val firebaseSyncRepository = FirebaseSyncRepository()
    private val db = AppDatabase.getInstance(application)
    private val kioskManager = KioskManager()

    private val _uiState = MutableStateFlow(ParentUiState())
    val uiState: StateFlow<ParentUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val installedApps = appsRepository.getInstalledApps()

            combine(
                db.appRestrictionDao().getAllRestrictions(),
                preferencesRepository.parentPin,
                preferencesRepository.dailyTimeLimitMinutes,
                preferencesRepository.bonusTimeMinutes,
                preferencesRepository.selectedTheme
            ) { restrictions, pin, limit, bonus, theme ->

                val hiddenSet = restrictions.filter { it.isHidden }.map { it.packageName }.toSet()
                val appsWithStatus = installedApps.map { app ->
                    app.copy(isHidden = app.packageName in hiddenSet)
                }

                _uiState.value = _uiState.value.copy(
                    allApps = appsWithStatus,
                    hiddenPackages = hiddenSet,
                    parentPin = pin,
                    dailyLimitMinutes = limit,
                    bonusMinutesToday = bonus,
                    selectedTheme = theme
                )
            }.collect {}
        }

        viewModelScope.launch {
            preferencesRepository.familyCode.collect { code ->
                _uiState.value = _uiState.value.copy(familyCode = code)
            }
        }

        viewModelScope.launch {
            db.routineDao().getAllRoutines().collect { routines ->
                _uiState.value = _uiState.value.copy(routines = routines)
            }
        }
    }

    fun toggleAppVisibility(packageName: String, appName: String, hide: Boolean) {
        viewModelScope.launch {
            val restriction = AppRestriction(
                packageName = packageName,
                appName = appName,
                isHidden = hide
            )
            db.appRestrictionDao().insertOrUpdate(restriction)

            val updatedHiddenList = _uiState.value.hiddenPackages.toMutableSet().apply {
                if (hide) add(packageName) else remove(packageName)
            }.toList()

            firebaseSyncRepository.updateRemoteSettings(
                _uiState.value.familyCode,
                mapOf("hiddenPackages" to updatedHiddenList)
            )
        }
    }

    fun setParentPin(newPin: String) {
        viewModelScope.launch {
            preferencesRepository.setParentPin(newPin)
            firebaseSyncRepository.updateRemoteSettings(
                _uiState.value.familyCode,
                mapOf("parentPin" to newPin)
            )
        }
    }

    fun setDailyLimit(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyTimeLimit(minutes)
            firebaseSyncRepository.updateRemoteSettings(
                _uiState.value.familyCode,
                mapOf("dailyLimitMinutes" to minutes)
            )
        }
    }

    fun addBonusMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.addBonusTime(minutes)
            val newTotalBonus = _uiState.value.bonusMinutesToday + minutes
            firebaseSyncRepository.updateRemoteSettings(
                _uiState.value.familyCode,
                mapOf("bonusMinutesToday" to newTotalBonus)
            )
        }
    }

    fun setSelectedTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedTheme(theme)
            firebaseSyncRepository.updateRemoteSettings(
                _uiState.value.familyCode,
                mapOf("selectedTheme" to theme)
            )
        }
    }

    fun setFamilyCode(code: String) {
        viewModelScope.launch {
            preferencesRepository.setFamilyCode(code)
        }
    }

    fun addRoutine(title: String, description: String, rewardMinutes: Int) {
        viewModelScope.launch {
            db.routineDao().insert(
                Routine(
                    title = title,
                    description = description,
                    rewardMinutes = rewardMinutes
                )
            )
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            db.routineDao().delete(routine)
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        return kioskManager.verifyPin(enteredPin, _uiState.value.parentPin)
    }
}
