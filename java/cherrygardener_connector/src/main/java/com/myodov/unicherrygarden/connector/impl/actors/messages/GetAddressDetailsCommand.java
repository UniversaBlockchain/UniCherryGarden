package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.function.Function;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.messages.cherrypicker.GetAddressDetails;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “get the detailed information about a single address, tracked or not”.
 */
public class GetAddressDetailsCommand
        extends ConnectorActorCommandImpl<GetAddressDetails.@NonNull GADRequestPayload, GetAddressDetailsCommand.Result, GetAddressDetails.Response> {
    /**
     * During the command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse
            extends ReceptionistResponseImpl<GetAddressDetails.@NonNull GADRequestPayload, Result> {
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    GetAddressDetails.@NonNull GADRequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    public static class InternalResult
            extends InternalResultImpl<GetAddressDetails.@NonNull Response, Result> {
        public InternalResult(GetAddressDetails.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    public static class Result
            extends ConnectorActorCommandImpl.ResultImpl<GetAddressDetails.@NonNull Response> {
        public Result(GetAddressDetails.@NonNull Response response) {
            super(response);
        }
    }


    /**
     * Constructor.
     */
    public GetAddressDetailsCommand(@NonNull ActorRef<Result> replyTo,
                                    GetAddressDetails.@NonNull GADRequestPayload payload) {
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
            @NonNull String address) {
        return (replyTo) -> new GetAddressDetailsCommand(
                replyTo,
                new GetAddressDetails.GADRequestPayload(
                        address
                ));
    }

    @Override
    @NonNull
    public final ServiceKey<GetAddressDetails.Request> makeServiceKey(@NonNull String realm) {
        return GetAddressDetails.makeServiceKey(realm);
    }
}
