package com.myodov.unicherrygarden.messages.cherrygardener;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.myodov.unicherrygarden.messages.CherryGardenerRequest;
import com.myodov.unicherrygarden.messages.CherryGardenerResponse;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PingCherryGardener {
    public static final ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(PingCherryGardener.Request.class, "pingCherryGardenerService");

    public static class Request implements CherryGardenerRequest {
        public final ActorRef<Response> replyTo;

        public Request(ActorRef<Response> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class Response implements CherryGardenerResponse {
        @NonNull
        public final String data;

        public Response(@NonNull String data) {
            assert data != null;
            this.data = data;
        }
    }
}
