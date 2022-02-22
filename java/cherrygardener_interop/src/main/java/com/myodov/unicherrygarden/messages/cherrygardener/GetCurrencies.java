package com.myodov.unicherrygarden.messages.cherrygardener;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.myodov.unicherrygarden.api.types.SystemStatus;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.ResponsePayload;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import com.myodov.unicherrygarden.messages.CherryGardenResponseWithPayload;
import com.myodov.unicherrygarden.messages.CherryGardenerRequest;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;


public class GetCurrencies {
    @NonNull
    public static ServiceKey<Request> makeServiceKey(@NonNull String realm) {
        return ServiceKey.create(
                Request.class,
                String.format("%s:getCurrenciesService", Objects.requireNonNull(realm)));
    }


    // We want to be able to serialize this, no matter the class is empty.
    // Otherwise Jackson fails with a error like
    // "No serializer found for class ... and no properties discovered to create BeanSerializer
    // (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)".
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public static final class GCRequestPayload
            implements RequestPayload {

        /**
         * What currency keys (only) do we want to select?
         */
        @Nullable
        public final Set<String> filterCurrencyKeys;

        /**
         * Do we want the verified currencies?
         */
        public final boolean getVerified;

        /**
         * Do we want the unverified currencies?
         */
        public final boolean getUnverified;

        @JsonCreator
        public GCRequestPayload(@Nullable Set<String> filterCurrencyKeys,
                                boolean getVerified,
                                boolean getUnverified) {
            // At least one of the two should be defined
            assert getVerified || getUnverified : String.format("%s/%s/%s", filterCurrencyKeys, getVerified, getUnverified);
            this.filterCurrencyKeys = filterCurrencyKeys;
            this.getVerified = getVerified;
            this.getUnverified = getUnverified;
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s, %s)",
                    getClass().getSimpleName(),
                    filterCurrencyKeys, getVerified, getUnverified);
        }
    }


    public static final class Request
            extends RequestWithReplyTo<GCRequestPayload, Response>
            implements CherryGardenerRequest {

        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull GCRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static class CurrenciesRequestResultPayload extends SuccessPayload {
        /**
         * The total status of blockchain synchronization/blockchain/Ethereum node/UniCherryGarden node.
         */
        @NonNull
        public final SystemStatus systemStatus;

        @NonNull
        public final List<Currency> currencies;

        @JsonCreator
        public CurrenciesRequestResultPayload(@NonNull SystemStatus systemStatus,
                                              @NonNull List<Currency> currencies) {
            assert systemStatus != null;
            assert currencies != null;

            this.systemStatus = systemStatus;
            this.currencies = currencies;
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s)",
                    getClass().getSimpleName(),
                    systemStatus, currencies);
        }
    }

    public static class CurrenciesRequestResultFailure extends SpecificFailurePayload {
    }


    public static final class Response
            extends CherryGardenResponseWithPayload<CurrenciesRequestResultPayload, CurrenciesRequestResultFailure> {

        @JsonCreator
        private Response(@NonNull ResponsePayload payload) {
            super(payload);
        }

        public Response(@NonNull CurrenciesRequestResultPayload payload) {
            this((ResponsePayload) payload);
        }

        public Response(@NonNull CommonFailurePayload commonFailure) {
            this((ResponsePayload) commonFailure);
        }

        public Response(@NonNull CurrenciesRequestResultFailure specificFailure) {
            this((ResponsePayload) specificFailure);
        }

        @NonNull
        public static Response fromCommonFailure(@NonNull CommonFailurePayload commonFailure) {
            assert commonFailure != null : commonFailure;
            return new Response(commonFailure);
        }
    }
}
