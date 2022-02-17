package com.myodov.unicherrygarden.connector.impl.actors;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.function.Function;
import com.myodov.unicherrygarden.connector.impl.ClientConnectorImpl;
import com.myodov.unicherrygarden.connector.impl.actors.messages.*;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import com.myodov.unicherrygarden.messages.cherrygardener.PingCherryGardener;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTransfers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Primary Akka actor to handle all the operations for {@link ClientConnectorImpl}.
 */
public class ConnectorActor extends AbstractBehavior<ConnectorActorMessage> {
    final Logger logger = LoggerFactory.getLogger(ConnectorActor.class);

    /**
     * Default timeout for all Akka calls.
     */
    public static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(15);


    @NonNull
    private final ActorRef<Receptionist.Listing> receptionistSubscribeCherryGardenResponseAdapter;

    @NonNull
    private final String realm;

    private final List<ActorRef<WaitForBootCommand.BootCompleted>> waitForBootCallers = new ArrayList<>();

    //
    // Cached service keys:
    //

    // CherryGardener
    @NonNull
    private final ServiceKey<GetCurrencies.Request> skGetCurrencies;
    @NonNull
    private final ServiceKey<PingCherryGardener.Request> skPingCherryGardener;
    // CherryPicker
    @NonNull
    private final ServiceKey<AddTrackedAddresses.Request> skAddTrackedAddresses;
    @NonNull
    private final ServiceKey<GetBalances.Request> skGetBalances;
    @NonNull
    private final ServiceKey<GetTrackedAddresses.Request> skGetTrackedAddresses;
    @NonNull
    private final ServiceKey<GetTransfers.Request> skGetTransfers;


    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Private constructor, not usable directly. `create` is to be used instead.
     */
    private ConnectorActor(@NonNull ActorContext<ConnectorActorMessage> context,
                           @NonNull String realm) {
        super(context);
        assert realm != null;

        this.realm = realm;
        logger.debug("Launching ConnectorActor in realm \"{}\"", realm);
        receptionistSubscribeCherryGardenResponseAdapter = context.messageAdapter(
                Receptionist.Listing.class,
                ReceptionistSubscribeCherryGardenResponse::new);

        // Let's cache the service keys:
        // 1. CherryGardener service keys
        skGetCurrencies = GetCurrencies.makeServiceKey(realm);
        skPingCherryGardener = PingCherryGardener.makeServiceKey(realm);
        // 2. CherryPicker service keys
        skAddTrackedAddresses = AddTrackedAddresses.makeServiceKey(realm);
        skGetBalances = GetBalances.makeServiceKey(realm);
        skGetTrackedAddresses = GetTrackedAddresses.makeServiceKey(realm);
        skGetTransfers = GetTransfers.makeServiceKey(realm);

        // On launch, we want to subscribe to Receptionist’s changes in CherryGardener (clustered) availability.
        // Each time when CherryGardener availability (by PingCherryGardener.SERVICE_KEY) changes,
        // the message ReceptionistSubscribeCherryGardenResponse is emitted.
        context.getSystem().receptionist().tell(
                Receptionist.subscribe(
                        skPingCherryGardener,
                        receptionistSubscribeCherryGardenResponseAdapter
                )
        );
    }

    public static Behavior<ConnectorActorMessage> create(@NonNull String realm) {
        assert realm != null;
        return Behaviors.setup((context) -> new ConnectorActor(context, realm));
    }

