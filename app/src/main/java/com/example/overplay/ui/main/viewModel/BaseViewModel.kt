package com.example.overplay.ui.main.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<UserAction, SideEffect : Any, ViewState : Any>(
    initialState: ViewState
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(initialState)
    val stateFlow get() = _stateFlow.asStateFlow()

    private val _sideEffect = MutableSharedFlow<SideEffect>(
        replay = FLOW_REPLAY_COUNT,
        extraBufferCapacity = FLOW_EXTRA_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val sideEffect get() = _sideEffect.asSharedFlow()

    private val userAction = MutableSharedFlow<UserAction>(
        replay = FLOW_REPLAY_COUNT,
        extraBufferCapacity = FLOW_EXTRA_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            userAction.asSharedFlow().collect(::handleUserAction)
        }
    }

    abstract suspend fun handleUserAction(action: UserAction)

    protected suspend fun updateState(reduce: (ViewState.() -> ViewState)) {
        _stateFlow.value.let { _stateFlow.emit(reduce(it)) }
    }

    protected suspend fun emitSideEffect(sideEffect: SideEffect) {
        _sideEffect.emit(sideEffect)
    }

    fun dispatch(action: UserAction) = userAction.tryEmit(action)

    companion object {
        private const val FLOW_EXTRA_BUFFER_CAPACITY = 20
        private const val FLOW_REPLAY_COUNT = 1
    }
}