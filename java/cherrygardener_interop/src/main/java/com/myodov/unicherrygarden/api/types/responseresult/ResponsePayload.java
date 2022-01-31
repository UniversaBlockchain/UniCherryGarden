package com.myodov.unicherrygarden.api.types.responseresult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.checkerframework.checker.nullness.qual.NonNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface ResponsePayload {
    @JsonIgnore
    ResponseResult.@NonNull Type getType();
}
