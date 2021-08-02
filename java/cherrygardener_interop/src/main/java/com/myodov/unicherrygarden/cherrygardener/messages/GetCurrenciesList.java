package com.myodov.unicherrygarden.cherrygardener.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;


public class GetCurrenciesList {
    public static final ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getCurrenciesListService");

    public static final class Request implements CherryGardenerRequest {
        public final ActorRef<Response> replyTo;

        @JsonCreator
        public Request(ActorRef<Response> replyTo) {
            this.replyTo = replyTo;
        }

        public String toString() {
            return String.format("Currencies.GetCurrenciesList(%s)", replyTo);
        }
    }

    public static final class Response implements CherryGardenerResponse {
        public final String value;

        @JsonCreator
        public Response(String value) {
            this.value = value;
        }

        public String toString() {
            return String.format("Currencies.GetCurrenciesListResp(%s)", value);
        }
    }
}
