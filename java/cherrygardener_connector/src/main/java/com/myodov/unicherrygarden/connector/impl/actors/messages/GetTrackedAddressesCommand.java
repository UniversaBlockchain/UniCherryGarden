package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.function.Function;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * Akka API command to “list tracked addresses”.
 */
public class GetTrackedAddressesCommand
        extends ConnectorActorCommandImpl<GetTrackedAddresses.@NonNull GTARequestPayload, GetTrackedAddressesCommand.Result, GetTrackedAddresses.Response> {
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

    /**
     * Simplified constructor with payload details.
     *
     * @return a function (in Akka style, not just the pure Java Functional interface)
     * that turns the incoming `replyTo` ActorRef into a Command handling this `replyTo` with the payload
     * containing the incoming arguments.
     */
    public static Function<ActorRef<Result>, ConnectorActorMessage> createReplier(
            @Nullable Set<String> filterAddresses,
            boolean includeComment,
            boolean includeSyncedFrom) {
        return (replyTo) -> new GetTrackedAddressesCommand(
                replyTo,
                new GetTrackedAddresses.GTARequestPayload(
                        filterAddresses,
                        includeComment,
                        includeSyncedFrom
                ));
    }

    @Override
    @NonNull
    public final ServiceKey<GetTrackedAddresses.Request> makeServiceKey(@NonNull String realm) {
        return GetTrackedAddresses.makeServiceKey(realm);
    }
}
