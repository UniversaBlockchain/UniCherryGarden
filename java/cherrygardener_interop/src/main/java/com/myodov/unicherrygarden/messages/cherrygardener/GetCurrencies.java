package com.myodov.unicherrygarden.messages.cherrygardener;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.messages.CherryGardenerRequest;
import com.myodov.unicherrygarden.messages.CherryGardenerResponse;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;


public class GetCurrencies {
    public static final @NonNull ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getCurrenciesService");

    public static final class Request implements CherryGardenerRequest {
        @NonNull
        public final ActorRef<Response> replyTo;

        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo) {
            this.replyTo = replyTo;
        }

        @Override
        public String toString() {
            return String.format("GetCurrencies.Request(%s)", replyTo);
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
            return String.format("GetCurrencies.Response(%s)", currencies);
        }
    }
}
