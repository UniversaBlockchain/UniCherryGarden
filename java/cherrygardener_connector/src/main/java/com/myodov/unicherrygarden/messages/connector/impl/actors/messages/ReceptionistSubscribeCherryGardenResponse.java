package com.myodov.unicherrygarden.messages.connector.impl.actors.messages;

import akka.actor.typed.receptionist.Receptionist;
import com.myodov.unicherrygarden.messages.connector.impl.actors.ConnectorActorNotification;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ReceptionistSubscribeCherryGardenResponse implements ConnectorActorNotification {
    public final Receptionist.@NonNull Listing listing;

     public ReceptionistSubscribeCherryGardenResponse(Receptionist.@NonNull Listing listing) {
        assert listing != null;
        this.listing = listing;
    }
}
