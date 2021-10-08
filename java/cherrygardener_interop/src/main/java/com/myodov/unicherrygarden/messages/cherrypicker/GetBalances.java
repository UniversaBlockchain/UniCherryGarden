package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.messages.CherryGardenerRequest;
import com.myodov.unicherrygarden.messages.CherryGardenerResponse;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;


public class GetBalances {
    public static final @NonNull ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getBalancesService");

    public static final class GBRequestPayload implements RequestPayload {
        @NonNull
        public final int confirmations;

        public GBRequestPayload(int confirmations) {
            assert confirmations >= 0 : confirmations;
            this.confirmations = confirmations;
        }

        @Override
        public String toString() {
            return String.format("GetBalances.GBRequestPayload(%s)", confirmations);
        }
    }

    public static final class Request implements CherryGardenerRequest {
        @NonNull
        public final ActorRef<Response> replyTo;

        @NonNull
        public final GBRequestPayload payload;

        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull GBRequestPayload payload) {
            assert replyTo != null;
            assert payload != null;
            this.replyTo = replyTo;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return String.format("GetBalances.Request(%s, %s)", replyTo, payload);
        }
    }

    public static final class Response implements CherryGardenerResponse {
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
