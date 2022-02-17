package com.myodov.unicherrygarden.messages.cherrygardener;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.myodov.unicherrygarden.messages.CherryGardenerRequest;
import com.myodov.unicherrygarden.messages.CherryGardenerResponse;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public class PingCherryGardener {
    @NonNull
    public static ServiceKey<Request> makeServiceKey(@NonNull String realm) {
        return ServiceKey.create(
                Request.class,
                String.format("%s:pingCherryGardenerService", Objects.requireNonNull(realm)));
    }


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
