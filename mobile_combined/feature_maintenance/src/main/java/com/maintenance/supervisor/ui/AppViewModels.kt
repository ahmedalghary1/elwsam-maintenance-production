package com.maintenance.supervisor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maintenance.supervisor.domain.model.EmergencyMaintenance
import com.maintenance.supervisor.domain.model.MaintenanceAnswer
import com.maintenance.supervisor.data.remote.NetworkMonitor
import com.maintenance.supervisor.domain.repository.*
import com.maintenance.supervisor.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(val phone: String = "", val password: String = "", val loading: Boolean = false, val error: String? = null, val done: Boolean = false)
@HiltViewModel class LoginViewModel @Inject constructor(private val auth: AuthRepository, private val maintenance: MaintenanceRepository, private val scheduler: SyncScheduler) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState()); val state = _state.asStateFlow()
    fun phone(value: String) { _state.update { it.copy(phone = value, error = null) } }
    fun password(value: String) { _state.update { it.copy(password = value, error = null) } }
    fun submit() { if (_state.value.loading) return; viewModelScope.launch {
        val s = _state.value
        if (s.phone.isBlank() || s.password.isBlank()) { _state.update { it.copy(error = "أدخل رقم الهاتف وكلمة المرور.") }; return@launch }
        _state.update { it.copy(loading = true, error = null) }
        when (val result = auth.login(s.phone, s.password)) {
            is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            is AppResult.Success -> { maintenance.bootstrap(); scheduler.enqueue(); _state.update { it.copy(loading = false, done = true, password = "") } }
        }
    } }
}

data class HomeUiState(val snapshot: HomeSnapshot = HomeSnapshot(null, null, null, null), val refreshing: Boolean = false, val message: String? = null, val connected: Boolean = false)
@HiltViewModel class HomeViewModel @Inject constructor(private val repository: MaintenanceRepository, private val auth: AuthRepository, private val scheduler: SyncScheduler, network: NetworkMonitor) : ViewModel() {
    private val transient = MutableStateFlow(Pair(false, null as String?))
    val state = combine(repository.observeHome(), transient, network.connected) { home, t, connected -> HomeUiState(home, t.first, t.second, connected) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
    init {
        scheduler.enqueue()
        refresh()
        startPeriodicSync()
    }
    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (true) {
                delay(25_000)
                repository.sync()
            }
        }
    }
    fun onResume() {
        viewModelScope.launch {
            repository.sync()
        }
    }
    fun refresh() = viewModelScope.launch { transient.value = true to null; val r = repository.sync(); transient.value = false to (r as? AppResult.Error)?.message }
    fun start(onReady: () -> Unit) = viewModelScope.launch { when (val r = repository.startOrLoadToday()) { is AppResult.Success -> onReady(); is AppResult.Error -> transient.value = false to r.message } }
    fun selectAsset(assetId: Int) = viewModelScope.launch {
        transient.value = true to null
        when (val result = repository.selectCurrentAsset(assetId)) {
            is AppResult.Success -> transient.value = false to null
            is AppResult.Error -> transient.value = false to result.message
        }
    }
    fun resetTodayReport() = viewModelScope.launch {
        transient.value = true to null
        when (val r = repository.resetTodayReport()) {
            is AppResult.Success -> transient.value = false to null
            is AppResult.Error -> transient.value = false to r.message
        }
    }
    fun logout(onDone: () -> Unit) = viewModelScope.launch { auth.logout(); onDone() }
}

data class PersonnelDraft(
    val cleanerName: String = "",
    val mechanicalTechnician: String = "",
    val electricalTechnician: String = "",
    val maintenanceManager: String = ""
)

data class InspectionUiState(
    val home: HomeSnapshot? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val emergencyItems: List<EmergencyMaintenance> = emptyList(),
    val cleanerName: String = "",
    val mechanicalTechnician: String = "",
    val electricalTechnician: String = "",
    val maintenanceManager: String = ""
)
@HiltViewModel class InspectionViewModel @Inject constructor(private val repository: MaintenanceRepository, private val scheduler: SyncScheduler) : ViewModel() {

    /**
     * Local answer overrides — updated IMMEDIATELY on user interaction.
     * This ensures that when LazyColumn recycles items on scroll, the
     * latest checked/note values survive even if the async DB write
     * hasn't completed yet.
     */
    private val _answerOverrides = MutableStateFlow<Map<Int, MaintenanceAnswer>>(emptyMap())
    private val _emergencyItems = MutableStateFlow<List<EmergencyMaintenance>?>(null)
    private val _personnelDraft = MutableStateFlow<PersonnelDraft?>(null)
    private val _completion = MutableStateFlow(Pair(false, null as String?))

