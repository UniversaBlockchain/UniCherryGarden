package com.myodov.unicherrygarden.messages.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorCommandImpl;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “add tracked addresses”.
 */
public class AddTrackedAddressesCommand
        extends ConnectorActorCommandImpl<AddTrackedAddresses.@NonNull ATARequestPayload, AddTrackedAddressesCommand.Result> {

    /**
     * During the command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse
            extends ConnectorActorCommandImpl.ReceptionistResponseImpl<AddTrackedAddresses.@NonNull ATARequestPayload, Result> {
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    AddTrackedAddresses.@NonNull ATARequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    public static class InternalResult
            extends ConnectorActorCommandImpl.InternalResultImpl<AddTrackedAddresses.@NonNull Response, Result> {
        public InternalResult(AddTrackedAddresses.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    public static class Result
            extends ConnectorActorCommandImpl.ResultImpl<AddTrackedAddresses.@NonNull Response> {
        public Result(AddTrackedAddresses.@NonNull Response response) {
            super(response);
        }
    }


    /**
     * Constructor.
     */
    public AddTrackedAddressesCommand(@NonNull ActorRef<Result> replyTo,
                                      AddTrackedAddresses.@NonNull ATARequestPayload payload) {
        super(replyTo, payload);
    }
}
