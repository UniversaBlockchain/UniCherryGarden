package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.function.Function;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTransfers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * Akka API command to “get transfers”.
 */
public class GetTransfersCommand
        extends ConnectorActorCommandImpl<GetTransfers.@NonNull GTRequestPayload, GetTransfersCommand.Result, GetTransfers.Response> {
    /**
     * During the command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse
            extends ConnectorActorCommandImpl.ReceptionistResponseImpl<GetTransfers.@NonNull GTRequestPayload, Result> {
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    GetTransfers.@NonNull GTRequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    public static class InternalResult
            extends ConnectorActorCommandImpl.InternalResultImpl<GetTransfers.@NonNull Response, Result> {
        public InternalResult(GetTransfers.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    public static class Result
            extends ConnectorActorCommandImpl.ResultImpl<GetTransfers.@NonNull Response> {
        public Result(GetTransfers.@NonNull Response response) {
            super(response);
        }
    }


    /**
     * Constructor.
     */
    public GetTransfersCommand(@NonNull ActorRef<Result> replyTo,
                               GetTransfers.@NonNull GTRequestPayload payload) {
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
            int confirmations,
            @Nullable String sender,
            @Nullable String receiver,
            @Nullable Integer fromBlock,
            @Nullable Integer toBlock,
            @Nullable Set<String> filterCurrencyKeys) {
        return (replyTo) -> new GetTransfersCommand(
                replyTo,
                new GetTransfers.GTRequestPayload(confirmations, sender, receiver, fromBlock, toBlock, filterCurrencyKeys));
    }

    @NonNull
    @Override
    public ServiceKey<GetTransfers.Request> getServiceKey() {
        return GetTransfers.SERVICE_KEY;
    }
}
