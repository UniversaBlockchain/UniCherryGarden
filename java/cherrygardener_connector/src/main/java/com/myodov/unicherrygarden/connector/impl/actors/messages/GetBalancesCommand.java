package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “get balances”.
 */
public class GetBalancesCommand
        extends ConnectorActorCommandImpl<GetBalances.@NonNull GBRequestPayload, GetBalancesCommand.Result, GetBalances.Response> {
    /**
     * During the command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     */
    public static class ReceptionistResponse
            extends ConnectorActorCommandImpl.ReceptionistResponseImpl<GetBalances.@NonNull GBRequestPayload, Result> {
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    GetBalances.@NonNull GBRequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    public static class InternalResult
            extends ConnectorActorCommandImpl.InternalResultImpl<GetBalances.@NonNull Response, Result> {
        public InternalResult(GetBalances.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    public static class Result
            extends ConnectorActorCommandImpl.ResultImpl<GetBalances.@NonNull Response> {
        public Result(GetBalances.@NonNull Response response) {
            super(response);
        }
    }


    /**
     * Constructor.
     */
    public GetBalancesCommand(@NonNull ActorRef<Result> replyTo,
                              GetBalances.@NonNull GBRequestPayload payload) {
        super(replyTo, payload);
    }

    @NonNull
    @Override
    public ServiceKey<GetBalances.Request> getServiceKey() {
        return GetBalances.SERVICE_KEY;
    }
}