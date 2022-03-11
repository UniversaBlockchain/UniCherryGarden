package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.function.Function;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

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
    public static final class ReceptionistResponse
            extends ReceptionistResponseImpl<GetBalances.@NonNull GBRequestPayload, Result> {
        public ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                    GetBalances.@NonNull GBRequestPayload payload,
                                    @NonNull ActorRef<Result> replyTo) {
            super(listing, payload, replyTo);
        }
    }


    public static final class InternalResult
            extends InternalResultImpl<GetBalances.@NonNull Response, Result> {
        public InternalResult(GetBalances.@NonNull Response response,
                              @NonNull ActorRef<Result> replyTo) {
            super(response, replyTo);
        }
    }


    public static final class Result
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

    /**
     * Simplified constructor with payload details.
     *
     * @return a function (in Akka style, not just the pure Java Functional interface)
     * that turns the incoming `replyTo` ActorRef into a Command handling this `replyTo` with the payload
     * containing the incoming arguments.
     */
    public static Function<ActorRef<Result>, ConnectorActorMessage> createReplier(
            int confirmations,
            @NonNull String address,
            @Nullable Set<String> filterCurrencyKeys) {
        assert address != null && EthUtils.Addresses.isValidLowercasedAddress(address) : address;
        return (replyTo) -> new GetBalancesCommand(
                replyTo,
                new GetBalances.GBRequestPayload(confirmations, address, filterCurrencyKeys));
    }

    @NonNull
    @Override
    public final ServiceKey<GetBalances.Request> makeServiceKey(@NonNull String realm) {
        return GetBalances.makeServiceKey(realm);
    }
}
