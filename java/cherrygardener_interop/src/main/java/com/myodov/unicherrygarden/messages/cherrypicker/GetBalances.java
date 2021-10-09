package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.CherryPickerResponse;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;


public class GetBalances {
    @NonNull
    public static final ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getBalancesService");


    public static final class GBRequestPayload
            implements RequestPayload {
        @NonNull
        public final int confirmations;

        @JsonCreator
        public GBRequestPayload(int confirmations) {
            assert confirmations >= 0 : confirmations;
            this.confirmations = confirmations;
        }

        @Override
        public String toString() {
            return String.format("GetBalances.GBRequestPayload(%s)", confirmations);
        }
    }


    public static final class Request
            extends RequestWithReplyTo<GBRequestPayload, Response>
            implements CherryPickerRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull GBRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static final class Response
            implements CherryPickerResponse {
        @NonNull
        public final List<Currency> currencies;

        @JsonCreator
        public Response(@NonNull List<Currency> currencies) {
            this.currencies = currencies;
        }

        @Override
        public String toString() {
            return String.format("GetBalances.Response(%s)", currencies);
        }
    }
}
