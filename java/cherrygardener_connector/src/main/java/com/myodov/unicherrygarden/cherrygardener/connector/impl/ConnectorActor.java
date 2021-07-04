package com.myodov.unicherrygarden.cherrygardener.connector.impl;


import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ConnectorActor extends AbstractBehavior<ConnectorActor.Message> {
    public static class Message {
        public final String text;

        public Message(@NonNull String text) {
            assert text != null;
            this.text = text;
        }

        public String toString() {
            return String.format("ConnectorActor.Message(%s)", text);
        }
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(ConnectorActor::new);
    }

    private ConnectorActor(ActorContext<Message> context) {
        super(context);
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder().onMessage(Message.class, this::onAkkaMessage).build();
    }

    private Behavior<Message> onAkkaMessage(Message msg) {
        System.out.printf("Received message %s\n", msg);
        return this;
    }
}
