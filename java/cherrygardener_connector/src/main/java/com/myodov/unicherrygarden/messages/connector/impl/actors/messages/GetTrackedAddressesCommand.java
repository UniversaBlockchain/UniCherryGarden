package com.myodov.unicherrygarden.messages.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorCommandImpl;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “list tracked addresses”.
 */
public class GetTrackedAddressesCommand
        extends ConnectorActorCommandImpl<GetTrackedAddresses.@NonNull GTARequestPayload, GetTrackedAddressesCommand.Result> {

    /**
     * During the command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse
            extends ConnectorActorCommandImpl.ReceptionistResponseImpl<GetTrackedAddresses.@NonNull GTARequestPayload, Result> {
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    GetTrackedAddresses.@NonNull GTARequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    public static class InternalResult
            extends ConnectorActorCommandImpl.InternalResultImpl<GetTrackedAddresses.@NonNull Response, Result> {
        public InternalResult(GetTrackedAddresses.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    public static class Result
            extends ConnectorActorCommandImpl.ResultImpl<GetTrackedAddresses.@NonNull Response> {
        public Result(GetTrackedAddresses.@NonNull Response response) {
            super(response);
        }
    }


    /**
     * Constructor.
     */
    public GetTrackedAddressesCommand(@NonNull ActorRef<Result> replyTo,
                                      GetTrackedAddresses.@NonNull GTARequestPayload payload) {
        super(replyTo, payload);
    }
}
