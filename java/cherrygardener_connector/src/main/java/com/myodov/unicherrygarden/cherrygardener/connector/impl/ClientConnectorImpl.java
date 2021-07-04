package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import akka.actor.typed.ActorSystem;
import com.myodov.unicherrygarden.ethereum.Ethereum;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * The implementation of CherryGardener client connector API.
 */
public class ClientConnectorImpl {
    final Logger logger = LoggerFactory.getLogger(ClientConnectorImpl.class);


    /**
     * Constructor of CherryGardener client connnector.
     *
     * @param gardenerAkkaUrl URL of CherryGardener service, that
     */
    public ClientConnectorImpl(@NonNull String gardenerAkkaUrl,
                               @NonNull String gardenWatcherAkkaUrl
//                               @NonNull String jdbcUrl
    ) {
        assert gardenerAkkaUrl != null;
        assert gardenWatcherAkkaUrl != null;
//        assert jdbcUrl != null;
        logger.info("Launching CherryGardener connector: gardener url {}, garden watcher url {}",
                gardenerAkkaUrl, gardenWatcherAkkaUrl);

    }

    public static void main(String[] args) {
        System.out.println("Ohai2");
        final ECKeyPair pair;

        try {
            pair = Keys.createEcKeyPair();

            final String address = Keys.getAddress(pair);
            System.out.printf("Address: 0x%s\n", address);

            final byte[] bytes = Numeric.toBytesPadded(pair.getPrivateKey(), Ethereum.PRIVATE_KEY_SIZE_BYTES);
            System.out.printf("Private key: %s\n", Hex.toHexString(bytes));
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }

        final ActorSystem<ConnectorActor.Message> connectorMain = ActorSystem.create(
                ConnectorActor.create(), "CherryGarden");
        connectorMain.tell(new ConnectorActor.Message("Hiiii"));
    }
}