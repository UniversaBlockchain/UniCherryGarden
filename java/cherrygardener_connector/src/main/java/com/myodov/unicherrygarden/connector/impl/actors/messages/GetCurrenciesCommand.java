package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Akka API command to “get currencies” (which are supported by the system).
 * <p>
 * ReqPayload=`GetCurrencies.@NonNull GCRequestPayload`
 * Res=`GetCurrencies.Result`
 * Resp=`GetCurrencies.Response`
 */
public class GetCurrenciesCommand
        extends ConnectorActorCommandImpl<GetCurrencies.@NonNull GCRequestPayload, GetCurrenciesCommand.Result, GetCurrencies.Response> {
    /**
     * During the command execution, we ask the Receptionist
     * about available service providing this command; this class is the response adapted
     * to handle the command.
     * <p>
     * NOTE:
     * ReqPayload=`GetCurrencies.@NonNull GCRequestPayload`
     * Res=`Result`
     */
    public static class ReceptionistResponse
            extends ConnectorActorCommandImpl.ReceptionistResponseImpl<GetCurrencies.@NonNull GCRequestPayload, Result> {
        @JsonCreator
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    GetCurrencies.@NonNull GCRequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    /**
     * Resp=`GetCurrencies.@NonNull Response`
     * Res=`Result`
     */
    public static class InternalResult
            extends ConnectorActorCommandImpl.InternalResultImpl<GetCurrencies.@NonNull Response, Result> {
        public InternalResult(GetCurrencies.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    /**
     * Resp=`GetCurrencies.@NonNull Response`
     */
    public static class Result
            extends ConnectorActorCommandImpl.ResultImpl<GetCurrencies.@NonNull Response> {
        public Result(GetCurrencies.@NonNull Response response) {
            super(response);
        }
    }


    /**
     * Constructor.
     */
    public GetCurrenciesCommand(@NonNull ActorRef<Result> replyTo,
                                GetCurrencies.@NonNull GCRequestPayload payload) {
        super(replyTo, payload);
    }

    /**
     * Simplified constructor with payload details.
     */
    public static GetCurrenciesCommand create(@NonNull ActorRef<Result> replyTo,
                                              boolean getVerified,
                                              boolean getUnverified) {
        assert (getVerified || getUnverified) : String.format("%s/%s", getVerified, getUnverified);

        return new GetCurrenciesCommand(
                replyTo,
                new GetCurrencies.GCRequestPayload(getVerified, getUnverified));
    }

    @NonNull
    @Override
    public ServiceKey<GetCurrencies.Request> getServiceKey() {
        return GetCurrencies.SERVICE_KEY;
    }
}
