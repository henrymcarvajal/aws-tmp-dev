package com.mps.payment.core.repository.criteria

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.util.stream.Stream

enum class SearchOperation(@get:JsonValue val operationName: String) {
    EXISTENT("EXISTENT"),
    NON_EXISTENT("NON_EXISTENT"),
    GREATER_THAN("GREATER_THAN"),
    LESS_THAN("LESS_THAN"),
    GREATER_THAN_EQUAL("GREATER_THAN_EQUAL"),
    LESS_THAN_EQUAL("LESS_THAN_EQUAL"),
    NOT_EQUAL("NOT_EQUAL"),
    EQUAL("EQUAL"),
    MATCH("MATCH"),
    MATCH_END("MATCH_END");

    companion object {
        @JsonCreator
        fun decode(operationName: String): SearchOperation {
            return Stream.of(*values()).filter { targetEnum: SearchOperation -> targetEnum.operationName == operationName }
                .findFirst().orElse(null)
        }
    }
}