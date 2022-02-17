package com.myodov.unicherrygarden.connector.impl.actors;

import akka.actor.typed.receptionist.ServiceKey;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import com.myodov.unicherrygarden.connector.impl.ClientConnectorImpl;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Any command (typically from {@link ClientConnectorImpl})
 * for {@link ConnectorActor} to execute, which is based on some UniCherryGarden ServiceKey.
 */
public interface ServiceKeyedConnectorActorCommand<ReqPayload, Resp>
        extends ConnectorActorCommand {
    @NonNull
    ServiceKey<? extends RequestWithReplyTo<ReqPayload, Resp>> makeServiceKey(@NonNull String realm);
}
