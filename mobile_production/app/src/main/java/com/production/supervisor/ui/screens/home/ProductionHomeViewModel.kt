package com.production.supervisor.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.supervisor.data.local.entity.*
import com.production.supervisor.data.remote.dto.ShiftReportDto
import com.production.supervisor.data.repository.AuthRepository
import com.production.supervisor.data.repository.ProductionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProductionHomeUiState(
    val selectedShift: String = "FIRST", // FIRST, SECOND, THIRD
    val currentDate: String = LocalDate.now().toString(),
    val currentReportId: String = "",
    val factoryName: String = "",
    val factoryCode: String = "",
    val availableFactories: List<com.production.supervisor.data.remote.dto.FactoryDto> = emptyList(),
    val selectedFactoryId: Int? = null,
    val assets: List<AssetEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val operators: List<OperatorEntity> = emptyList(),
    val entries: Map<Int, MachineEntryEntity> = emptyMap(), // assetId -> entry
    val stoppages: List<StoppageEntity> = emptyList(),
    val pendingHandover: ShiftReportDto? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val selectedAssetForEntry: AssetEntity? = null,
    val isStoppageDialogVisible: Boolean = false,
    val isHandoverDialogVisible: Boolean = false
)

@HiltViewModel
class ProductionHomeViewModel @Inject constructor(
    private val repository: ProductionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionHomeUiState())
    val uiState: StateFlow<ProductionHomeUiState> = _uiState.asStateFlow()

    init {
        observeDatabase()
        loadData()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            repository.getAssetsFlow().collect { assetList ->
                _uiState.update { it.copy(assets = assetList) }
            }
        }
        viewModelScope.launch {
            repository.getProductsFlow().collect { prodList ->
                _uiState.update { it.copy(products = prodList) }
            }
        }
        viewModelScope.launch {
            repository.getOperatorsFlow().collect { opList ->
                _uiState.update { it.copy(operators = opList) }
            }
        }
        viewModelScope.launch {
            _uiState.map { it.currentReportId }
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .flatMapLatest { reportId -> repository.getEntriesFlow(reportId) }
                .collect { entryList ->
                    val map = entryList.associateBy { it.assetId }
                    _uiState.update { it.copy(entries = map) }
                }
        }
        viewModelScope.launch {
            _uiState.map { it.currentReportId }
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .flatMapLatest { reportId -> repository.getStoppagesFlow(reportId) }
                .collect { stopList ->
                    _uiState.update { it.copy(stoppages = stopList) }
                }
        }
    }

    fun loadData(factoryId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Fetch bootstrap from backend
            val bootstrapResult = repository.refreshBootstrap(factoryId)
            bootstrapResult.onSuccess { bootstrap ->
                _uiState.update {
                    it.copy(
                        pendingHandover = bootstrap.pendingHandover,
                        factoryName = bootstrap.factory.name,
                        factoryCode = bootstrap.factory.code,
                        selectedFactoryId = bootstrap.factory.id,
                        availableFactories = bootstrap.factories.ifEmpty { listOf(bootstrap.factory) },
                        message = if (factoryId != null) "تم تحميل بيانات ${bootstrap.factory.name}" else null
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(message = ex.message ?: "تعذر الاتصال بالسيرفر، يتم عرض البيانات المحلية")
                }
            }

            // 2. Setup current report
            val date = _uiState.value.currentDate
            val shift = _uiState.value.selectedShift
            val report = repository.getOrCreateShiftReport(date, shift)
            _uiState.update { it.copy(currentReportId = report.clientReportId, isLoading = false) }
        }
    }

    fun selectFactory(factory: com.production.supervisor.data.remote.dto.FactoryDto) {
        loadData(factory.id.toString())
    }

    fun setShift(shift: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedShift = shift) }
            val report = repository.getOrCreateShiftReport(_uiState.value.currentDate, shift)
            _uiState.update { it.copy(currentReportId = report.clientReportId) }
        }
    }

    fun openMachineEntry(asset: AssetEntity) {
        _uiState.update { it.copy(selectedAssetForEntry = asset) }
    }

    fun closeMachineEntry() {
        _uiState.update { it.copy(selectedAssetForEntry = null) }
    }

    fun saveMachineEntry(entry: MachineEntryEntity) {
        viewModelScope.launch {
            repository.saveMachineEntry(entry)
            closeMachineEntry()
            _uiState.update { it.copy(message = "تم تسجيل بيانات ماكينة ${entry.assetCode} بنجاح") }
        }
    }

    fun openStoppageDialog() {
        _uiState.update { it.copy(isStoppageDialogVisible = true) }
    }

    fun closeStoppageDialog() {
        _uiState.update { it.copy(isStoppageDialogVisible = false) }
    }

    fun addStoppage(stoppage: StoppageEntity) {
        viewModelScope.launch {
            repository.addStoppage(stoppage)
            closeStoppageDialog()
            _uiState.update { it.copy(message = "تم تسجيل التوقف بنجاح") }
        }
    }

    fun addFridayPrayerStoppage() {
        viewModelScope.launch {
            repository.addFridayPrayerStoppage(_uiState.value.currentReportId)
            _uiState.update { it.copy(message = "تم تسجيل إيقاف صلاة الجمعة لجميع الماكينات (60 دقيقة)") }
        }
    }

    fun openHandoverDialog() {
        _uiState.update { it.copy(isHandoverDialogVisible = true) }
    }

    fun closeHandoverDialog() {
        _uiState.update { it.copy(isHandoverDialogVisible = false) }
    }

    fun confirmHandover(notes: String) {
        val pending = _uiState.value.pendingHandover ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.confirmHandover(pending.clientReportId, notes)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingHandover = null,
                        isHandoverDialogVisible = false,
                        message = "تم تأكيد استلام الوردية السابقة بنجاح"
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = ex.message ?: "تعذر تأكيد الاستلام"
                    )
                }
            }
        }
    }

    fun finishShift(notes: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.finishShift(_uiState.value.currentReportId, notes)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = "تم إنهاء الوردية وجاهزة لاستلام المشرف التالي"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