    val state: StateFlow<InspectionUiState> = combine(
        repository.observeHome(),
        _answerOverrides,
        _emergencyItems,
        _personnelDraft,
        _completion
    ) { home, overrides, localEmergency, localPersonnel, completion ->
        if (home.daily?.report == null) {
            val p = localPersonnel ?: PersonnelDraft()
            return@combine InspectionUiState(
                home, completion.first, completion.second, localEmergency.orEmpty(),
                p.cleanerName, p.mechanicalTechnician, p.electricalTechnician, p.maintenanceManager
            )
        }
        val report = home.daily.report
        val mergedAnswers = (report.answers + overrides.values)
            .associateBy { it.checklistItemId }
            .values
            .toList()
        val effectiveEmergency = localEmergency ?: report.emergencyMaintenances
        val cleaner = localPersonnel?.cleanerName ?: report.cleanerName
        val mechanical = localPersonnel?.mechanicalTechnician ?: report.mechanicalTechnician
        val electrical = localPersonnel?.electricalTechnician ?: report.electricalTechnician
        val manager = localPersonnel?.maintenanceManager ?: report.maintenanceManager

        val mergedReport = report.copy(
            answers = mergedAnswers,
            emergencyMaintenances = effectiveEmergency,
            cleanerName = cleaner,
            mechanicalTechnician = mechanical,
            electricalTechnician = electrical,
            maintenanceManager = manager
        )
        val mergedDaily = home.daily.copy(report = mergedReport)
        val mergedHome = home.copy(daily = mergedDaily)
        InspectionUiState(
            mergedHome, completion.first, completion.second, effectiveEmergency,
            cleaner, mechanical, electrical, manager
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InspectionUiState())

    fun update(itemId: Int, checked: Boolean, note: String) {
        val answer = MaintenanceAnswer(itemId, checked, note)
        // 1) Immediately update local state — this is SYNCHRONOUS
        //    so the merged state is available on the very next frame,
        //    even before the DB write completes.
        _answerOverrides.update { current -> current + (itemId to answer) }

        // 2) Persist to DB asynchronously
        val id = state.value.home?.daily?.report?.clientReportId ?: return
        viewModelScope.launch { repository.saveAnswer(id, answer) }
    }

    fun addEmergency(defaultAssetId: Int) {
        val current = (_emergencyItems.value ?: state.value.home?.daily?.report?.emergencyMaintenances).orEmpty()
        val updated = current + EmergencyMaintenance(
            assetId = defaultAssetId,
            issueDescription = "",
            responsiblePerson = "",
            notes = ""
        )
        _emergencyItems.value = updated
        persistEmergency(updated)
    }

    fun updateEmergency(index: Int, item: EmergencyMaintenance) {
        val current = (_emergencyItems.value ?: state.value.home?.daily?.report?.emergencyMaintenances).orEmpty().toMutableList()
        if (index in current.indices) {
            current[index] = item
            _emergencyItems.value = current
            persistEmergency(current)
        }
    }

    fun removeEmergency(index: Int) {
        val current = (_emergencyItems.value ?: state.value.home?.daily?.report?.emergencyMaintenances).orEmpty().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _emergencyItems.value = current
            persistEmergency(current)
        }
    }

    private fun persistEmergency(items: List<EmergencyMaintenance>) {
        val id = state.value.home?.daily?.report?.clientReportId ?: return
        viewModelScope.launch { repository.saveEmergencyItems(id, items) }
    }

    fun updateCleanerName(name: String) {
        val current = _personnelDraft.value ?: PersonnelDraft(
            state.value.cleanerName, state.value.mechanicalTechnician,
            state.value.electricalTechnician, state.value.maintenanceManager
        )
        val updated = current.copy(cleanerName = name)
        _personnelDraft.value = updated
        persistPersonnel(updated)
    }

    fun updateMechanicalTechnician(name: String) {
        val current = _personnelDraft.value ?: PersonnelDraft(
            state.value.cleanerName, state.value.mechanicalTechnician,
            state.value.electricalTechnician, state.value.maintenanceManager
        )
        val updated = current.copy(mechanicalTechnician = name)
        _personnelDraft.value = updated
        persistPersonnel(updated)
    }

    fun updateElectricalTechnician(name: String) {
        val current = _personnelDraft.value ?: PersonnelDraft(
            state.value.cleanerName, state.value.mechanicalTechnician,
            state.value.electricalTechnician, state.value.maintenanceManager
        )
        val updated = current.copy(electricalTechnician = name)
        _personnelDraft.value = updated
        persistPersonnel(updated)
    }

    fun updateMaintenanceManager(name: String) {
        val current = _personnelDraft.value ?: PersonnelDraft(
            state.value.cleanerName, state.value.mechanicalTechnician,
            state.value.electricalTechnician, state.value.maintenanceManager
        )
        val updated = current.copy(maintenanceManager = name)
        _personnelDraft.value = updated
        persistPersonnel(updated)
    }

    private fun persistPersonnel(draft: PersonnelDraft) {
        val id = state.value.home?.daily?.report?.clientReportId ?: return
        viewModelScope.launch {
            repository.savePersonnel(
                id, draft.cleanerName, draft.mechanicalTechnician,
                draft.electricalTechnician, draft.maintenanceManager
            )
        }
    }

    fun complete(onDone: () -> Unit) {
        if (_completion.value.first) return
        val report = state.value.home?.daily?.report ?: return
        val emergencyToSave = state.value.emergencyItems.filter {
            it.issueDescription.isNotBlank() || it.responsiblePerson.isNotBlank() || it.notes.isNotBlank()
        }
        val cleaner = state.value.cleanerName
        val mechanical = state.value.mechanicalTechnician
        val electrical = state.value.electricalTechnician
        val manager = state.value.maintenanceManager
        viewModelScope.launch {
            _completion.value = true to null
            when (val result = repository.completeReport(
                report.clientReportId,
                report.answers,
                emergencyToSave,
                cleaner,
                mechanical,
                electrical,
                manager
            )) {
                is AppResult.Success -> {
                    _answerOverrides.value = emptyMap()
                    _emergencyItems.value = null
                    _personnelDraft.value = null
                    _completion.value = false to null
                    scheduler.enqueue()
                    onDone()
                }
                is AppResult.Error -> _completion.value = false to result.message
            }
        }
    }

    fun resetTodayReport(onDone: () -> Unit) = viewModelScope.launch {
        _answerOverrides.value = emptyMap()
        _emergencyItems.value = null
        _personnelDraft.value = null
        repository.resetTodayReport()
        onDone()
    }
}
