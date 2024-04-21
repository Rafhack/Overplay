package com.example.overplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _mainStateFlow = MutableStateFlow(MainViewState.INITIAL_STATE)
    val mainStateFlow get() = _mainStateFlow.asStateFlow()

    private val userAction = MutableSharedFlow<MainUserAction>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            respondToUserAction()
        }
    }

    fun dispatch(userAction: MainUserAction) = this.userAction.tryEmit(userAction)

    private suspend fun handleUserAction(action: MainUserAction) = when (action) {
        MainUserAction.ViewScreen -> {
            delay(5000)
            updateState {
                copy(
                    isLoading = false
                )
            }
        }
    }

    private suspend fun respondToUserAction() {
        userAction.asSharedFlow().collect(::handleUserAction)
    }

    private suspend fun updateState(reduce: (MainViewState.() -> MainViewState)) {
        _mainStateFlow.value.let { _mainStateFlow.emit(reduce(it)) }
    }

}