package com.myodov.unicherrygarden.messages.connector.impl.actors;

import com.myodov.unicherrygarden.messages.connector.impl.ClientConnectorImpl;

/**
 * Any command (typically from {@link ClientConnectorImpl})
 * for {@link ConnectorActor} to execute.
 * <p>
 * E.g. if a ClientConnector wants to send a message and ask the {@link ConnectorActor} to do something,
 * it will be a {@link ConnectorActorCommand}.
 */
public interface ConnectorActorCommand extends ConnectorActorMessage {
}
