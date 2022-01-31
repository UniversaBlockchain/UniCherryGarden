package com.myodov.unicherrygarden.api.types.responseresult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ResponsePayloadTest {
    private static String makeJson(Object value) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(value);
    }

    @Test
    public void testJacksonSerialization() throws IOException {
        assertEquals(
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$UnspecifiedFailure\",\"msg\":\"Some error message\"}",
                makeJson(new FailurePayload.UnspecifiedFailure("Some error message"))
        );

        assertEquals(
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$NotAvailableInOfflineMode\"}",
                makeJson(FailurePayload.CommonFailurePayload.NOT_AVAILABLE_IN_OFFLINE_MODE)
        );

        assertEquals(
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$NoResponseFromCherryGarden\"}",
                makeJson(FailurePayload.CommonFailurePayload.NO_RESPONSE_FROM_CHERRYGARDEN)
        );

        assertEquals(
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$CancellationCompletionFailure\"}",
                makeJson(FailurePayload.CommonFailurePayload.CANCELLATION_COMPLETION_FAILURE)
        );
    }
}
