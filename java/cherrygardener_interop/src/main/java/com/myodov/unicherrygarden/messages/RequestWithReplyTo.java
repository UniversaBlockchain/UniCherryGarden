package com.myodov.unicherrygarden.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Abstract generic request which has a payload (<code>ReqPayload</code>)
 * and <code>replyTo</code> field, which is an {@link ActorRef}
 * to an actor that can handle <code>Resp</code> messages.
 * <p>
 * Also assumes each request has a ServiceKey, which can be reached
 * via {@link #}
 */
public class RequestWithReplyTo<ReqPayload, Resp> {
    @NonNull
    public final ActorRef<Resp> replyTo;

    @NonNull
    public final ReqPayload payload;

    @JsonCreator
    public RequestWithReplyTo(@NonNull ActorRef<Resp> replyTo,
                              @NonNull ReqPayload payload) {
        assert replyTo != null;
        assert payload != null;
        this.replyTo = replyTo;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s)",
                this.getClass().getSimpleName(), replyTo, payload);
    }
}
