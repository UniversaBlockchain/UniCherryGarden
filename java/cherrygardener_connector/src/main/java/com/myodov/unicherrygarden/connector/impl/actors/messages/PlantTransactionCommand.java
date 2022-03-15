package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.function.Function;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.messages.cherrypicker.GetAddressDetails;
import com.myodov.unicherrygarden.messages.cherryplanter.PlantTransaction;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “plant transaction” to Ethereum blockchain.
 */
public class PlantTransactionCommand
        extends ConnectorActorCommandImpl<PlantTransaction.@NonNull PTRequestPayload, PlantTransactionCommand.Result, PlantTransaction.Response> {
    /**
     * During the command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static final class ReceptionistResponse
            extends ReceptionistResponseImpl<PlantTransaction.@NonNull PTRequestPayload, Result> {
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    PlantTransaction.@NonNull PTRequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    public static final class InternalResult
            extends InternalResultImpl<PlantTransaction.@NonNull Response, Result> {
        public InternalResult(PlantTransaction.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    public static final class Result
            extends ConnectorActorCommandImpl.ResultImpl<PlantTransaction.@NonNull Response> {
        public Result(PlantTransaction.@NonNull Response response) {
            super(response);
        }
    }


    /**
     * Constructor.
     */
    public PlantTransactionCommand(@NonNull ActorRef<Result> replyTo,
                                   PlantTransaction.@NonNull PTRequestPayload payload) {
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
            @NonNull byte[] bytes) {
        assert bytes != null && bytes.length > 0: bytes;

        return (replyTo) -> new PlantTransactionCommand(
                replyTo,
                new PlantTransaction.PTRequestPayload(
                        bytes
                ));
    }

    @Override
    @NonNull
    public final ServiceKey<PlantTransaction.Request> makeServiceKey(@NonNull String realm) {
        return PlantTransaction.makeServiceKey(realm);
    }
}
