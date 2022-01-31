package com.myodov.unicherrygarden.api.types.responseresult;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface ResponsePayload {
}
