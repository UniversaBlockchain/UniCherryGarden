package com.myodov.unicherrygarden.cherrygardener;

import com.myodov.unicherrygarden.connector.api.ClientConnector;
import com.myodov.unicherrygarden.connector.impl.ClientConnectorImpl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * The set of network/connections settings needed for any network/Akka configuration;
 * they can be taken from the HOCON conf file, or overridden from the CLI options.
 */
final class ConnectionSettings {
    public final int listenPort;

    @NonNull
    public final List<String> connectUrls;

    @NonNull
    public final String realm;

    @Nullable
    public final Long chainId;


    /**
     * Constructor.
     */
    ConnectionSettings(int listenPort,
                       @NonNull List<String> connectUrls,
                       @NonNull String realm,
                       @Nullable Long chainId) {
        assert 0 <= listenPort && listenPort <= 65535 : listenPort;
        assert connectUrls != null : connectUrls;
        assert realm != null : realm;
        assert chainId == null || chainId == -1 || chainId >= 1: chainId;

        this.listenPort = listenPort;
        this.connectUrls = connectUrls;
        this.realm = realm;
        this.chainId = chainId;
    }

    @Override
    public String toString() {
        return String.format("%s(listenPort=%d, connectUrls=%s, realm=%s, chainId=%s)",
                getClass().getSimpleName(),
                listenPort, connectUrls, realm, chainId);
    }

    @NonNull
    public ClientConnector createClientConnector(int mandatoryConfirmations) {
        return new ClientConnectorImpl(connectUrls, realm, listenPort, mandatoryConfirmations, chainId);
    }
}
