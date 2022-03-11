package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.function.Function;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.messages.cherrygardener.Ping;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;


/**
 * Akka API command to “ping” the cluster.
 */
public class PingCommand
        extends ConnectorActorCommandImpl<Ping.@NonNull PRequestPayload, PingCommand.Result, Ping.Response> {
    /**
     * During the command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static final class ReceptionistResponse
            extends ReceptionistResponseImpl<Ping.@NonNull PRequestPayload, Result> {
        @JsonCreator
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    Ping.@NonNull PRequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    public static final class InternalResult
            extends InternalResultImpl<Ping.@NonNull Response, Result> {
        public InternalResult(Ping.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    public static final class Result
            extends ConnectorActorCommandImpl.ResultImpl<Ping.@NonNull Response> {
        public Result(Ping.@NonNull Response response) {
            super(response);
        }
    }


    /**
     * Constructor.
     */
    public PingCommand(@NonNull ActorRef<Result> replyTo,
                       Ping.@NonNull PRequestPayload payload) {
        super(replyTo, payload);
    }

    /**
     * Simplified constructor with payload details.
     *
     * @return a function (in Akka style, not just the pure Java Functional interface)
     * that turns the incoming `replyTo` ActorRef into a Command handling this `replyTo` with the payload
     * containing the incoming arguments.
     */
    public static Function<ActorRef<Result>, ConnectorActorMessage> createReplier() {
        return (replyTo) -> new PingCommand(
                replyTo,
                new Ping.PRequestPayload());
    }

    @NonNull
    @Override
    public final ServiceKey<Ping.Request> makeServiceKey(@NonNull String realm) {
        return Ping.makeServiceKey(realm);
    }
}
