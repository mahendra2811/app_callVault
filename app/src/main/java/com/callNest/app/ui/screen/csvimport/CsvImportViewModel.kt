package com.callNest.app.ui.screen.csvimport

import android.app.Application
import android.content.ContentProviderOperation
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.util.CsvContactParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class CsvImportUiState(
    val sourceUri: Uri? = null,
    val rows: List<CsvContactParser.Row> = emptyList(),
    val skipped: Int = 0,
    val busy: Boolean = false,
    val resultMessage: String? = null,
)

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    app: Application,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(CsvImportUiState())
    val state: StateFlow<CsvImportUiState> = _state.asStateFlow()

    fun setError(message: String) {
        _state.value = _state.value.copy(resultMessage = message)
    }

    fun onPicked(uri: Uri) {
        _state.value = _state.value.copy(sourceUri = uri, resultMessage = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openInputStream(uri).use { stream ->
                    requireNotNull(stream)
                    CsvContactParser.parse(stream)
                }
            }.onSuccess { parsed ->
                _state.value = _state.value.copy(rows = parsed.rows, skipped = parsed.skipped)
            }.onFailure {
                Timber.w(it, "CSV parse failed")
                _state.value = _state.value.copy(rows = emptyList(), skipped = 0)
            }
        }
    }

    /** Inserts each parsed row as a new raw contact via ContentResolver.applyBatch. */
    fun import(onDoneFmt: String, onFailFmt: String) {
        val rows = _state.value.rows
        if (rows.isEmpty()) return
        _state.value = _state.value.copy(busy = true, resultMessage = null)
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                runCatching {
                    val ops = arrayListOf<ContentProviderOperation>()
                    rows.forEach { row ->
                        val anchor = ops.size
                        ops += ContentProviderOperation
                            .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                            .build()
                        if (!row.name.isNullOrBlank()) {
                            ops += ContentProviderOperation
                                .newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, anchor)
                                .withValue(
                                    ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                                )
                                .withValue(
                                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                                    row.name
                                )
                                .build()
                        }
                        ops += ContentProviderOperation
                            .newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, anchor)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, row.normalized)
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                            )
                            .build()
                    }
                    getApplication<Application>().contentResolver
                        .applyBatch(ContactsContract.AUTHORITY, ops)
                    rows.size
                }.fold(
                    onSuccess = { count -> String.format(onDoneFmt, count) },
                    onFailure = { e ->
                        Timber.w(e, "CSV import failed")
                        String.format(onFailFmt, e.message ?: "unknown")
                    },
                )
            }
            _state.value = _state.value.copy(busy = false, resultMessage = message)
        }
    }
}
