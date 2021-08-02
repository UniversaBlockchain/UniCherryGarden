package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.myodov.unicherrygarden.cherrygardener.connector.api.ClientConnector;
import com.myodov.unicherrygarden.cherrygardener.connector.api.types.Currency;
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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * The implementation of CherryGardener client connector API.
 *
 * @implSpec The default implementation is the primary API for CherryGardener calls;
 * and by default, it uses {@link ConnectorActor} as the primary
 */
public class ClientConnectorImpl implements ClientConnector {
    public static final Duration LAUNCH_TIMEOUT = Duration.ofSeconds(10);

    final Logger logger = LoggerFactory.getLogger(ClientConnectorImpl.class);

    @NonNull
    private final ActorSystem<ConnectorActor.Message> actorSystem;

    /**
     * Constructor of CherryGardener client connector.
     *
     * @param gardenerAkkaUrl URL of CherryGardener service, that
     */
    public ClientConnectorImpl(@NonNull String gardenerAkkaUrl,
                               @NonNull String gardenWatcherAkkaUrl) {
        assert gardenerAkkaUrl != null;
        assert gardenWatcherAkkaUrl != null;
        logger.info("Launching CherryGardener connector: gardener url {}, garden watcher url {}",
                gardenerAkkaUrl, gardenWatcherAkkaUrl);

        actorSystem = ActorSystem.create(ConnectorActor.create(), "CherryGarden");

        logger.debug("Waiting to boot...");
        final CompletionStage<ConnectorActor.WaitForBootCommand.BootCompleted> stage = AskPattern.ask(
                actorSystem,
                ConnectorActor.WaitForBootCommand::new,
                LAUNCH_TIMEOUT,
                actorSystem.scheduler());
        final ConnectorActor.WaitForBootCommand.BootCompleted bootCompleted = stage.toCompletableFuture().join();
        logger.debug("Boot is completed!");
    }

    @Override
    public void shutdown() {
        actorSystem.terminate();
    }

    /**
     * Main loop execution.
     */
    public static void main(String[] args) {
        System.out.println("Oh");
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
    }

    @Override
    @NonNull
    public List<Currency> getCurrencies() {
        final CompletionStage<ConnectorActor.ListSupportedCurrenciesCommand.Result> stage =
                AskPattern.ask(
                        actorSystem,
                        ConnectorActor.ListSupportedCurrenciesCommand::new,
                        ConnectorActor.DEFAULT_CALL_TIMEOUT,
                        actorSystem.scheduler());

        final ConnectorActor.ListSupportedCurrenciesCommand.Result result = stage.toCompletableFuture().join();
        System.err.printf("Received getCurrencies response: %s\n", result.response.value);
        return List.of();
//        return result.response;
    }
}
