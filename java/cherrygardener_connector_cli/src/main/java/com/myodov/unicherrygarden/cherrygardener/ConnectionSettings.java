package com.myodov.unicherrygarden.cherrygardener;

import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.impl.ClientConnectorImpl;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * The set of network/connections settings needed for any network/Akka configuration;
 * they can be taken from the HOCON conf file, or overridden from the CLI options.
 */
class ConnectionSettings {
    public final int listenPort;

    @NonNull
    public final List<String> connectUrls;

    @NonNull
    public final String realm;

    public final long chainId;


    /**
     * Constructor.
     */
    ConnectionSettings(int listenPort,
                       @NonNull List<String> connectUrls,
                       @NonNull String realm,
                       long chainId) {
        assert 0 <= listenPort && listenPort <= 65535 : listenPort;
        assert connectUrls != null : connectUrls;
        assert realm != null : realm;
        assert chainId == -1 || chainId >= 1: chainId;

        this.listenPort = listenPort;
        this.connectUrls = connectUrls;
        this.realm = realm;
        this.chainId = chainId;
    }

    @Override
    public String toString() {
        return String.format("ConnectionSettings(listenPort=%d, connectUrls=%s, realm=%s, chainId=%s)",
                listenPort, connectUrls, realm, chainId);
    }

    @NonNull
    public final ClientConnector createClientConnector(int mandatoryConfirmations) {
        return new ClientConnectorImpl(connectUrls, realm, chainId, listenPort, mandatoryConfirmations);
    }

    @NonNull
    public final ClientConnector createClientConnector() {
        return new ClientConnectorImpl(connectUrls, realm, chainId, listenPort);
    }
}
