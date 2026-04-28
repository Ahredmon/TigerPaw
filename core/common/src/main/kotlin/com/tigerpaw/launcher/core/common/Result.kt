package com.tigerpaw.launcher.core.common

import kotlinx.coroutines.flow.Flow

/** Generic wrapper for async results across the app. */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> = kotlinx.coroutines.flow.flow {
    emit(Result.Loading)
    try {
        collect { value -> emit(Result.Success(value)) }
    } catch (e: Exception) {
        emit(Result.Error(e))
    }
}
