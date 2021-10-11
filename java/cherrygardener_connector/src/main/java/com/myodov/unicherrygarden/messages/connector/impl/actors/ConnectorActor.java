package com.myodov.unicherrygarden.messages.connector.impl.actors;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.japi.function.Function;
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies;
import com.myodov.unicherrygarden.messages.cherrygardener.PingCherryGardener;
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses;
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import com.myodov.unicherrygarden.messages.connector.impl.ClientConnectorImpl;
import com.myodov.unicherrygarden.messages.connector.impl.actors.messages.*;
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
    public static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(5);


    private final ActorRef<Receptionist.Listing> receptionistSubscribeCherryGardenResponseAdapter;

    private final List<ActorRef<WaitForBootCommand.BootCompleted>> waitForBootCallers = new ArrayList<>();

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Private constructor, not usable directly. `create` is to be used instead.
     */
    private ConnectorActor(@NonNull ActorContext<ConnectorActorMessage> context) {
        super(context);
        receptionistSubscribeCherryGardenResponseAdapter = context.messageAdapter(
                Receptionist.Listing.class,
                ReceptionistSubscribeCherryGardenResponse::new);

        // On launch, we want to subscribe to Receptionist’s changes in CherryGardener (clustered) availability.
        // Each time when CherryGardener availability (by PingCherryGardener.SERVICE_KEY) changes,
        // the message ReceptionistSubscribeCherryGardenResponse is emitted.
        context.getSystem().receptionist().tell(
                Receptionist.subscribe(PingCherryGardener.SERVICE_KEY, receptionistSubscribeCherryGardenResponseAdapter)
        );
    }

    public static Behavior<ConnectorActorMessage> create() {
        return Behaviors.setup(ConnectorActor::new);
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
                msg.listing.getServiceInstances(PingCherryGardener.SERVICE_KEY);
        logger.debug("Received onGetCurrenciesReceptionistResponse with reachable instances {}",
                reachableInstances);

        // If we received at least one CherryGarden instance, we call all those who wait for CherryGarden to boot,
        // and those are stored in `waitForBootCallersToCallback`.
        if (!reachableInstances.isEmpty()) {
            final List<ActorRef<WaitForBootCommand.BootCompleted>> waitForBootCallersToCallback;
            synchronized (this) {
                // Copying the ones to callback
                if (!waitForBootCallers.isEmpty()) {
                    waitForBootCallersToCallback = Collections.unmodifiableList(new ArrayList(waitForBootCallers)); // TODO since Java 11: List.copyOf(waitForBootCallers)
                    waitForBootCallers.clear();
                } else {
                    waitForBootCallersToCallback = new ArrayList(); // TODO since Java 9: List.of()
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
        final ServiceKey<GetCurrencies.Request> serviceKey = msg.getServiceKey();

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
        final ServiceKey<GetTrackedAddresses.Request> serviceKey = msg.getServiceKey();

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
        final ServiceKey<AddTrackedAddresses.Request> serviceKey = msg.getServiceKey();

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
        final ServiceKey<GetBalances.Request> serviceKey = msg.getServiceKey();

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


    private Behavior<ConnectorActorMessage> onGetCurrenciesReceptionistResponse(
            GetCurrenciesCommand.@NonNull ReceptionistResponse msg) {
        assert msg != null;

        final ActorContext<ConnectorActorMessage> context = getContext();

        final Set<ActorRef<GetCurrencies.Request>> reachableInstances =
                msg.listing.getServiceInstances(GetCurrencies.SERVICE_KEY);

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
                msg.listing.getServiceInstances(GetTrackedAddresses.SERVICE_KEY);

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
                msg.listing.getServiceInstances(AddTrackedAddresses.SERVICE_KEY);

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
                msg.listing.getServiceInstances(GetBalances.SERVICE_KEY);

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
