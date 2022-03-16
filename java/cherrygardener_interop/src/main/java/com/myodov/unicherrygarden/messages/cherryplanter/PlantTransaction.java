package com.myodov.unicherrygarden.messages.cherryplanter;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.planted.transactions.SignedOutgoingTransfer;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponsePayload;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import com.myodov.unicherrygarden.messages.CherryGardenResponseWithPayload;
import com.myodov.unicherrygarden.messages.CherryPlanterRequest;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
        public final SignedOutgoingTransfer transfer;

        @Nullable
        public final String comment;

        @JsonCreator
        public PTRequestPayload(@NonNull SignedOutgoingTransfer transfer,
                                @Nullable String comment) {
            assert transfer != null : transfer;

            this.transfer = transfer;
            this.comment = comment;
        }

        @Override
        public final String toString() {
            return String.format("%s(%s, %s)",
                    getClass().getSimpleName(),
                    transfer, comment);
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
         * Whether a new transaction has been just planted. If <code>False</code>,
         * means that the transaction with such contents exists already.
         */
        public final boolean newlyAdded;

        /**
         * The key which can be used to look up specifically this planted transaction;
         * essentially the id in the table of planted transactions.
         */
        public final long plantKey;

        /**
         * Constructor.
         */
        @JsonCreator
        public PlantTransactionRequestResultPayload(boolean newlyAdded,
                                                    long plantKey) {
            assert plantKey > 0 : plantKey;

            this.newlyAdded = newlyAdded;
            this.plantKey = plantKey;
        }

        @Override
        public final String toString() {
            return String.format("%s(%s, %s)",
                    getClass().getSimpleName(),
                    newlyAdded, plantKey);
        }
    }

    public static final class PlantTransactionRequestResultFailure extends SpecificFailurePayload {
        @NonNull
        public final String message;

        /**
         * Constructor.
         */
        @JsonCreator
        public PlantTransactionRequestResultFailure(@NonNull String message) {
            assert message != null : message;
            this.message = message;
        }

        @Override
        public final String toString() {
            return String.format("%s(%s)",
                    getClass().getSimpleName(),
                    message);
        }
    }

    public static final class Response
            extends CherryGardenResponseWithPayload<PlantTransactionRequestResultPayload, PlantTransactionRequestResultFailure> {

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

        public Response(@NonNull PlantTransactionRequestResultFailure specificFailure) {
            this((ResponsePayload) specificFailure);
        }

        @NonNull
        public static Response fromCommonFailure(@NonNull CommonFailurePayload commonFailure) {
            assert commonFailure != null : commonFailure;
            return new Response(commonFailure);
        }
    }
}
