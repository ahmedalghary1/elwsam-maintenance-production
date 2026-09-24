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

enum class WizardSubStep {
    STATUS,     // السؤال الرئيسي: شغالة تمام أم عطلانة
    WEIGHT,     // إدخال الوزن بواسطة كيبورد ATM
    STOPPAGE,   // تسجيل سبب التوقف والمدة
    SUMMARY     // شاشة المراجعة والتسليم النهائي
}

data class ProductionHomeUiState(
    val selectedShift: String = "FIRST", // FIRST, SECOND, THIRD
    val currentDate: String = LocalDate.now().toString(),
    val currentReportId: String = "",
    val factoryName: String = "",
    val factoryCode: String = "",
    val availableFactories: List<com.production.supervisor.data.remote.dto.FactoryDto> = emptyList(),
    val selectedFactoryId: Int? = null,
    val currentUser: com.production.supervisor.data.remote.dto.UserDto? = null,
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
    val isHandoverDialogVisible: Boolean = false,
    // Fawry Wizard Mode State
    val isFawryMode: Boolean = true,
    val wizardStarted: Boolean = false,
    val wizardAssetIndex: Int = 0,
    val wizardSubStep: WizardSubStep = WizardSubStep.STATUS,
    val exceptionAssetForEdit: AssetEntity? = null
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
        viewModelScope.launch {
            val report = repository.getOrCreateShiftReport(_uiState.value.currentDate, _uiState.value.selectedShift)
            _uiState.update { it.copy(currentReportId = report.clientReportId) }
        }
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
            var currentShift = _uiState.value.selectedShift

            bootstrapResult.onSuccess { bootstrap ->
                val assignedShift = bootstrap.user.shift
                if (!assignedShift.isNullOrBlank() && factoryId == null) {
                    currentShift = assignedShift
                }
                _uiState.update {
                    it.copy(
                        currentUser = bootstrap.user,
                        selectedShift = currentShift,
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
            val report = repository.getOrCreateShiftReport(date, currentShift)
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
            val validReportId = if (entry.clientReportId.isNotBlank()) {
                entry.clientReportId
            } else if (_uiState.value.currentReportId.isNotBlank()) {
                _uiState.value.currentReportId
            } else {
                val rep = repository.getOrCreateShiftReport(_uiState.value.currentDate, _uiState.value.selectedShift)
                _uiState.update { it.copy(currentReportId = rep.clientReportId) }
                rep.clientReportId
            }
            val finalEntry = if (entry.clientReportId != validReportId) entry.copy(clientReportId = validReportId) else entry
            repository.saveMachineEntry(finalEntry)
            closeMachineEntry()
            _uiState.update { it.copy(message = "تم تسجيل بيانات ماكينة ${finalEntry.assetCode} بنجاح") }
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

    // ==========================================
    // Fawry Wizard Mode Actions & State Handlers
    // ==========================================

    fun toggleFawryMode(enabled: Boolean) {
        _uiState.update { it.copy(isFawryMode = enabled) }
    }

    fun startWizard(targetIndex: Int = 0) {
        val assets = _uiState.value.assets
        val firstUnrecordedIndex = assets.indexOfFirst { _uiState.value.entries[it.id] == null }
        val startIndex = if (targetIndex != 0) {
            targetIndex.coerceIn(0, (assets.size - 1).coerceAtLeast(0))
        } else if (firstUnrecordedIndex != -1) {
            firstUnrecordedIndex
        } else {
            0
        }
        _uiState.update {
            it.copy(
                wizardStarted = true,
                wizardAssetIndex = startIndex,
                wizardSubStep = WizardSubStep.STATUS
            )
        }
    }

    fun exitWizardToWelcome() {
        _uiState.update {
            it.copy(
                wizardStarted = false,
                wizardSubStep = WizardSubStep.STATUS
            )
        }
    }

    fun setWizardAssetIndex(index: Int) {
        val totalAssets = _uiState.value.assets.size
        if (index in 0 until totalAssets) {
            _uiState.update {
                it.copy(
                    wizardStarted = true,
                    wizardAssetIndex = index,
                    wizardSubStep = WizardSubStep.STATUS
                )
            }
        }
    }

    fun setWizardSubStep(step: WizardSubStep) {
        _uiState.update { it.copy(wizardSubStep = step) }
    }

    fun nextWizardMachine() {
        val totalAssets = _uiState.value.assets.size
        val currentIndex = _uiState.value.wizardAssetIndex
        if (currentIndex + 1 < totalAssets) {
            _uiState.update {
                it.copy(
                    wizardAssetIndex = currentIndex + 1,
                    wizardSubStep = WizardSubStep.STATUS
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    wizardSubStep = WizardSubStep.SUMMARY
                )
            }
        }
    }

    fun previousWizardMachine() {
        val currentIndex = _uiState.value.wizardAssetIndex
        if (_uiState.value.wizardSubStep == WizardSubStep.SUMMARY) {
            _uiState.update {
                it.copy(
                    wizardSubStep = WizardSubStep.STATUS,
                    wizardAssetIndex = (it.assets.size - 1).coerceAtLeast(0)
                )
            }
        } else if (_uiState.value.wizardSubStep != WizardSubStep.STATUS) {
            _uiState.update { it.copy(wizardSubStep = WizardSubStep.STATUS) }
        } else if (currentIndex > 0) {
            _uiState.update {
                it.copy(
                    wizardAssetIndex = currentIndex - 1,
                    wizardSubStep = WizardSubStep.STATUS
                )
            }
        } else {
            _uiState.update { it.copy(wizardStarted = false) }
        }
    }

    fun goToWizardSummary() {
        _uiState.update { it.copy(wizardStarted = true, wizardSubStep = WizardSubStep.SUMMARY) }
    }

    fun openExceptionDialog(asset: AssetEntity) {
        _uiState.update { it.copy(exceptionAssetForEdit = asset) }
    }

    fun closeExceptionDialog() {
        _uiState.update { it.copy(exceptionAssetForEdit = null) }
    }

    fun saveFawryWorkingEntry(
        asset: AssetEntity,
        weightKg: Double,
        customOperatorId: Int? = null,
        customProductId: Int? = null,
        customCavities: Int? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val validReportId = if (_uiState.value.currentReportId.isNotBlank()) {
                _uiState.value.currentReportId
            } else {
                val rep = repository.getOrCreateShiftReport(_uiState.value.currentDate, _uiState.value.selectedShift)
                _uiState.update { it.copy(currentReportId = rep.clientReportId) }
                rep.clientReportId
            }

            val operator = customOperatorId?.let { id -> _uiState.value.operators.find { it.id == id } }
            val product = customProductId?.let { id -> _uiState.value.products.find { it.id == id } }

            val resolvedOperatorId = operator?.id ?: asset.defaultOperatorId
            val resolvedOperatorName = operator?.name ?: asset.defaultOperatorName ?: ""
            val resolvedProductId = product?.id ?: asset.defaultProductId
            val resolvedProductName = product?.name ?: asset.defaultProductName ?: ""
            val resolvedCavities = customCavities ?: asset.originalCavities

            val existing = _uiState.value.entries[asset.id]
            val entry = MachineEntryEntity(
                id = existing?.id ?: 0,
                clientReportId = validReportId,
                assetId = asset.id,
                assetCode = asset.assetCode,
                operatorId = resolvedOperatorId,
                operatorName = resolvedOperatorName,
                originalOperatorId = asset.defaultOperatorId,
                originalOperatorName = asset.defaultOperatorName ?: "",
                operatorChanged = (resolvedOperatorId != asset.defaultOperatorId),
                productId = resolvedProductId,
                productName = resolvedProductName,
                originalProductId = asset.defaultProductId,
                originalProductName = asset.defaultProductName ?: "",
                productChanged = (resolvedProductId != asset.defaultProductId),
                originalCavities = asset.originalCavities,
                currentCavities = resolvedCavities,
                operationMode = "AUTO",
                coolingTimeSeconds = asset.coolingTimeSeconds,
                cycleTimeSeconds = asset.cycleTimeSeconds,
                rawMaterial = "",
                finalProductionWeightKg = weightKg,
                targetCycleProduction = asset.targetCycleProduction,
                packagingType = "براميل",
                notes = notes
            )

            repository.saveMachineEntry(entry)
            _uiState.update { it.copy(message = "تم تسجيل ماكينة ${asset.assetCode}: $weightKg كجم") }
            nextWizardMachine()
        }
    }

    fun saveFawryStoppage(
        asset: AssetEntity,
        stoppageType: String,
        stoppageTypeDisplay: String,
        durationMinutes: Int,
        notes: String = "",
        partialWeightKg: Double = 0.0
    ) {
        viewModelScope.launch {
            val validReportId = if (_uiState.value.currentReportId.isNotBlank()) {
                _uiState.value.currentReportId
            } else {
                val rep = repository.getOrCreateShiftReport(_uiState.value.currentDate, _uiState.value.selectedShift)
                _uiState.update { it.copy(currentReportId = rep.clientReportId) }
                rep.clientReportId
            }

            // 1. Add stoppage
            val stoppage = StoppageEntity(
                clientReportId = validReportId,
                assetId = asset.id,
                assetCode = asset.assetCode,
                stoppageType = stoppageType,
                stoppageTypeDisplay = stoppageTypeDisplay,
                description = notes.ifBlank { "عطل ماكينة ${asset.assetCode} ($stoppageTypeDisplay)" },
                durationMinutes = durationMinutes,
                actionTaken = "تم تسجيل توقف: $stoppageTypeDisplay"
            )
            repository.addStoppage(stoppage)

            // 2. Add entry (with 0.0 or partial production weight if any)
            val existing = _uiState.value.entries[asset.id]
            val entry = MachineEntryEntity(
                id = existing?.id ?: 0,
                clientReportId = validReportId,
                assetId = asset.id,
                assetCode = asset.assetCode,
                operatorId = asset.defaultOperatorId,
                operatorName = asset.defaultOperatorName ?: "",
                originalOperatorId = asset.defaultOperatorId,
                originalOperatorName = asset.defaultOperatorName ?: "",
                productId = asset.defaultProductId,
                productName = asset.defaultProductName ?: "",
                originalProductId = asset.defaultProductId,
                originalProductName = asset.defaultProductName ?: "",
                originalCavities = asset.originalCavities,
                currentCavities = asset.originalCavities,
                operationMode = "AUTO",
                coolingTimeSeconds = asset.coolingTimeSeconds,
                cycleTimeSeconds = asset.cycleTimeSeconds,
                rawMaterial = "",
                finalProductionWeightKg = partialWeightKg,
                targetCycleProduction = asset.targetCycleProduction,
                notes = "ماكينة متوقفة: $stoppageTypeDisplay (${durationMinutes} دقيقة)"
            )
            repository.saveMachineEntry(entry)

            _uiState.update { it.copy(message = "تم تسجيل توقف ماكينة ${asset.assetCode}: $stoppageTypeDisplay") }
            nextWizardMachine()
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
