package com.myodov.unicherrygarden.messages.cherryplanter;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponsePayload;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import com.myodov.unicherrygarden.messages.CherryGardenResponseWithPayload;
import com.myodov.unicherrygarden.messages.CherryPlanterRequest;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public class PlantTransaction {
    @NonNull
    public static ServiceKey<Request> makeServiceKey(@NonNull String realm) {
        return ServiceKey.create(
                Request.class,
                String.format("%s:plantTransactionService", Objects.requireNonNull(realm)));
    }


    public static final class PTRequestPayload
            implements RequestPayload {

        @NonNull
        public byte[] bytes;

        @JsonCreator
        public PTRequestPayload(@NonNull byte[] bytes) {
            assert bytes != null && bytes.length > 0 : bytes;

            this.bytes = bytes;
        }

        @Override
        public final String toString() {
            return String.format("%s(%s)",
                    getClass().getSimpleName(),
                    Hex.toHexString(bytes));
        }
    }


    public static final class Request
            extends RequestWithReplyTo<PTRequestPayload, Response>
            implements CherryPlanterRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull PTRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static final class PlantTransactionRequestResultPayload extends SuccessPayload {

        /**
         * The key which can be used to look up specifically this planted transaction;
         * essentially the id in the table of planted transactions.
         */
        @NonNull
        public final long plantKey;

        /**
         * Constructor.
         */
        @JsonCreator
        public PlantTransactionRequestResultPayload(long plantKey) {
            assert plantKey > 0 : plantKey;

            this.plantKey = plantKey;
        }

        @Override
        public final String toString() {
            return String.format("%s(%s)",
                    getClass().getSimpleName(),
                    plantKey);
        }
    }

    public static final class AddressDetailsRequestResultFailure extends SpecificFailurePayload {
    }

    public static final class Response
            extends CherryGardenResponseWithPayload<PlantTransactionRequestResultPayload, AddressDetailsRequestResultFailure> {

        @JsonCreator
        private Response(@NonNull ResponsePayload payload) {
            super(payload);
        }

        public Response(@NonNull PlantTransactionRequestResultPayload payload) {
            this((ResponsePayload) payload);
        }

        public Response(@NonNull CommonFailurePayload commonFailure) {
            this((ResponsePayload) commonFailure);
        }

        public Response(@NonNull AddressDetailsRequestResultFailure specificFailure) {
            this((ResponsePayload) specificFailure);
        }

        @NonNull
        public static Response fromCommonFailure(@NonNull CommonFailurePayload commonFailure) {
            assert commonFailure != null : commonFailure;
            return new Response(commonFailure);
        }
    }
}