    @Override
    public Receive<ConnectorActorMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReceptionistSubscribeCherryGardenResponse.class, this::onReceptionistSubscribeCherryGardenResponse)
                .onMessage(WaitForBootCommand.class, this::onWaitForBoot)
                // GetCurrenciesCommand
                .onMessage(GetCurrenciesCommand.class, this::onGetCurrencies)
                .onMessage(GetCurrenciesCommand.ReceptionistResponse.class, this::onGetCurrenciesReceptionistResponse)
                .onMessage(
                        GetCurrenciesCommand.InternalResult.class,
                        makeMsgResultHandler(
                                GetCurrencies.Response.class,
                                GetCurrenciesCommand.Result.class))
                // GetTrackedAddresses
                .onMessage(GetTrackedAddressesCommand.class, this::onGetTrackedAddresses)
                .onMessage(GetTrackedAddressesCommand.ReceptionistResponse.class, this::onGetTrackedAddressesReceptionistResponse)
                .onMessage(
                        GetTrackedAddressesCommand.InternalResult.class,
                        makeMsgResultHandler(
                                GetTrackedAddresses.Response.class,
                                GetTrackedAddressesCommand.Result.class))
                // AddTrackedAddresses
                .onMessage(AddTrackedAddressesCommand.class, this::onAddTrackedAddresses)
                .onMessage(AddTrackedAddressesCommand.ReceptionistResponse.class, this::onAddTrackedAddressesReceptionistResponse)
                .onMessage(
                        AddTrackedAddressesCommand.InternalResult.class,
                        makeMsgResultHandler(
                                AddTrackedAddresses.Response.class,
                                AddTrackedAddressesCommand.Result.class))
                // GetBalances
                .onMessage(GetBalancesCommand.class, this::onGetBalances)
                .onMessage(GetBalancesCommand.ReceptionistResponse.class, this::onGetBalancesReceptionistResponse)
                .onMessage(
                        GetBalancesCommand.InternalResult.class,
                        makeMsgResultHandler(
                                GetBalances.Response.class,
                                GetBalancesCommand.Result.class))
                // GetTransfers
                .onMessage(GetTransfersCommand.class, this::onGetTransfers)
                .onMessage(GetTransfersCommand.ReceptionistResponse.class, this::onGetTransfersReceptionistResponse)
                .onMessage(
                        GetTransfersCommand.InternalResult.class,
                        makeMsgResultHandler(
                                GetTransfers.Response.class,
                                GetTransfersCommand.Result.class))
                .build();
    }

