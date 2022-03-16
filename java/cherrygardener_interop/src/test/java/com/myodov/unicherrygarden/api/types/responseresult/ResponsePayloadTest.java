package com.myodov.unicherrygarden.api.types.responseresult;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import org.junit.Test;

import java.io.IOException;


public class ResponsePayloadTest extends AbstractJacksonSerializationTest {
    @Test
    public void testJacksonSerialization() throws IOException {
        assertJsonSerialization(
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$UnspecifiedFailure\",\"msg\":\"Some error message\"}",
                new FailurePayload.UnspecifiedFailure("Some error message"),
                FailurePayload.UnspecifiedFailure.class
        );

        assertJsonSerialization(
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$UnspecifiedFailure\",\"msg\":\"Some error message\"}",
                new FailurePayload.UnspecifiedFailure("Some error message"),
                FailurePayload.UnspecifiedFailure.class
        );

        assertJsonSerialization(
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$NoResponseFromCherryGarden\"}",
                FailurePayload.CommonFailurePayload.NO_RESPONSE_FROM_CHERRYGARDEN,
                FailurePayload.CommonFailurePayload.class
        );

        assertJsonSerialization(
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$CancellationCompletionFailure\"}",
                FailurePayload.CommonFailurePayload.CANCELLATION_COMPLETION_FAILURE,
                FailurePayload.CommonFailurePayload.class
        );
    }
}
