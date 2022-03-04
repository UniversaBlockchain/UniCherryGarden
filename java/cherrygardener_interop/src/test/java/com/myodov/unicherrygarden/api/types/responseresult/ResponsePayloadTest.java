package com.myodov.unicherrygarden.api.types.responseresult;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import org.junit.Test;

import java.io.IOException;


public class ResponsePayloadTest extends AbstractJacksonSerializationTest {
    @Test
    public void testJacksonSerialization() throws IOException {
        assertJsonDeserialization(
                new FailurePayload.UnspecifiedFailure("Some error message"),
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$UnspecifiedFailure\",\"msg\":\"Some error message\"}",
                FailurePayload.UnspecifiedFailure.class
        );

        assertJsonDeserialization(
                new FailurePayload.UnspecifiedFailure("Some error message"),
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$UnspecifiedFailure\",\"msg\":\"Some error message\"}",
                FailurePayload.UnspecifiedFailure.class
        );

        assertJsonDeserialization(
                FailurePayload.CommonFailurePayload.NO_RESPONSE_FROM_CHERRYGARDEN,
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$NoResponseFromCherryGarden\"}",
                FailurePayload.CommonFailurePayload.class
        );

        assertJsonDeserialization(
                FailurePayload.CommonFailurePayload.CANCELLATION_COMPLETION_FAILURE,
                "{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$CancellationCompletionFailure\"}",
                FailurePayload.CommonFailurePayload.class
        );
    }
}
