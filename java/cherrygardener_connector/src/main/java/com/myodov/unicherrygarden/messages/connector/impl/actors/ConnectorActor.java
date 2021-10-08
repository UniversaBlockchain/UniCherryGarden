package com.myodov.unicherrygarden.messages.connector.impl.actors;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
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
                .onMessage(GetCurrenciesCommand.InternalResult.class, this::onGetCurrenciesResult)
                // GetTrackedAddresses
                .onMessage(GetTrackedAddressesCommand.class, this::onGetTrackedAddresses)
                .onMessage(GetTrackedAddressesCommand.ReceptionistResponse.class, this::onGetTrackedAddressesReceptionistResponse)
                .onMessage(GetTrackedAddressesCommand.InternalResult.class, this::onGetTrackedAddressesResult)
                // AddTrackedAddresses
                .onMessage(AddTrackedAddressesCommand.class, this::onAddTrackedAddresses)
                .onMessage(AddTrackedAddressesCommand.ReceptionistResponse.class, this::onAddTrackedAddressesReceptionistResponse)
                .onMessage(AddTrackedAddressesCommand.ATAInternalResult.class, this::onAddTrackedAddressesResult)
                // GetBalances
                .onMessage(GetBalancesCommand.class, this::onGetBalances)
                .onMessage(GetBalancesCommand.ReceptionistResponse.class, this::onGetBalancesReceptionistResponse)
                .onMessage(GetBalancesCommand.InternalResult.class, this::onGetBalancesResult)
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

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(GetCurrencies.SERVICE_KEY, replyTo),
                // Adapt the incoming response into `GetCurrenciesCommand.ReceptionistResponse`
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<GetCurrencies.Request>> serviceInstances =
                            response.getServiceInstances(GetCurrencies.SERVICE_KEY);
                    logger.debug("Service instances for {}: {}", response.getKey(), serviceInstances);
                    return new GetCurrenciesCommand.ReceptionistResponse(response, msg.payload, msg.replyTo);
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

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(GetTrackedAddresses.SERVICE_KEY, replyTo),
                // Adapt the incoming response into `GetTrackedAddressesCommand.ReceptionistResponse`
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<GetTrackedAddresses.Request>> serviceInstances =
                            response.getServiceInstances(GetTrackedAddresses.SERVICE_KEY);
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

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(AddTrackedAddresses.SERVICE_KEY, replyTo),
                // Adapt the incoming response into `AddTrackedAddressesCommand.ReceptionistResponse`
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<AddTrackedAddresses.Request>> serviceInstances =
                            response.getServiceInstances(AddTrackedAddresses.SERVICE_KEY);
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

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(GetBalances.SERVICE_KEY, replyTo),
                // Adapt the incoming response into `GetBalancesCommand.ReceptionistResponse`
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<GetBalances.Request>> serviceInstances =
                            response.getServiceInstances(GetBalances.SERVICE_KEY);
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
                        return new GetCurrenciesCommand.InternalResult(response, msg.gcReplyTo);
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
                        return new GetTrackedAddressesCommand.InternalResult(response, msg.gtaReplyTo);
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
                        return new AddTrackedAddressesCommand.ATAInternalResult(response, msg.ataReplyTo);
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
                        return new GetBalancesCommand.InternalResult(response, msg.gbReplyTo);
                    }
            );
        }
        return this;
    }

    private Behavior<ConnectorActorMessage> onGetCurrenciesResult(GetCurrenciesCommand.@NonNull InternalResult msg) {
        msg.gcReplyTo.tell(new GetCurrenciesCommand.Result(msg.response));
        return this;
    }

    private Behavior<ConnectorActorMessage> onGetTrackedAddressesResult(GetTrackedAddressesCommand.@NonNull InternalResult msg) {
        msg.gtaReplyTo.tell(new GetTrackedAddressesCommand.Result(msg.response));
        return this;
    }

    private Behavior<ConnectorActorMessage> onAddTrackedAddressesResult(AddTrackedAddressesCommand.ATAInternalResult msg) {
        msg.ataReplyTo.tell(new AddTrackedAddressesCommand.Result(msg.response));
        return this;
    }

    private Behavior<ConnectorActorMessage> onGetBalancesResult(GetBalancesCommand.@NonNull InternalResult msg) {
        msg.gbReplyTo.tell(new GetBalancesCommand.Result(msg.response));
        return this;
    }

}
