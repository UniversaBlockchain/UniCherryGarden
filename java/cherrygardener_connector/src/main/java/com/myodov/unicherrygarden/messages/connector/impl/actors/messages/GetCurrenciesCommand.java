package com.myodov.unicherrygarden.messages.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorCommand;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorNotification;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “get currencies” (which are supported by the system).
 */
public class GetCurrenciesCommand implements ConnectorActorCommand {
    /**
     * During GetCurrencies command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse implements ConnectorActorNotification {
        public final Receptionist.@NonNull Listing listing;

        public final GetCurrencies.@NonNull GCRequestPayload payload;

        @NonNull
        public final ActorRef<Result> gcReplyTo;

        public ReceptionistResponse(
                Receptionist.@NonNull Listing listing,
                GetCurrencies.@NonNull GCRequestPayload payload,
                @NonNull ActorRef<Result> gcReplyTo) {
            assert listing != null;
            assert payload != null;
            assert gcReplyTo != null;
            this.listing = listing;
            this.payload = payload;
            this.gcReplyTo = gcReplyTo;
        }
    }

    public static class InternalResult implements ConnectorActorNotification {
        public final GetCurrencies.Response response;
        @NonNull
        public final ActorRef<Result> gcReplyTo;

        public InternalResult(GetCurrencies.Response response,
                              @NonNull ActorRef<Result> gcReplyTo) {
            assert response != null;
            assert gcReplyTo != null;
            this.response = response;
            this.gcReplyTo = gcReplyTo;
        }
    }

    public static class Result {
        public final GetCurrencies.Response response;

        public Result(GetCurrencies.Response response) {
            assert response != null;
            this.response = response;
        }
    }


    @NonNull
    public final ActorRef<Result> replyTo;

    public final GetCurrencies.@NonNull GCRequestPayload payload;

    /**
     * Constructor.
     */
    public GetCurrenciesCommand(@NonNull ActorRef<Result> replyTo,
                                GetCurrencies.@NonNull GCRequestPayload payload) {
        assert replyTo != null;
        assert payload != null;
        this.replyTo = replyTo;
        this.payload = payload;
    }

    /**
     * Simplified constructor, with empty payload.
     */
    public GetCurrenciesCommand(@NonNull ActorRef<Result> replyTo) {
        this(replyTo, new GetCurrencies.GCRequestPayload());
    }

    @Override
    public String toString() {
        return String.format("GetCurrenciesCommand(%s, %s)", replyTo, payload);
    }
}
