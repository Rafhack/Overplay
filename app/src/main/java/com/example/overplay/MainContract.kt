package com.example.overplay

data class MainViewState(
    val isLoading: Boolean
) {
    companion object {
        val INITIAL_STATE = MainViewState(
            isLoading = true
        )
    }
}

sealed class MainUserAction {
    data object ViewScreen : MainUserAction()
}