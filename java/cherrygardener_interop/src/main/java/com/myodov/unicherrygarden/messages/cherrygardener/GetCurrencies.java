package com.myodov.unicherrygarden.messages.cherrygardener;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.messages.CherryGardenerRequest;
import com.myodov.unicherrygarden.messages.CherryGardenerResponse;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;


public class GetCurrencies {
    @NonNull
    public static final ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getCurrenciesService");


    // We want to be able to serialize this, no matter the class is empty.
    // Otherwise Jackson fails with a error like
    // "No serializer found for class ... and no properties discovered to create BeanSerializer
    // (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)".
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    public static final class GCRequestPayload
            implements RequestPayload {
        @JsonCreator
        public GCRequestPayload() {
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


    public static final class Response
            implements CherryGardenerResponse {
        @NonNull
        public final List<Currency> currencies;

        @JsonCreator
        public Response(@NonNull List<Currency> currencies) {
            this.currencies = currencies;
        }

        @Override
        public String toString() {
            return String.format("GetCurrencies.Response(%s)", currencies);
        }
    }
}
