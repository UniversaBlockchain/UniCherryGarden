package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommandImpl;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Akka API command to “add tracked addresses”.
 */
public class AddTrackedAddressesCommand
        extends ConnectorActorCommandImpl<AddTrackedAddresses.@NonNull ATARequestPayload, AddTrackedAddressesCommand.Result, AddTrackedAddresses.Response> {
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

    /**
     * Simplified constructor with payload details.
     */
    public static AddTrackedAddressesCommand create(@NonNull ActorRef<Result> replyTo,
                                                    AddTrackedAddresses.@NonNull StartTrackingAddressMode trackingMode,
                                                    @NonNull List<AddTrackedAddresses.AddressDataToTrack> addressesToTrack,
                                                    @Nullable Integer fromBlock) {
        return new AddTrackedAddressesCommand(
                replyTo,
                new AddTrackedAddresses.ATARequestPayload(trackingMode, addressesToTrack, fromBlock));
    }

    @NonNull
    @Override
    public ServiceKey<AddTrackedAddresses.Request> getServiceKey() {
        return AddTrackedAddresses.SERVICE_KEY;
    }
}
