package com.myodov.unicherrygarden.connector.impl.actors.messages;

import akka.actor.typed.ActorRef;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorCommand;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Actor convenience command that waits for the cluster to be up
 * (the connector to finish connecting to it, and CherryGardener being available).
 */
public class WaitForBootCommand implements ConnectorActorCommand {

    public static final class BootCompleted {
        public BootCompleted() {
        }

        @Override
        public String toString() {
            return "WaitForBootCommand.BootCompleted()";
        }
    }

    @NonNull
    public final ActorRef<BootCompleted> replyTo;


    public WaitForBootCommand(@NonNull ActorRef<BootCompleted> replyTo) {
        assert replyTo != null;
        this.replyTo = replyTo;
    }

    @Override
    public String toString() {
        return "WaitForBootCommand()";
    }
}
