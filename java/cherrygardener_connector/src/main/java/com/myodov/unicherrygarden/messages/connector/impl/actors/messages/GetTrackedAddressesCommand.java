package com.myodov.unicherrygarden.messages.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorCommand;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorNotification;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “list tracked addresses”.
 */
public class GetTrackedAddressesCommand implements ConnectorActorCommand {
    /**
     * During GetTrackedAddressesCommand command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse implements ConnectorActorNotification {
        public final Receptionist.@NonNull Listing listing;

        public final GetTrackedAddresses.@NonNull GTARequestPayload payload;

        @NonNull
        public final ActorRef<Result> gtaReplyTo;

        public ReceptionistResponse(
                Receptionist.@NonNull Listing listing,
                GetTrackedAddresses.@NonNull GTARequestPayload payload,
                @NonNull ActorRef<Result> gtaReplyTo) {
            assert listing != null;
            assert payload != null;
            assert gtaReplyTo != null;
            this.listing = listing;
            this.payload = payload;
            this.gtaReplyTo = gtaReplyTo;
        }
    }

    public static class InternalResult implements ConnectorActorNotification {
        public final GetTrackedAddresses.Response response;
        @NonNull
        public final ActorRef<Result> gtaReplyTo;

        public InternalResult(GetTrackedAddresses.Response response,
                              @NonNull ActorRef<Result> gtaReplyTo) {
            assert response != null;
            assert gtaReplyTo != null;
            this.response = response;
            this.gtaReplyTo = gtaReplyTo;
        }
    }

    public static class Result {
        public final GetTrackedAddresses.Response response;

        public Result(GetTrackedAddresses.Response response) {
            assert response != null;
            this.response = response;
        }
    }

    @NonNull
    public final ActorRef<Result> replyTo;

    public final GetTrackedAddresses.@NonNull GTARequestPayload payload;

    /**
     * Constructor.
     */
    public GetTrackedAddressesCommand(@NonNull ActorRef<Result> replyTo,
                                      GetTrackedAddresses.@NonNull GTARequestPayload payload) {
        assert replyTo != null;
        assert payload != null;
        this.replyTo = replyTo;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return String.format("GetTrackedAddressesCommand(%s, %s)", replyTo, payload);
    }
}
