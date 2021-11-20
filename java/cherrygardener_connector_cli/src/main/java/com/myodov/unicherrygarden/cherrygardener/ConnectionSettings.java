package com.myodov.unicherrygarden.cherrygardener;

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

    /**
     * Constructor.
     */
    ConnectionSettings(int listenPort, @NonNull List<String> connectUrls) {
        assert 0 <= listenPort && listenPort <= 65535 : listenPort;
        assert connectUrls != null : connectUrls;

        this.listenPort = listenPort;
        this.connectUrls = connectUrls;
    }

    @Override
    public String toString() {
        return String.format("ConnectionSettings(listenPort=%d, connectUrls=%s)",
                listenPort, connectUrls);
    }
}
