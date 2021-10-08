package com.myodov.unicherrygarden.messages.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorCommand;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorNotification;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “add tracked addresses”.
 */
public class AddTrackedAddressesCommand implements ConnectorActorCommand {
    /**
     * During AddTrackedAddressesCommand command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse implements ConnectorActorNotification {
        public final Receptionist.@NonNull Listing listing;

        public final AddTrackedAddresses.@NonNull ATARequestPayload payload;

        @NonNull
        public final ActorRef<Result> ataReplyTo;

        public ReceptionistResponse(
                Receptionist.@NonNull Listing listing,
                AddTrackedAddresses.@NonNull ATARequestPayload payload,
                @NonNull ActorRef<Result> ataReplyTo) {
            assert listing != null;
            assert payload != null;
            assert ataReplyTo != null;
            this.listing = listing;
            this.payload = payload;
            this.ataReplyTo = ataReplyTo;
        }
    }

    public static class ATAInternalResult implements ConnectorActorNotification {
        public final AddTrackedAddresses.@NonNull Response response;
        @NonNull
        public final ActorRef<Result> ataReplyTo;

        public ATAInternalResult(AddTrackedAddresses.@NonNull Response response,
                                 @NonNull ActorRef<Result> ataReplyTo) {
            assert response != null;
            assert ataReplyTo != null;
            this.response = response;
            this.ataReplyTo = ataReplyTo;
        }
    }

    public static class Result {
        final AddTrackedAddresses.@NonNull Response response;

        public Result(AddTrackedAddresses.@NonNull Response response) {
            assert response != null;
            this.response = response;
        }
    }

    @NonNull
    public final ActorRef<Result> replyTo;

    public final AddTrackedAddresses.@NonNull ATARequestPayload payload;

    /**
     * Constructor.
     */
    public AddTrackedAddressesCommand(@NonNull ActorRef<Result> replyTo,
                                      AddTrackedAddresses.@NonNull ATARequestPayload payload) {
        assert replyTo != null;
        assert payload != null;
        this.replyTo = replyTo;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return String.format("AddTrackedAddressesCommand(%s, %s)", replyTo, payload);
    }
}