//    @NonNull
//    private ExecutionContextExecutor getBlockingDispatcher() {
//        return getContext().getSystem().dispatchers().lookup(DispatcherSelector.blocking());
//    }

    private Behavior<ConnectorActorMessage> onReceptionistSubscribeCherryGardenResponse(
            @NonNull ReceptionistSubscribeCherryGardenResponse msg) {
        assert msg != null;

        final Set<ActorRef<PingCherryGardener.Request>> reachableInstances =
                msg.listing.getServiceInstances(skPingCherryGardener);
        logger.debug("Received onReceptionistSubscribeCherryGardenResponse with reachable instances {}",
                reachableInstances);

        // If we received at least one CherryGarden instance, we call all those who wait for CherryGarden to boot,
        // and those are stored in `waitForBootCallersToCallback`.
        if (!reachableInstances.isEmpty()) {
            final List<ActorRef<WaitForBootCommand.BootCompleted>> waitForBootCallersToCallback;
            synchronized (this) {
                // Copying the ones to callback
                if (!waitForBootCallers.isEmpty()) {
                    final List<ActorRef<WaitForBootCommand.BootCompleted>> copy = new ArrayList<>(waitForBootCallers);
                    waitForBootCallersToCallback = Collections.unmodifiableList(copy); // TODO since Java 11: List.copyOf(waitForBootCallers)
                    waitForBootCallers.clear();
                } else {
                    waitForBootCallersToCallback = new ArrayList<>(); // TODO since Java 9: List.of()
                }
            }

            if (!waitForBootCallersToCallback.isEmpty()) {
                logger.debug("Received non-empty reachable CherryGarden instances, notifying {} WaitForBoot callers",
                        waitForBootCallersToCallback.size());
                waitForBootCallersToCallback.forEach(actor -> actor.tell(new WaitForBootCommand.BootCompleted()));
            }
        }
        return this;
    }

    private Behavior<ConnectorActorMessage> onWaitForBoot(WaitForBootCommand msg) {
        assert msg != null;
        synchronized (this) {
            waitForBootCallers.add(msg.replyTo);
        }
        return this;
    }

    /**
     * When someone (like ClientConnector) has sent the {@link GetCurrenciesCommand} message to the actor system
     * and expect it to be processed and return the result.
     */
    private Behavior<ConnectorActorMessage> onGetCurrencies(@NonNull GetCurrenciesCommand msg) {
        assert msg != null;
        logger.debug("onGetCurrencies: Received message {}", msg);

        final ActorContext<ConnectorActorMessage> context = getContext();
        final ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();
        final ServiceKey<GetCurrencies.Request> serviceKey = skGetCurrencies;

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(serviceKey, replyTo),
                // Adapt the incoming response into `GetCurrenciesCommand.ReceptionistResponse`
                (Receptionist.Listing listing, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", listing);
                    final Set<ActorRef<GetCurrencies.Request>> serviceInstances =
                            listing.getServiceInstances(serviceKey);
                    logger.debug("Service instances for {}: {}", listing.getKey(), serviceInstances);
                    return new GetCurrenciesCommand.ReceptionistResponse(
                            listing, msg.payload, msg.replyTo);
                }
        );

        return this;
    }

    /**
     * When someone (like ClientConnector) has sent the {@link GetTrackedAddressesCommand} message to the actor system
     * and expect it to be processed and return the result.
     */
    private Behavior<ConnectorActorMessage> onGetTrackedAddresses(@NonNull GetTrackedAddressesCommand msg) {
        assert msg != null;
        logger.debug("onGetTrackedAddresses: Received message {}", msg);

        final ActorContext<ConnectorActorMessage> context = getContext();
        final ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();
        final ServiceKey<GetTrackedAddresses.Request> serviceKey = skGetTrackedAddresses;

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(serviceKey, replyTo),
                // Adapt the incoming response into `GetTrackedAddressesCommand.ReceptionistResponse`
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<GetTrackedAddresses.Request>> serviceInstances =
                            response.getServiceInstances(serviceKey);
                    logger.debug("Service instances for {}: {}", response.getKey(), serviceInstances);
                    return new GetTrackedAddressesCommand.ReceptionistResponse(response, msg.payload, msg.replyTo);
                }
        );

        return this;
    }

    /**
     * When someone (like ClientConnector) has sent the {@link AddTrackedAddresses} message to the actor system
     * and expect it to be processed and return the result.
     */
    private Behavior<ConnectorActorMessage> onAddTrackedAddresses(@NonNull AddTrackedAddressesCommand msg) {
        assert msg != null;
        logger.debug("onAddTrackedAddresses: Received message {}", msg);

        final ActorContext<ConnectorActorMessage> context = getContext();
        final ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();
        final ServiceKey<AddTrackedAddresses.Request> serviceKey = skAddTrackedAddresses;

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(serviceKey, replyTo),
                // Adapt the incoming response into `AddTrackedAddressesCommand.ReceptionistResponse`
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<AddTrackedAddresses.Request>> serviceInstances =
                            response.getServiceInstances(serviceKey);
                    logger.debug("Service instances for {}: {}", response.getKey(), serviceInstances);
                    return new AddTrackedAddressesCommand.ReceptionistResponse(response, msg.payload, msg.replyTo);
                }
        );

        return this;
    }

    /**
     * When someone (like ClientConnector) has sent the {@link GetBalancesCommand} message to the actor system
     * and expect it to be processed and return the result.
     */
    private Behavior<ConnectorActorMessage> onGetBalances(@NonNull GetBalancesCommand msg) {
        assert msg != null;
        logger.debug("onGetBalances: Received message {}", msg);

        final ActorContext<ConnectorActorMessage> context = getContext();
        final ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();
        final ServiceKey<GetBalances.Request> serviceKey = skGetBalances;

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(serviceKey, replyTo),
                // Adapt the incoming response into `GetBalancesCommand.ReceptionistResponse`
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<GetBalances.Request>> serviceInstances =
                            response.getServiceInstances(serviceKey);
                    logger.debug("Service instances for {}: {}", response.getKey(), serviceInstances);
                    return new GetBalancesCommand.ReceptionistResponse(response, msg.payload, msg.replyTo);
                }
        );

        return this;
    }

    /**
     * When someone (like ClientConnector) has sent the {@link GetTransfersCommand} message to the actor system
     * and expect it to be processed and return the result.
     */
    private Behavior<ConnectorActorMessage> onGetTransfers(@NonNull GetTransfersCommand msg) {
        assert msg != null;
        logger.debug("onGetTransfers: Received message {}", msg);

        final ActorContext<ConnectorActorMessage> context = getContext();
        final ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();
        final ServiceKey<GetTransfers.Request> serviceKey = skGetTransfers;

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(serviceKey, replyTo),
                // Adapt the incoming response into `GetTransfersCommand.ReceptionistResponse`
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<GetTransfers.Request>> serviceInstances =
                            response.getServiceInstances(serviceKey);
                    logger.debug("Service instances for {}: {}", response.getKey(), serviceInstances);
                    return new GetTransfersCommand.ReceptionistResponse(response, msg.payload, msg.replyTo);
                }
        );

        return this;
    }


    private Behavior<ConnectorActorMessage> onGetCurrenciesReceptionistResponse(
            GetCurrenciesCommand.@NonNull ReceptionistResponse msg) {
        assert msg != null;

        final ActorContext<ConnectorActorMessage> context = getContext();

        final Set<ActorRef<GetCurrencies.Request>> reachableInstances =
                msg.listing.getServiceInstances(skGetCurrencies);

        logger.debug("Received onGetCurrenciesReceptionistResponse with reachable instances {}",
                reachableInstances);
        if (!reachableInstances.isEmpty()) {
            // There may be multiple instance, but we take only one, on random
            final ActorRef<GetCurrencies.Request> gclProvider = reachableInstances.iterator().next();

            context.ask(
                    GetCurrencies.Response.class,
                    gclProvider,
                    DEFAULT_CALL_TIMEOUT,
                    // Construct the outgoing message
                    (ActorRef<GetCurrencies.Response> replyTo) ->
                            new GetCurrencies.Request(replyTo, msg.payload),
                    // Adapt the incoming response
                    (GetCurrencies.Response response, Throwable throwable) -> {
                        logger.debug("Returned GetCurrencies response: {}", response);
                        return new GetCurrenciesCommand.InternalResult(response, msg.replyTo);
                    }
            );
        }
        return this;
    }

    private Behavior<ConnectorActorMessage> onGetTrackedAddressesReceptionistResponse(
            GetTrackedAddressesCommand.@NonNull ReceptionistResponse msg) {
        assert msg != null;

        final ActorContext<ConnectorActorMessage> context = getContext();

        final Set<ActorRef<GetTrackedAddresses.Request>> reachableInstances =
                msg.listing.getServiceInstances(skGetTrackedAddresses);

        logger.debug("Received onGetTrackedAddressesReceptionistResponse with reachable instances {}",
                reachableInstances);
        if (!reachableInstances.isEmpty()) {
            // There may be multiple instance, but we take only one, on random
            final ActorRef<GetTrackedAddresses.Request> gclProvider = reachableInstances.iterator().next();

            context.ask(
                    GetTrackedAddresses.Response.class,
                    gclProvider,
                    DEFAULT_CALL_TIMEOUT,
                    // Construct the outgoing message
//                    GetTrackedAddresses.GTARequest::new,
                    (ActorRef<GetTrackedAddresses.Response> replyTo) ->
                            new GetTrackedAddresses.Request(replyTo, msg.payload),
                    // Adapt the incoming response
                    (GetTrackedAddresses.Response response, Throwable throwable) -> {
                        logger.debug("Returned GetTrackedAddresses response: {}", response);
                        return new GetTrackedAddressesCommand.InternalResult(response, msg.replyTo);
                    }
            );
        }
        return this;
    }

    private Behavior<ConnectorActorMessage> onAddTrackedAddressesReceptionistResponse(
            AddTrackedAddressesCommand.@NonNull ReceptionistResponse msg) {
        assert msg != null;

        final ActorContext<ConnectorActorMessage> context = getContext();

        final Set<ActorRef<AddTrackedAddresses.Request>> reachableInstances =
                msg.listing.getServiceInstances(skAddTrackedAddresses);

        logger.debug("Received onAddTrackedAddressesReceptionistResponse with reachable instances {}",
                reachableInstances);
        if (!reachableInstances.isEmpty()) {
            // There may be multiple instance, but we take only one, on random
            final ActorRef<AddTrackedAddresses.Request> gclProvider = reachableInstances.iterator().next();

            context.ask(
                    AddTrackedAddresses.Response.class,
                    gclProvider,
                    DEFAULT_CALL_TIMEOUT,
                    // Construct the outgoing message
                    (replyTo) -> new AddTrackedAddresses.Request(replyTo, msg.payload),
                    // Adapt the incoming response
                    (AddTrackedAddresses.Response response, Throwable throwable) -> {
                        logger.debug("Returned AddTrackedAddresses response: {}", response);
                        return new AddTrackedAddressesCommand.InternalResult(response, msg.replyTo);
                    }
            );
        }
        return this;
    }

    private Behavior<ConnectorActorMessage> onGetBalancesReceptionistResponse(
            GetBalancesCommand.@NonNull ReceptionistResponse msg) {
        assert msg != null;

        final ActorContext<ConnectorActorMessage> context = getContext();

        final Set<ActorRef<GetBalances.Request>> reachableInstances =
                msg.listing.getServiceInstances(skGetBalances);

        logger.debug("Received onGetBalancesReceptionistResponse with reachable instances {}",
                reachableInstances);
        if (!reachableInstances.isEmpty()) {
            // There may be multiple instance, but we take only one, on random
            final ActorRef<GetBalances.Request> gclProvider = reachableInstances.iterator().next();

            context.ask(
                    GetBalances.Response.class,
                    gclProvider,
                    DEFAULT_CALL_TIMEOUT,
                    // Construct the outgoing message
                    (replyTo) -> new GetBalances.Request(replyTo, msg.payload),
                    // Adapt the incoming response
                    (GetBalances.Response response, Throwable throwable) -> {
                        logger.debug("Returned GetBalances response: {}", response);
                        return new GetBalancesCommand.InternalResult(response, msg.replyTo);
                    }
            );
        }
        return this;
    }

    private Behavior<ConnectorActorMessage> onGetTransfersReceptionistResponse(
            GetTransfersCommand.@NonNull ReceptionistResponse msg) {
        assert msg != null;

        final ActorContext<ConnectorActorMessage> context = getContext();

        final Set<ActorRef<GetTransfers.Request>> reachableInstances =
                msg.listing.getServiceInstances(skGetTransfers);

        logger.debug("Received onGetTransfersReceptionistResponse with reachable instances {}",
                reachableInstances);
        if (!reachableInstances.isEmpty()) {
            // There may be multiple instance, but we take only one, on random
            final ActorRef<GetTransfers.Request> gclProvider = reachableInstances.iterator().next();

            context.ask(
                    GetTransfers.Response.class,
                    gclProvider,
                    DEFAULT_CALL_TIMEOUT,
                    // Construct the outgoing message
                    (replyTo) -> new GetTransfers.Request(replyTo, msg.payload),
                    // Adapt the incoming response
                    (GetTransfers.Response response, Throwable throwable) -> {
                        logger.debug("Returned GetTransfers response: {}", response);
                        return new GetTransfersCommand.InternalResult(response, msg.replyTo);
                    }
            );
        }
        return this;
    }


    /**
     * A generic method that makes a handler for any “InternalResult” (<code>IntRes</code>)
     * for any ConnectorActorCommand.
     * This InternalResult actually contains a real response (of type <code>Res</code>) inside.
     * The generated handler takes the <code>replyTo</code> field of IntRes, and sends it a message
     * of type <code>Res</code>.
     * <p>
     * Examples:
     * Resp=GetCurrencies.Response
     * Res=GetCurrenciesCommand.Result
     * IntRes=GetCurrenciesCommand.InternalResult
     */
    private <Resp, Res, IntRes extends ConnectorActorCommandImpl.InternalResultImpl<Resp, Res>> Function<IntRes, Behavior<ConnectorActorMessage>> makeMsgResultHandler(
            Class<Resp> respClass,
            Class<Res> resClass
    ) {
        final Constructor<Res> constructor;
        try {
            constructor = resClass.getConstructor(respClass);
        } catch (NoSuchMethodException e) {
            logger.error("No constructor for {}({})", resClass, respClass);
            logger.error("Stack:", e);
            throw new RuntimeException(String.format("Failed to make Result Handler for %s(%s)", resClass, respClass));
        }

        return (final IntRes msg) -> {
            msg.replyTo.tell(constructor.newInstance(msg.response));
            return this;
        };
    }
}
