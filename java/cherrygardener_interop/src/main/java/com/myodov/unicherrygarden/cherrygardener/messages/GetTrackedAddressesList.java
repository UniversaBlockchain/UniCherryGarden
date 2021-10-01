package com.myodov.unicherrygarden.cherrygardener.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;


public class GetTrackedAddressesList {
    public static final @NonNull ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getTrackedAddressesListService");

    public static final class Request implements CherryPickerRequest {
        @NonNull
        public final ActorRef<Response> replyTo;

        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo) {
            this.replyTo = replyTo;
        }

        public String toString() {
            return String.format("GetTrackedAddressesList.Request(%s)", replyTo);
        }
    }

    public static final class Response implements CherryPickerResponse {
        @NonNull
        public final List<String> addresses;

        @JsonCreator
        public Response(@NonNull List<String> addresses) {
            this.addresses = addresses;
        }

        public String toString() {
            return String.format("GetTrackedAddressesList.Response(%s)", addresses);
        }
    }
}
