package com.myodov.unicherrygarden.messages.cherrygardener;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.myodov.unicherrygarden.api.types.SystemStatus;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponsePayload;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import com.myodov.unicherrygarden.messages.CherryGardenResponseWithPayload;
import com.myodov.unicherrygarden.messages.CherryGardenerRequest;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public class Ping {
    @NonNull
    public static ServiceKey<Request> makeServiceKey(@NonNull String realm) {
        return ServiceKey.create(
                Request.class,
                String.format("%s:pingService", Objects.requireNonNull(realm)));
    }


    // We want to be able to serialize this, no matter the class is empty.
    // Otherwise Jackson fails with a error like
    // "No serializer found for class ... and no properties discovered to create BeanSerializer
    // (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)".
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public static final class PRequestPayload
            implements RequestPayload {

        @JsonCreator
        public PRequestPayload() {
        }

        @Override
        public final String toString() {
            return String.format("%s",
                    getClass().getSimpleName()
            );
        }
    }


    public static final class Request
            extends RequestWithReplyTo<PRequestPayload, Ping.Response>
            implements CherryGardenerRequest {

        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull PRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static final class PingRequestResultPayload extends SuccessPayload {
        @NonNull
        public final SystemStatus systemStatus;

        @NonNull
        public final String realm;

        public final long chainId;

        @NonNull
        public final String version;

        @NonNull
        public final String buildTs;

        @JsonCreator
        public PingRequestResultPayload(@NonNull SystemStatus systemStatus,
                                        @NonNull String realm,
                                        long chainId,
                                        @NonNull String version,
                                        @NonNull String buildTs) {
            assert systemStatus != null;
            assert realm != null;
            assert chainId == -1 || chainId >= 1 : chainId;
            assert version != null;
            assert buildTs != null;

            this.systemStatus = systemStatus;
            this.realm = realm;
            this.chainId = chainId;
            this.version = version;
            this.buildTs = buildTs;
        }

        @Override
        public final String toString() {
            return String.format("%s(%s, %s,%s,  %s, %s)",
                    getClass().getSimpleName(),
                    systemStatus, realm, chainId, version, buildTs);
        }
    }

    public static final class PingRequestResultFailure extends SpecificFailurePayload {
    }

    public static final class Response
            extends CherryGardenResponseWithPayload<PingRequestResultPayload, PingRequestResultFailure> {

        @JsonCreator
        private Response(@NonNull ResponsePayload payload) {
            super(payload);
        }

        public Response(@NonNull PingRequestResultPayload payload) {
            this((ResponsePayload) payload);
        }

        public Response(@NonNull CommonFailurePayload commonFailure) {
            this((ResponsePayload) commonFailure);
        }

        public Response(@NonNull PingRequestResultFailure specificFailure) {
            this((ResponsePayload) specificFailure);
        }

        @NonNull
        public static Response fromCommonFailure(@NonNull CommonFailurePayload commonFailure) {
            assert commonFailure != null : commonFailure;
            return new Response(commonFailure);
        }
    }
}
