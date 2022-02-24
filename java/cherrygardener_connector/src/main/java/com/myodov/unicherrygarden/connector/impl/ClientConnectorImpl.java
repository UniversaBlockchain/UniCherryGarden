package com.myodov.unicherrygarden.connector.impl;

import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.ClusterCommand;
import akka.cluster.typed.JoinSeedNodes;
import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import com.myodov.unicherrygarden.connector.api.*;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActor;
import com.myodov.unicherrygarden.connector.impl.actors.ConnectorActorMessage;
import com.myodov.unicherrygarden.connector.impl.actors.messages.GetCurrenciesCommand;
import com.myodov.unicherrygarden.connector.impl.actors.messages.PingCommand;
import com.myodov.unicherrygarden.connector.impl.actors.messages.WaitForBootCommand;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import com.myodov.unicherrygarden.messages.cherrygardener.Ping;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.tx.ChainIdLong;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * The implementation of CherryGardener client connector API.
 *
 * @implSpec The default implementation is the primary API for CherryGardener calls;
 * and by default, it uses {@link ConnectorActor} as the primary
 */
public final class ClientConnectorImpl implements ClientConnector {
    @NonNull
    protected static final AddressOwnershipConfirmator confirmator = new AddressOwnershipConfirmatorImpl();
    @NonNull
    protected static final Keygen keygen = new KeygenImpl();
    @Nullable
    protected final Observer observer;
    @Nullable
    protected final Sender sender;

    public static final Duration LAUNCH_TIMEOUT = Duration.ofSeconds(10);

    final Logger logger = LoggerFactory.getLogger(ClientConnectorImpl.class);


    @NonNull
    private final ActorSystem<ConnectorActorMessage> actorSystem;

    /**
     * The “Realm” of the connector (to distinguish multiple different CherryGardens running in the same environment).
     */
    @NonNull
    private final String realm;

    /**
     * Whether the connector is launched in “offline mode”.
     */
    private final boolean offlineMode;

    protected final int mandatoryConfirmations;

