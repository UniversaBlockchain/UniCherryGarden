package com.myodov.unicherrygarden.cherrygardener.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;


public class GetCurrenciesList {
    public static final @NonNull ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getCurrenciesListService");

    public static final class Request implements CherryGardenerRequest {
        @NonNull
        public final ActorRef<Response> replyTo;

        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo) {
            this.replyTo = replyTo;
        }

        public String toString() {
            return String.format("Currencies.GetCurrenciesList(%s)", replyTo);
        }
    }

    public static final class Response implements CherryGardenerResponse {
        @NonNull
        public final List<Currency> currencies;

        @JsonCreator
        public Response(@NonNull List<Currency> currencies) {
            this.currencies = currencies;
        }

        public String toString() {
            return String.format("Currencies.GetCurrenciesListResp(%s)", currencies);
        }
    }
}
