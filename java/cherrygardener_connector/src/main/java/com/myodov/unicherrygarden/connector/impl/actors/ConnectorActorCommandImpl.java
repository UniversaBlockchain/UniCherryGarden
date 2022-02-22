package com.myodov.unicherrygarden.connector.impl.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Abstract generic service command to ConnectorActor.
 */
public abstract class ConnectorActorCommandImpl<
        ReqPayload, Res, Resp
        >
        implements ServiceKeyedConnectorActorCommand<ReqPayload, Resp> {
    /**
     * Generic response from {@link Receptionist},
     * which should be delivered to <code>replyTo</code> actor
     * that handles <code>Res</code> messages.
     * The receptionist replies about the availability of the service
     * which, later, can receive a Reply-able message with <code>payload</code>.
     */
    public static class ReceptionistResponseImpl<ReqPayload, Res>
            implements ConnectorActorNotification {
        public final Receptionist.@NonNull Listing listing;

        @NonNull
        public final ReqPayload payload;

        @NonNull
        public final ActorRef<Res> replyTo;

        /**
         * Must be marked with <code>@JsonCreator</code>!
         */
        public ReceptionistResponseImpl(
                Receptionist.@NonNull Listing listing,
                @NonNull ReqPayload payload,
                @NonNull ActorRef<Res> replyTo) {
            assert listing != null;
            assert payload != null;
            assert replyTo != null;
            this.listing = listing;
            this.payload = payload;
            this.replyTo = replyTo;
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s, %s)",
                    getClass().getSimpleName(),
                    listing, payload, replyTo);
        }
    }

    /**
     * Generic {@link ConnectorActorNotification},
     * which contains a <code>response</code> typed <code>Resp</code>
     * that should eventually be delivered to an actor handling <code>Res</code> messages.
     */
    public static class InternalResultImpl<Resp, Res>
            implements ConnectorActorNotification {
        @NonNull
        public final Resp response;
        @NonNull
        public final ActorRef<Res> replyTo;

        public InternalResultImpl(@NonNull Resp response,
                                  @NonNull ActorRef<Res> replyTo) {
            assert response != null;
            assert replyTo != null;
            this.response = response;
            this.replyTo = replyTo;
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s)",
                    getClass().getSimpleName(),
                    response, replyTo);
        }
    }


    /**
     * Generic result, containing a response typed <code>Resp</code>.
     */
    public static class ResultImpl<Resp> {
        @NonNull
        public final Resp response;

        public ResultImpl(@NonNull Resp response) {
            assert response != null;
            this.response = response;
        }

        @Override
        public String toString() {
            return String.format("%s(%s)",
                    getClass().getSimpleName(),
                    response);
        }
    }


    @NonNull
    public final ActorRef<Res> replyTo;

    @NonNull
    public final ReqPayload payload;

    /**
     * Constructor.
     */
    public ConnectorActorCommandImpl(@NonNull ActorRef<Res> replyTo,
                                     @NonNull ReqPayload payload) {
        assert replyTo != null;
        assert payload != null;
        this.replyTo = replyTo;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s)",
                getClass().getSimpleName(),
                replyTo, payload);
    }
}