    /**
     * Primary and most detailed constructor of CherryGardener client connector.
     *
     * @param gardenerUrls           the list of URLs of CherryGardener service (strings containing host and port),
     *                               e.g. <code>List.of("127.0.0.1:2551", "127.0.0.1:2552")</code>.
     *                               If the list is empty, the connector is executed in “offline mode”,
     *                               capable to perform only the operations which are executed locally.
     * @param realm                  the UniCherryGarden “realm” where the connector lives in.
     *                               There may be multiple CherryGarden instances running simultaneously
     *                               (and even in the same Akka cluster). For example, one may handle the
     *                               Ethereum Mainnet blockchain, another for Ethereum Ropsten testnet, some other one
     *                               for Ethereum Classic. Each of them will have a separate realm as a text string,
     *                               here you should put the same string as the one configured in the particular
     *                               instance of CherryGarden.
     * @param chainId                the Ethereum network Chain ID.
     *                               Use 1 for Ethereum Mainnet, 3 for Ropsten testnet, 4 for Rinkeby testnet, and other
     *                               standard Chain IDs.
     *                               For reference, see some Chain ID values at
     *                               <a href="https://github.com/web3j/web3j/blob/master/core/src/main/java/org/web3j/tx/ChainIdLong.java">ChainIdLong.java</a>
     *                               in Web3j source.
     *                               If <code>null</code>, the Chain ID will be autodetected (what requires accesing
     *                               the CherryGarden by network).
     *                               So it cannot be <code>null</code> when in “offline mode”.
     * @param listenPort             IP the client should listen to. Between 0 and 65535 (inclusive;
     *                               0 means it will be autogenerated).
     *                               Note your client (at port) and the CherryGardener service URLs should be mutually
     *                               reachable.
     * @param mandatoryConfirmations the necessary number of Ethereum block confirmations to be sure
     *                               in any blockchain data stability.
     *                               None of the functionality (getting balances, getting the list of transfers, etc)
     *                               will return any data confirmed with less than this amount of confirmations/blocks.
     *                               <p>
     *                               Some API calls may let you add extra number of confirmations on top of this number.
     *                               0 confirmations (should not be used!) means the transaction is considered valid
     *                               if it is available in the latest mined block; 1 confirmation for a transaction
     *                               means there is a 1 block mined <i>after</i> the transaction.
     *                               <p>
     *                               conds.
     *                               <p>
     *                               Some numbers to consider:
     *                               <ul>
     *                               <li>5 confirmations – often been considered as “everyday level of security”.</li>
     *                               <li>12 confirmations – often been considered as “everyday level of security”.</li>
     *                               <li>20 confirmations – used on large exchange Kraken for most transactions.</li>
     *                               <li>250, 375, even 500 confirmations – used by most conservative crypto
     *                               exchanges.</li>
     *                               </ul>
     * @throws CompletionException if failed to initialize.
     */
    public ClientConnectorImpl(@NonNull List<String> gardenerUrls,
                               @NonNull String realm,
                               @Nullable Long chainId,
                               int listenPort,
                               int mandatoryConfirmations) throws CompletionException {
        if (gardenerUrls == null) {
            throw new UniCherryGardenError.ArgumentError("gardenerUrls should not be null! " +
                    "Pass an empty list of URLs if you want an offline mode.");
        }
        offlineMode = gardenerUrls.isEmpty();
        if (realm == null) {
            throw new UniCherryGardenError.ArgumentError("realm should not be null!");
        }
        if (chainId != null && chainId < 1 && chainId != -1) {
            throw new UniCherryGardenError.ArgumentError("realm (if defined) should be 1 or higher, or -1 for \"None\"!");
        }
        if (offlineMode && chainId == null) {
            throw new UniCherryGardenError.ArgumentError("chainId cannot be null (what means autodetect) in offline mode!");
        }
        if (!gardenerUrls.isEmpty()) {
            // The listenPort and mandatoryConfirmations validations are relevant only if non-offline mode
            if (!(0 <= listenPort && listenPort <= 65535)) {
                throw new UniCherryGardenError.ArgumentError(
                        "When in non-offline mode, listenPort should be " +
                                "between 0 and 65535 inclusive!");
            }
            if (mandatoryConfirmations < 0) {
                throw new UniCherryGardenError.ArgumentError(
                        "When in non-offline mode, mandatoryConfirmations should be " +
                                "at least 0, the higher the better!");
            }
        }

        this.realm = realm;
        this.mandatoryConfirmations = mandatoryConfirmations;

        logger.info("Launching CherryGardener connector: gardener urls {}, listen port {}, mandatory confirmations {}",
                gardenerUrls, listenPort, mandatoryConfirmations);

        final Config config = ConfigFactory
                .parseString(String.format("akka.remote.artery.canonical.port=%d", listenPort))
                .withFallback(ConfigFactory.load());

        // All Cluster nodes should have the same name of Actor System
        final String actorSystemName = String.format("CherryGarden-%s", realm);
        actorSystem = ActorSystem.create(ConnectorActor.create(realm), actorSystemName, config);
        if (offlineMode) {
            this.observer = null;
            this.sender = new SenderImpl(chainId);
            logger.warn("Creating Connector in offline mode!");
        } else {
            final List<Address> seedNodes = gardenerUrls.stream()
                    .map(url -> AddressFromURIString.parse(String.format("akka://CherryGarden-%s@%s", realm, url)))
                    .collect(Collectors.toList());
            logger.info("Connecting to {}", seedNodes);

            final Cluster cluster = Cluster.get(actorSystem);
            final ActorRef<ClusterCommand> clusterManager = cluster.manager();
            clusterManager.tell(new JoinSeedNodes(seedNodes));

            logger.debug("Waiting to boot...");
            try {
                final CompletionStage<WaitForBootCommand.BootCompleted> stage = AskPattern.ask(
                        actorSystem,
                        WaitForBootCommand::new,
                        LAUNCH_TIMEOUT,
                        actorSystem.scheduler());
                stage.toCompletableFuture().join();
            } catch (CompletionException | CancellationException exc) {
                logger.error("Could not boot ClientConnector; shutting down Akka", exc);
                close();
                throw exc;
            }
            logger.debug("Boot is completed!");

            // If we don’t have ChainID defined, we do a ping

            final long chainIdFinal;
            if (chainId == null) {
                final Ping.Response pingResponse = ping();

                if (pingResponse.isFailure()) {
                    throw new UniCherryGardenError.NetworkError(
                            "Could not ping UniCherryGarden! Problem: " +
                                    pingResponse.getFailure());
                } else {
                    final Ping.PingRequestResultPayload payload = pingResponse.getPayloadAsSuccessful();
                    logger.debug("On initial ping, got the Chain ID \"{}\"", payload.chainId);
                    chainIdFinal = payload.chainId;
                }
            } else {
                chainIdFinal = chainId;
            }

            // Setting up the remaining subsystems

            this.observer = new ObserverImpl(actorSystem, mandatoryConfirmations);
            this.sender = new SenderImpl(actorSystem, chainIdFinal);
        }
    }

