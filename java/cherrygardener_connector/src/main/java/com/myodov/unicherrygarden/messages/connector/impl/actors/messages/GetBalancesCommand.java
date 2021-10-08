package com.myodov.unicherrygarden.messages.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorCommand;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorNotification;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “get balances”.
 */
public class GetBalancesCommand implements ConnectorActorCommand {
    /**
     * During GetBalancesCommand command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse implements ConnectorActorNotification {
        public final Receptionist.@NonNull Listing listing;

        public final GetBalances.@NonNull GBRequestPayload payload;

        @NonNull
        public final ActorRef<Result> gbReplyTo;

        public ReceptionistResponse(
                Receptionist.@NonNull Listing listing,
                GetBalances.@NonNull GBRequestPayload payload,
                @NonNull ActorRef<Result> gbReplyTo) {
            assert listing != null;
            assert payload != null;
            assert gbReplyTo != null;
            this.listing = listing;
            this.payload = payload;
            this.gbReplyTo = gbReplyTo;
        }
    }

    public static class InternalResult implements ConnectorActorNotification {
        public final GetBalances.@NonNull Response response;
        @NonNull
        public final ActorRef<Result> gbReplyTo;

        public InternalResult(GetBalances.@NonNull Response response,
                              @NonNull ActorRef<Result> gbReplyTo) {
            assert response != null;
            assert gbReplyTo != null;
            this.response = response;
            this.gbReplyTo = gbReplyTo;
        }
    }

    public static class Result {
        final GetBalances.@NonNull Response response;

        public Result(GetBalances.@NonNull Response response) {
            assert response != null;
            this.response = response;
        }
    }

    @NonNull
    public final ActorRef<Result> replyTo;

    public final GetBalances.@NonNull GBRequestPayload payload;

    /**
     * Constructor.
     */
    public GetBalancesCommand(@NonNull ActorRef<Result> replyTo,
                              GetBalances.@NonNull GBRequestPayload payload) {
        assert replyTo != null;
        assert payload != null;
        this.replyTo = replyTo;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return String.format("GetBalancesCommand(%s, %s)", replyTo, payload);
    }
}
