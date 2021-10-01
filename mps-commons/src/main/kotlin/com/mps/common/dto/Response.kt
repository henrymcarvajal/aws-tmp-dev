package com.mps.common.dto

sealed class GenericResponse<T>(val objP: T) {
    data class SuccessResponse<T>(val obj: T) : GenericResponse<T>(obj)
    data class ErrorResponse(val message: String) : GenericResponse<String>(message)
}

sealed class ServiceResponse<T>(val objP: T) {
    data class Success<T>(val obj: T) : ServiceResponse<T>(obj)
    data class Error(val message: String) : ServiceResponse<String>(message)
    //data class Error(val errorData: ErrorData) : ServiceResponse<ErrorData>(errorData)
    //data class ErrorData(val message: String, val cause: String)
}