    /**
     * Simplified constructor, with safe defaults (24 confirmations, around 6 minutes, and connecting to mainnet).
     *
     * @param gardenerUrls the list of URLs of CherryGardener service (strings containing host and port),
     *                     e.g. <code>List.of("127.0.0.1:2551", "127.0.0.1:2552")</code>.
     *                     If the list is empty, the connector is executed in “offline mode”,
     *                     capable to perform only the operations which are executed locally.
     * @param realm        the UniCherryGarden “realm” where the connector lives in.
     *                     There may be multiple CherryGarden instances running simultaneously
     *                     (and even in the same Akka cluster). For example, one may handle the
     *                     Ethereum Mainnet blockchain, another for Ethereum Ropsten testnet, some other one
     *                     for Ethereum Classic. Each of them will have a separate realm as a text string,
     *                     here you should put the same string as the one configured in the particular
     *                     instance of CherryGarden.
     * @param chainId      the Ethereum network Chain ID.
     *                     Use 1 for Ethereum Mainnet, 3 for Ropsten testnet, 4 for Rinkeby testnet, and other
     *                     standard Chain IDs.
     *                     For reference, see some Chain ID values at
     *                     <a href="https://github.com/web3j/web3j/blob/master/core/src/main/java/org/web3j/tx/ChainIdLong.java">ChainIdLong.java</a>
     *                     in Web3j source.
     * @param listenPort   IP the client should listen to. Between 0 and 65535 (inclusive, 0 means the port
     *                     will be autogenerated).
     *                     Note your client (at port) and the CherryGardener service URLs should be mutually
     *                     reachable.
     * @throws CompletionException if failed to initialize.
     */
    @SuppressWarnings("unused")
    public ClientConnectorImpl(@NonNull List<String> gardenerUrls,
                               @NonNull String realm,
                               long chainId,
                               int listenPort) throws CompletionException {
        this(gardenerUrls, realm, chainId, listenPort, 24);
    }

    /**
     * Simplified constructor, with autogenerating the `listenPort` (i.e. using 0 as the port),
     * and safe defaults for the Ethereum blockchain data (24 confirmations, around 6 minutes).
     *
     * @param gardenerUrls the list of URLs of CherryGardener service (strings containing host and port),
     *                     e.g. <code>List.of("127.0.0.1:2551", "127.0.0.1:2552")</code>.
     *                     If the list is empty, the connector is executed in “offline mode”,
     *                     capable to perform only the operations which are executed locally.
     * @throws CompletionException if failed to initialize.
     */
    @SuppressWarnings("unused")
    public ClientConnectorImpl(@NonNull List<String> gardenerUrls,
                               @NonNull String realm) throws CompletionException {
        this(gardenerUrls, realm, ChainIdLong.MAINNET, 0, 24);
    }

    /**
     * Simplified constructor, opening the connector in offline mode.
     *
     * @throws CompletionException if failed to initialize.
     */
    @SuppressWarnings("unused")
    public ClientConnectorImpl() throws CompletionException {
        this(Collections.emptyList(), "", 0, 0);
    }

    @Override
    public void close() {
        actorSystem.terminate();
    }

    //
    // Methods that return the engines/subsystems
    //

    @Override
    @NonNull
    public AddressOwnershipConfirmator getAddressOwnershipConfirmator() {
        return confirmator;
    }

    @Override
    @NonNull
    public Keygen getKeygen() {
        return keygen;
    }

    @Override
    @Nullable
    public Observer getObserver() {
        // “Observer is null/not initialized” if and only if “offlineMode is enabled”.
        assert offlineMode == (observer == null) :
                String.format("%s, %s", offlineMode, observer);
        return observer;
    }

    @Override
    @Nullable
    public Sender getSender() {
        // “Sender is null/not initialized” if and only if “offlineMode is enabled”.
        assert offlineMode == (sender == null) :
                String.format("%s, %s", offlineMode, sender);
        return sender;
    }

    //
    // Methods that return the actual business logic data.
    //

    @Override
    public Ping.@NonNull Response ping() {
        if (offlineMode) {
            return Ping.Response.fromCommonFailure(FailurePayload.NOT_AVAILABLE_IN_OFFLINE_MODE);
        } else {
            final CompletionStage<PingCommand.Result> stage =
                    AskPattern.ask(
                            actorSystem,
                            PingCommand.createReplier(),
                            ConnectorActor.DEFAULT_CALL_TIMEOUT,
                            actorSystem.scheduler());

            try {
                return stage.toCompletableFuture().join().response;
            } catch (CancellationException | CompletionException exc) {
                logger.error("Could not complete PingCommand command", exc);
                return Ping.Response.fromCommonFailure(FailurePayload.CANCELLATION_COMPLETION_FAILURE);
            }
        }
    }

    @Override
    public GetCurrencies.@NonNull Response getCurrencies(
            @Nullable Set<String> filterCurrencyKeys,
            boolean getVerified,
            boolean getUnverified
    ) {
        if (filterCurrencyKeys != null) {
            filterCurrencyKeys.forEach(ck -> Validators.requireValidCurrencyKey("filterCurrencyKeys item", ck));
        }

        if (offlineMode) {
            return GetCurrencies.Response.fromCommonFailure(FailurePayload.NOT_AVAILABLE_IN_OFFLINE_MODE);
        } else {
            final CompletionStage<GetCurrenciesCommand.Result> stage =
                    AskPattern.ask(
                            actorSystem,
                            GetCurrenciesCommand.createReplier(filterCurrencyKeys, getVerified, getUnverified),
                            ConnectorActor.DEFAULT_CALL_TIMEOUT,
                            actorSystem.scheduler());

            try {
                return stage.toCompletableFuture().join().response;
            } catch (CancellationException | CompletionException exc) {
                logger.error("Could not complete GetCurrenciesCommand command", exc);
                return GetCurrencies.Response.fromCommonFailure(FailurePayload.CANCELLATION_COMPLETION_FAILURE);
            }
        }
    }
}
