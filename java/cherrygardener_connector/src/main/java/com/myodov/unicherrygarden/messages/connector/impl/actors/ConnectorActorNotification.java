package com.myodov.unicherrygarden.messages.connector.impl.actors;

/**
 * Any notification (typically a reply for any message outgoing from {@link ConnectorActor})
 * for {@link ConnectorActor} to handle.
 * <p>
 * E.g. if the {@link ConnectorActor} has sent an outgoing message to some external actor, and expects a reply,
 * it will be a {@link ConnectorActorNotification}.
 */
public interface ConnectorActorNotification extends ConnectorActorMessage {
}
