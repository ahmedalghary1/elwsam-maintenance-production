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
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class WizardSubStep {
    STATUS,
    WEIGHT,
    STOPPAGE,
    SUMMARY
}

data class ProductionHomeUiState(
    val selectedShift: String = "FIRST", // FIRST, SECOND, THIRD
    val currentDate: String = LocalDate.now(ZoneId.of("Africa/Cairo")).toString(),
    val currentReportId: String = "",
    val factoryName: String = "",
    val factoryCode: String = "",
    val availableFactories: List<com.production.supervisor.data.remote.dto.FactoryDto> = emptyList(),
    val selectedFactoryId: Int? = null,
    val currentUser: com.production.supervisor.data.remote.dto.UserDto? = null,
    val cachedSupervisorName: String = "",
    val assets: List<AssetEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val operators: List<OperatorEntity> = emptyList(),
    val productionOptions: List<ProductionOptionEntity> = emptyList(),
    val entries: Map<Int, MachineEntryEntity> = emptyMap(), // assetId -> entry
    val stoppages: List<StoppageEntity> = emptyList(),
    val pendingHandover: ShiftReportDto? = null,
    val handoverChecked: Boolean = false,
    val isLoading: Boolean = false,
    val isSavingEntry: Boolean = false,
    val isFinishingShift: Boolean = false,
    val isReportFinished: Boolean = false,
    val isReportSynced: Boolean = false,
    val reportStatus: String = "DRAFT",
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
        loadData()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            authRepository.userNameFlow.collect { name ->
                _uiState.update { it.copy(cachedSupervisorName = name.orEmpty()) }
            }
        }
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
            repository.getProductionOptionsFlow().collect { options ->
                _uiState.update { it.copy(productionOptions = options) }
            }
        }
        viewModelScope.launch {
            _uiState.map { it.currentReportId }
                .distinctUntilChanged()
                .flatMapLatest { reportId ->
                    if (reportId.isBlank()) flowOf(emptyList()) else repository.getEntriesFlow(reportId)
                }
                .collect { entryList ->
                    val map = entryList.associateBy { it.assetId }
                    _uiState.update { it.copy(entries = map) }
                }
        }
        viewModelScope.launch {
            _uiState.map { it.currentReportId }
                .distinctUntilChanged()
                .flatMapLatest { reportId ->
                    if (reportId.isBlank()) flowOf(emptyList()) else repository.getStoppagesFlow(reportId)
                }
                .collect { stopList ->
                    _uiState.update { it.copy(stoppages = stopList) }
                }
        }
        viewModelScope.launch {
            _uiState.map { it.currentReportId }.distinctUntilChanged()
                .flatMapLatest { reportId ->
                    if (reportId.isBlank()) flowOf(null) else repository.getShiftReportFlow(reportId)
                }
                .collect { report ->
                    _uiState.update {
                        it.copy(
                            reportStatus = report?.status ?: "DRAFT",
                            isReportFinished = report?.completedAtDevice != null || report?.status == "CONFIRMED",
                            isReportSynced = report?.isSynced == true
                        )
                    }
                }
        }
    }

    fun loadData(factoryId: String? = null) {
        if (_uiState.value.isLoading || _uiState.value.isSavingEntry || _uiState.value.isFinishingShift) return
        _uiState.update { it.copy(isLoading = true, handoverChecked = false) }
        viewModelScope.launch {
            try {
                val bootstrapResult = repository.refreshBootstrap(factoryId)
                var currentShift = _uiState.value.selectedShift
                bootstrapResult.onSuccess { bootstrap ->
                    currentShift = bootstrap.user.shift?.takeIf { it.isNotBlank() } ?: currentShift
                    _uiState.update {
                        it.copy(
                            currentUser = bootstrap.user,
                            selectedShift = currentShift,
                            currentDate = bootstrap.serverDate,
                            pendingHandover = bootstrap.pendingHandover,
                            handoverChecked = true,
                            isHandoverDialogVisible = bootstrap.pendingHandover != null,
                            factoryName = bootstrap.factory.name,
                            factoryCode = bootstrap.factory.code,
                            selectedFactoryId = bootstrap.factory.id,
                            availableFactories = bootstrap.factories.ifEmpty { listOf(bootstrap.factory) },
                            message = if (factoryId != null) "تم تحميل بيانات ${bootstrap.factory.name}" else null
                        )
                    }
                }.onFailure {
                    currentShift = repository.getCachedShift() ?: currentShift
                    val cachedFactoryName = repository.getCachedFactoryName().orEmpty()
                    val cachedFactoryId = repository.getCachedFactoryId()
                    _uiState.update {
                        it.copy(
                            selectedShift = currentShift,
                            factoryName = cachedFactoryName,
                            selectedFactoryId = cachedFactoryId,
                            message = "اتصل بالإنترنت للتحقق من التقرير السابق قبل كتابة تقريرك. بياناتك المحفوظة موجودة."
                        )
                    }
                }
                if (!_uiState.value.handoverChecked || _uiState.value.pendingHandover != null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                val report = repository.getOrCreateShiftReport(_uiState.value.currentDate, currentShift)
                _uiState.update {
                    it.copy(
                        currentReportId = report.clientReportId,
                        wizardStarted = if (factoryId != null) false else it.wizardStarted,
                        wizardAssetIndex = if (factoryId != null) 0 else it.wizardAssetIndex,
                        wizardSubStep = if (factoryId != null) WizardSubStep.STATUS else it.wizardSubStep,
                        isLoading = false
                    )
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, message = ex.message ?: "تعذر فتح التقرير. أعد المحاولة.")
                }
            }
        }
    }

    fun selectFactory(factory: com.production.supervisor.data.remote.dto.FactoryDto) {
        loadData(factory.id.toString())
    }

    fun checkIncomingHandover() {
        if (!_uiState.value.handoverChecked || _uiState.value.isLoading) return
        viewModelScope.launch {
            repository.checkIncomingHandover().onSuccess { data ->
                _uiState.update { it.copy(pendingHandover = data.pendingHandover, currentUser = data.user) }
            }
        }
    }

    fun setShift(shift: String) {
        if (_uiState.value.isLoading || _uiState.value.isSavingEntry || _uiState.value.isFinishingShift) return
        viewModelScope.launch {
            try {
                val report = repository.getOrCreateShiftReport(_uiState.value.currentDate, shift)
                _uiState.update { it.copy(selectedShift = shift, currentReportId = report.clientReportId, wizardStarted = false) }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                _uiState.update { it.copy(message = ex.message ?: "تعذر فتح الوردية") }
            }
        }
    }

    fun openMachineEntry(asset: AssetEntity) {
        if (!canEditReport()) return
        _uiState.update { it.copy(selectedAssetForEntry = asset) }
    }

    fun closeMachineEntry() {
        _uiState.update { it.copy(selectedAssetForEntry = null) }
    }

    fun saveMachineEntry(entry: MachineEntryEntity) {
        if (!validMachineValues(entry.finalProductionQuantity, entry.currentCavities, entry.originalCavities,
                entry.coolingTimeSeconds, entry.cycleTimeSeconds)) return
        saveEntryLocally {
            val finalEntry = entry.copy(clientReportId = _uiState.value.currentReportId)
            repository.saveMachineEntry(finalEntry)
            _uiState.update { it.copy(entries = it.entries + (finalEntry.assetId to finalEntry)) }
            closeMachineEntry()
            _uiState.update { it.copy(message = "تم حفظ بيانات الماكينة ${finalEntry.assetCode}") }
        }
    }

    fun openStoppageDialog() {
        if (!canEditReport()) return
        _uiState.update { it.copy(isStoppageDialogVisible = true) }
    }

    fun closeStoppageDialog() {
        _uiState.update { it.copy(isStoppageDialogVisible = false) }
    }

    fun addGeneralFaults(description: String, onSaved: () -> Unit = {}) {
        if (description.isBlank()) return
        saveEntryLocally {
            repository.addStoppage(StoppageEntity(clientReportId = _uiState.value.currentReportId,
                stoppageType = "OTHER", stoppageTypeDisplay = "أعطال عامة", description = description.trim()))
            _uiState.update { it.copy(message = "تم حفظ الأعطال") }
            onSaved()
        }
    }

    fun addStoppage(stoppage: StoppageEntity) {
        if (stoppage.durationMinutes <= 0 || stoppage.description.isBlank()) {
            _uiState.update { it.copy(message = "أدخل سبب التوقف ومدة أكبر من صفر") }
            return
        }
        saveEntryLocally {
            repository.addStoppage(stoppage.copy(clientReportId = _uiState.value.currentReportId))
            closeStoppageDialog()
            _uiState.update { it.copy(message = "تم تسجيل التوقف بنجاح") }
        }
    }

    fun addFridayPrayerStoppage() {
        saveEntryLocally {
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

    fun confirmHandover(notes: String, decision: String = "ACCEPT") {
        if (decision == "REJECT" && notes.isBlank()) return
        if (_uiState.value.isLoading || _uiState.value.isSavingEntry || _uiState.value.isFinishingShift) return
        val pending = _uiState.value.pendingHandover ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.confirmHandover(pending.clientReportId, notes, decision)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        handoverChecked = false,
                        pendingHandover = null,
                        isHandoverDialogVisible = false,
                        message = "تم تسجيل قرار مراجعة التقرير"
                    )
                }
                loadData()
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

    fun finishShift(notes: String, drafts: Map<Int, MachineEntryEntity?> = emptyMap()) {
        if (!canEditReport()) return
        if (_uiState.value.currentUser?.nextShiftSupervisor == null) {
            _uiState.update { it.copy(message = "حدد المشرف المستلم من الداشبورد ثم حدّث بيانات التطبيق قبل التسليم") }
            return
        }
        val state = _uiState.value
        if (state.isFinishingShift || state.isSavingEntry || state.isLoading || state.isReportFinished) return
        val allEntries = resolveReportEntries(state.entries, drafts)
        val missing = state.assets.filter { asset ->
            !isMachineRecorded(allEntries[asset.id], state.stoppages)
        }
        if (state.currentReportId.isBlank() || state.assets.isEmpty() || missing.isNotEmpty()) {
            _uiState.update { it.copy(message = "أكمل بيانات جميع الماكينات قبل تسليم التقرير") }
            return
        }
        val entries = state.assets.map { allEntries.getValue(it.id)!! }
        if (entries.any { it.clientReportId != state.currentReportId ||
                !validMachineValues(it.finalProductionQuantity, it.currentCavities, it.originalCavities, it.coolingTimeSeconds, it.cycleTimeSeconds) }) return
        _uiState.update { it.copy(isFinishingShift = true) }
        viewModelScope.launch {
            try {
                repository.finishShift(state.currentReportId, notes, entries)
                    .onSuccess {
                        _uiState.update { it.copy(isReportFinished = true, message = "تم حفظ التقرير. يظهر تأكيد الإرسال عند وصوله إلى الموقع.") }
                    }.onFailure { ex ->
                        _uiState.update { it.copy(message = ex.message ?: "تعذر حفظ التقرير. أعد المحاولة.") }
                    }
            } finally {
                _uiState.update { it.copy(isFinishingShift = false) }
            }
        }
    }

    fun retrySync() {
        val reportId = _uiState.value.currentReportId.takeIf { it.isNotBlank() } ?: return
        repository.requestSync(reportId)
        _uiState.update { it.copy(message = "سيتم إرسال التقرير عند توفر الاتصال") }
    }

    private fun canEditReport(): Boolean {
        val state = _uiState.value
        if (!state.handoverChecked || state.pendingHandover != null) {
            _uiState.update { it.copy(message = "راجع تقرير المشرف السابق أولاً", isHandoverDialogVisible = it.pendingHandover != null) }
            return false
        }
        if (state.isReportFinished) {
            _uiState.update { it.copy(message = "تم تسليم هذا التقرير؛ يمكنك مراجعة البيانات") }
            return false
        }
        return !state.isLoading && !state.isSavingEntry && !state.isFinishingShift && state.currentReportId.isNotBlank()
    }

    private fun validMachineValues(quantity: Double, currentCavities: Int, originalCavities: Int,
                                   cooling: Double, cycle: Double): Boolean {
        val valid = quantity.isFinite() && quantity >= 0 && currentCavities >= 1 && originalCavities >= 1 &&
            cooling.isFinite() && cooling >= 0 && cycle.isFinite() && cycle >= 0
        if (!valid) _uiState.update { it.copy(message = "راجع الكمية واللقم وأزمنة الدورة والتبريد") }
        return valid
    }

    private fun saveEntryLocally(action: suspend () -> Unit) {
        if (!canEditReport()) return
        _uiState.update { it.copy(isSavingEntry = true) }
        viewModelScope.launch {
            try {
                action()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                _uiState.update { it.copy(message = ex.message ?: "تعذر الحفظ. أعد المحاولة.") }
            } finally {
                _uiState.update { it.copy(isSavingEntry = false) }
            }
        }
    }

    // ==========================================
    // Fawry Wizard Mode Actions & State Handlers
    // ==========================================

    fun toggleFawryMode(enabled: Boolean) {
        _uiState.update { it.copy(isFawryMode = enabled) }
    }

    fun startWizard(targetIndex: Int? = null) {
        val assets = _uiState.value.assets
        if (assets.isEmpty() || _uiState.value.isLoading || _uiState.value.isSavingEntry) return
        val state = _uiState.value
        val firstUnrecordedIndex = assets.indexOfFirst { asset ->
            !isMachineRecorded(state.entries[asset.id], state.stoppages)
        }
        val startIndex = if (targetIndex != null) {
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
                wizardSubStep = if (it.isReportFinished) WizardSubStep.SUMMARY else WizardSubStep.STATUS
            )
        }
    }

    fun exitWizardToWelcome() {
        if (_uiState.value.isSavingEntry || _uiState.value.isFinishingShift) return
        _uiState.update {
            it.copy(
                wizardStarted = false,
                wizardSubStep = WizardSubStep.STATUS
            )
        }
    }

    fun setWizardAssetIndex(index: Int) {
        if (!canEditReport()) return
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
        if (_uiState.value.isSavingEntry || _uiState.value.isReportFinished) return
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
        if (!canEditReport()) return
        _uiState.update { it.copy(exceptionAssetForEdit = asset) }
    }

    fun closeExceptionDialog() {
        _uiState.update { it.copy(exceptionAssetForEdit = null) }
    }

    fun saveFawryWorkingEntry(
        asset: AssetEntity,
        quantity: Double,
        finalUnit: ProductionOptionEntity?,
        currentCavities: Int,
        rawMaterial: ProductionOptionEntity?,
        packaging: ProductionOptionEntity?,
        coolingTimeSeconds: Double,
        cycleTimeSeconds: Double,
        operationMode: String,
        customOperatorId: Int? = null,
        customProductId: Int? = null,
        clearOperator: Boolean = false,
        clearProduct: Boolean = false,
        notes: String = "",
        originalCavities: Int = asset.originalCavities,
        advance: Boolean = true
    ) {
        if (!validMachineValues(quantity, currentCavities, originalCavities, coolingTimeSeconds, cycleTimeSeconds)) return
        if (quantity == 0.0 && advance) {
            _uiState.update { it.copy(message = "أدخل كمية الإنتاج، أو اختر تسجيل توقف") }
            return
        }
        saveEntryLocally {
            val validReportId = _uiState.value.currentReportId

            val existing = _uiState.value.entries[asset.id]
            val operator = customOperatorId?.let { id -> _uiState.value.operators.find { it.id == id } }
            val product = customProductId?.let { id -> _uiState.value.products.find { it.id == id } }

            val resolvedOperatorId = if (clearOperator) null else customOperatorId ?: existing?.operatorId ?: asset.defaultOperatorId
            val resolvedOperatorName = if (clearOperator) "" else operator?.name
                ?: existing?.takeIf { it.operatorId == resolvedOperatorId }?.operatorName ?: asset.defaultOperatorName.orEmpty()
            val resolvedProductId = if (clearProduct) null else customProductId ?: existing?.productId ?: asset.defaultProductId
            val resolvedProductName = if (clearProduct) "" else product?.name
                ?: existing?.takeIf { it.productId == resolvedProductId }?.productName ?: asset.defaultProductName.orEmpty()
            val originalOperatorId = if (existing != null) existing.originalOperatorId else asset.defaultOperatorId
            val originalProductId = if (existing != null) existing.originalProductId else asset.defaultProductId
            val selectedUnit = finalUnit ?: _uiState.value.productionOptions.find { it.id == asset.defaultFinalUnitId }
                ?: _uiState.value.productionOptions.firstOrNull { it.category == "FINAL_UNIT" && it.name == "كجم" }
            val selectedMaterial = rawMaterial ?: _uiState.value.productionOptions.find { it.id == asset.defaultRawMaterialId }
            val selectedPackaging = packaging ?: _uiState.value.productionOptions.find { it.id == asset.defaultPackagingId }
            val unitName = selectedUnit?.name ?: asset.defaultFinalUnitName ?: "كجم"
            val kgFactor = selectedUnit?.kgPerUnit
            val weightKg = if (kgFactor != null) quantity * kgFactor
                else if (unitName in setOf("كجم", "كيلو", "كيلوجرام")) quantity else 0.0

            val entry = MachineEntryEntity(
                id = existing?.id ?: 0,
                clientReportId = validReportId,
                assetId = asset.id,
                assetCode = asset.assetCode,
                operatorId = resolvedOperatorId,
                operatorName = resolvedOperatorName,
                originalOperatorId = originalOperatorId,
                originalOperatorName = existing?.originalOperatorName ?: asset.defaultOperatorName.orEmpty(),
                operatorChanged = (resolvedOperatorId != originalOperatorId),
                productId = resolvedProductId,
                productName = resolvedProductName,
                originalProductId = originalProductId,
                originalProductName = existing?.originalProductName ?: asset.defaultProductName.orEmpty(),
                productChanged = (resolvedProductId != originalProductId),
                originalCavities = originalCavities,
                currentCavities = currentCavities,
                operationMode = operationMode,
                coolingTimeSeconds = coolingTimeSeconds,
                cycleTimeSeconds = cycleTimeSeconds,
                originalCoolingTimeSeconds = existing?.originalCoolingTimeSeconds ?: asset.coolingTimeSeconds,
                originalCycleTimeSeconds = existing?.originalCycleTimeSeconds ?: asset.cycleTimeSeconds,
                rawMaterial = selectedMaterial?.name ?: "",
                rawMaterialOptionId = selectedMaterial?.id,
                finalProductionWeightKg = weightKg,
                finalProductionQuantity = quantity,
                finalProductionUnitId = selectedUnit?.id,
                finalProductionUnitName = unitName,
                targetCycleProduction = asset.targetCycleProduction,
                targetCycleUnitName = asset.targetCycleUnitName ?: "",
                packagingType = selectedPackaging?.name ?: "",
                packagingOptionId = selectedPackaging?.id,
                notes = notes
            )

            repository.saveMachineEntry(entry)
            _uiState.update { it.copy(entries = it.entries + (asset.id to entry)) }
            if (advance) nextWizardMachine()
        }
    }

    fun saveFawryStoppage(
        asset: AssetEntity,
        stoppageType: String,
        stoppageTypeDisplay: String,
        durationMinutes: Int,
        notes: String = "",
        partialWeightKg: Double = 0.0,
        currentCavities: Int? = null,
        originalCavities: Int? = null,
        coolingTimeSeconds: Double? = null,
        cycleTimeSeconds: Double? = null
    ) {
        val previousEntry = _uiState.value.entries[asset.id]
        val current = currentCavities ?: previousEntry?.currentCavities ?: asset.originalCavities
        val original = originalCavities ?: previousEntry?.originalCavities ?: asset.originalCavities
        val cooling = coolingTimeSeconds ?: previousEntry?.coolingTimeSeconds ?: asset.coolingTimeSeconds
        val cycle = cycleTimeSeconds ?: previousEntry?.cycleTimeSeconds ?: asset.cycleTimeSeconds
        if (!validMachineValues(partialWeightKg, current, original, cooling, cycle)) return
        if (durationMinutes <= 0) {
            _uiState.update { it.copy(message = "أدخل مدة توقف أكبر من صفر") }
            return
        }
        saveEntryLocally {
            val validReportId = _uiState.value.currentReportId

            // 1. Add stoppage
            val stoppage = StoppageEntity(
                id = _uiState.value.stoppages.firstOrNull { it.assetId == asset.id }?.id ?: 0,
                clientReportId = validReportId,
                assetId = asset.id,
                assetCode = asset.assetCode,
                stoppageType = stoppageType,
                stoppageTypeDisplay = stoppageTypeDisplay,
                description = notes.ifBlank { "عطل ماكينة ${asset.assetCode} ($stoppageTypeDisplay)" },
                durationMinutes = durationMinutes,
                actionTaken = "تم تسجيل توقف: $stoppageTypeDisplay"
            )

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
                originalCavities = original,
                currentCavities = current,
                operationMode = "AUTO",
                coolingTimeSeconds = cooling,
                cycleTimeSeconds = cycle,
                originalCoolingTimeSeconds = asset.coolingTimeSeconds,
                originalCycleTimeSeconds = asset.cycleTimeSeconds,
                rawMaterial = _uiState.value.productionOptions.find { it.id == asset.defaultRawMaterialId }?.name.orEmpty(),
                rawMaterialOptionId = asset.defaultRawMaterialId,
                finalProductionWeightKg = partialWeightKg,
                finalProductionQuantity = partialWeightKg,
                finalProductionUnitId = _uiState.value.productionOptions.find { it.id == asset.defaultFinalUnitId && it.name == "كجم" }?.id
                    ?: _uiState.value.productionOptions.firstOrNull { it.category == "FINAL_UNIT" && it.name == "كجم" }?.id,
                finalProductionUnitName = "كجم",
                targetCycleProduction = asset.targetCycleProduction,
                targetCycleUnitName = asset.targetCycleUnitName.orEmpty(),
                packagingType = _uiState.value.productionOptions.find { it.id == asset.defaultPackagingId }?.name.orEmpty(),
                packagingOptionId = asset.defaultPackagingId,
                notes = "ماكينة متوقفة: $stoppageTypeDisplay (${durationMinutes} دقيقة)"
            )
            val finalEntry = existing?.copy(
                currentCavities = current,
                originalCavities = original,
                coolingTimeSeconds = cooling,
                cycleTimeSeconds = cycle,
                finalProductionWeightKg = partialWeightKg,
                finalProductionQuantity = partialWeightKg,
                finalProductionUnitId = entry.finalProductionUnitId,
                finalProductionUnitName = "كجم",
                notes = notes.ifBlank { entry.notes }
            ) ?: entry.copy(notes = notes.ifBlank { entry.notes })
            repository.saveStoppedMachine(finalEntry, stoppage)
            _uiState.update {
                it.copy(entries = it.entries + (asset.id to finalEntry),
                    stoppages = it.stoppages.filterNot { old -> old.id == stoppage.id && stoppage.id != 0L } + stoppage)
            }
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
