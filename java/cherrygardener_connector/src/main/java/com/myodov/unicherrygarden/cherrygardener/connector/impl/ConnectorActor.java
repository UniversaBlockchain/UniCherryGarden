package com.myodov.unicherrygarden.cherrygardener.connector.impl;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import com.myodov.unicherrygarden.cherrygardener.messages.GetCurrenciesList;
import com.myodov.unicherrygarden.cherrygardener.messages.PingCherryGardener;
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
public class ConnectorActor extends AbstractBehavior<ConnectorActor.Message> {
    final Logger logger = LoggerFactory.getLogger(ConnectorActor.class);

    /**
     * Default timeout for all Akka calls.
     */
    public static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Any message to {@link ConnectorActor}.
     */
    interface Message {
    }

    /**
     * Any command (typically from {@link ClientConnectorImpl})
     * for {@link ConnectorActor} to execute.
     * <p>
     * E.g. if a ClientConnector wants to send a message and ask the {@link ConnectorActor} to do something,
     * it will be a {@link Command}.
     */
    interface Command extends Message {
    }

    /**
     * Any notification (typically a reply for any message outgoing from {@link ConnectorActor})
     * for {@link ConnectorActor} to handle.
     * <p>
     * E.g. if the {@link ConnectorActor} has sent an outgoing message to some external actor, and expects a reply,
     * it will be a {@link Notification}.
     */
    interface Notification extends Message {
    }

    private static class ReceptionistSubscribeCherryGardenResponse implements Notification {
        final Receptionist.@NonNull Listing listing;

        private ReceptionistSubscribeCherryGardenResponse(Receptionist.@NonNull Listing listing) {
            assert listing != null;
            this.listing = listing;
        }
    }

    /**
     * Actor convenience command that waits for the cluster to be up
     * (the connector to finish connecting to it, and CherryGardener being available).
     */
    public static class WaitForBootCommand implements Command {

        public static class BootCompleted {
            public BootCompleted() {
            }

            @Override
            public String toString() {
                return "WaitForBootCommand.BootCompleted()";
            }
        }

        @NonNull
        public final ActorRef<WaitForBootCommand.BootCompleted> replyTo;


        public WaitForBootCommand(@NonNull ActorRef<WaitForBootCommand.BootCompleted> replyTo) {
            assert replyTo != null;
            this.replyTo = replyTo;
        }

        public String toString() {
            return "WaitForBootCommand()";
        }
    }


    /**
     * Akka API command to “list supported currencies”.
     */
    public static class ListSupportedCurrenciesCommand implements Command {
        /**
         * During ListSupportedCurrencies command execution, we ask the Receptionist
         * about available service providing this command; this class is the response adapted
         * to handle the ListSupportedCurrencies command.
         */
        private static class ReceptionistResponse implements Notification {
            final Receptionist.@NonNull Listing listing;

            @NonNull
            final ActorRef<ListSupportedCurrenciesCommand.Result> lscReplyTo;

            private ReceptionistResponse(Receptionist.@NonNull Listing listing,
                                         @NonNull ActorRef<ListSupportedCurrenciesCommand.Result> lscReplyTo) {
                assert listing != null;
                assert lscReplyTo != null;
                this.listing = listing;
                this.lscReplyTo = lscReplyTo;
            }
        }

        private static class InternalResult implements Notification {
            final GetCurrenciesList.@NonNull Response response;
            @NonNull
            final ActorRef<ListSupportedCurrenciesCommand.Result> lscReplyTo;

            private InternalResult(GetCurrenciesList.@NonNull Response response,
                                   @NonNull ActorRef<ListSupportedCurrenciesCommand.Result> lscReplyTo) {
                assert response != null;
                assert lscReplyTo != null;
                this.response = response;
                this.lscReplyTo = lscReplyTo;
            }
        }

        public static class Result {
            final GetCurrenciesList.@NonNull Response response;

            private Result(GetCurrenciesList.@NonNull Response response) {
                assert response != null;
                this.response = response;
            }
        }


        @NonNull
        public final ActorRef<ListSupportedCurrenciesCommand.Result> replyTo;

        public ListSupportedCurrenciesCommand(@NonNull ActorRef<ListSupportedCurrenciesCommand.Result> replyTo) {
            assert replyTo != null;
            this.replyTo = replyTo;
        }

        public String toString() {
            return String.format("ListSupportedCurrenciesCommand()");
        }
    }


    private final ActorRef<Receptionist.Listing> receptionistSubscribeCherryGardenResponseAdapter;

    private final List<ActorRef<WaitForBootCommand.BootCompleted>> waitForBootCallers = new ArrayList<>();

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Private constructor, not usable directly. `create` is to be used instead.
     */
    private ConnectorActor(@NonNull ActorContext<Message> context) {
        super(context);
        receptionistSubscribeCherryGardenResponseAdapter = context.messageAdapter(
                Receptionist.Listing.class,
                ReceptionistSubscribeCherryGardenResponse::new);

        // On launch, we want to subscribe to Receptionist’s changes in CherryGardener (clustered) availability.
        context.getSystem().receptionist().tell(
                Receptionist.subscribe(PingCherryGardener.SERVICE_KEY, receptionistSubscribeCherryGardenResponseAdapter)
        );
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(ConnectorActor::new);
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReceptionistSubscribeCherryGardenResponse.class, this::onReceptionistSubscribeCherryGardenResponse)
                .onMessage(WaitForBootCommand.class, this::onWaitForBoot)
                .onMessage(ListSupportedCurrenciesCommand.class, this::onListSupportedCurrencies)
                .onMessage(ListSupportedCurrenciesCommand.ReceptionistResponse.class, this::onListSupportedCurrenciesReceptionistResponse)
                .onMessage(ListSupportedCurrenciesCommand.InternalResult.class, this::onListSupportedCurrenciesResult)
                .build();
    }

//    @NonNull
//    private ExecutionContextExecutor getBlockingDispatcher() {
//        return getContext().getSystem().dispatchers().lookup(DispatcherSelector.blocking());
//    }

    private Behavior<Message> onReceptionistSubscribeCherryGardenResponse(@NonNull ReceptionistSubscribeCherryGardenResponse msg) {
        assert msg != null;

        final Set<ActorRef<PingCherryGardener.Request>> reachableInstances = msg.listing.getServiceInstances(PingCherryGardener.SERVICE_KEY);
        logger.debug("Received onListSupportedCurrenciesReceptionistResponse with reachable instances {}",
                reachableInstances);

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

    private Behavior<Message> onWaitForBoot(ConnectorActor.@NonNull WaitForBootCommand msg) {
        assert msg != null;
        synchronized (this) {
            waitForBootCallers.add(msg.replyTo);
        }
        return this;
    }

    private Behavior<Message> onListSupportedCurrencies(ConnectorActor.@NonNull ListSupportedCurrenciesCommand msg) {
        assert msg != null;
        logger.debug("onListSupportedCurrencies: Received message {}", msg);

        final ActorContext<Message> context = getContext();
        final ActorRef<Receptionist.Command> receptionist = context.getSystem().receptionist();

        context.ask(
                Receptionist.Listing.class,
                receptionist,
                DEFAULT_CALL_TIMEOUT,
                // Construct the outgoing message
                (ActorRef<Receptionist.Listing> replyTo) ->
                        Receptionist.find(GetCurrenciesList.SERVICE_KEY, replyTo),
                // Adapt the incoming response
                (Receptionist.Listing response, Throwable throwable) -> {
                    logger.debug("Returned listing response: {}", response);
                    final Set<ActorRef<GetCurrenciesList.Request>> serviceInstances = response.getServiceInstances(GetCurrenciesList.SERVICE_KEY);
                    logger.debug("Service instances for {}: {}", response.getKey(), serviceInstances);
                    return new ListSupportedCurrenciesCommand.ReceptionistResponse(response, msg.replyTo);
                }
        );

        return this;
    }

    private Behavior<Message> onListSupportedCurrenciesReceptionistResponse(ListSupportedCurrenciesCommand.@NonNull ReceptionistResponse msg) {
        assert msg != null;

        final ActorContext<Message> context = getContext();

        final Set<ActorRef<GetCurrenciesList.Request>> reachableInstances = msg.listing.getServiceInstances(GetCurrenciesList.SERVICE_KEY);

        logger.debug("Received onListSupportedCurrenciesReceptionistResponse with reachable instances {}",
                reachableInstances);
        if (!reachableInstances.isEmpty()) {
            // There may be multiple instance, but we take only one, on random
            final ActorRef<GetCurrenciesList.Request> gclProvider = reachableInstances.iterator().next();

            context.ask(
                    GetCurrenciesList.Response.class,
                    gclProvider,
                    DEFAULT_CALL_TIMEOUT,
                    // Construct the outgoing message
                    GetCurrenciesList.Request::new,
                    // Adapt the incoming response
                    (GetCurrenciesList.Response response, Throwable throwable) -> {
                        logger.debug("Returned GetCurrenciesList response: {}", response);
                        return new ListSupportedCurrenciesCommand.InternalResult(response, msg.lscReplyTo);
                    }
            );
        }
        return this;
    }

    private Behavior<Message> onListSupportedCurrenciesResult(ListSupportedCurrenciesCommand.@NonNull InternalResult msg) {
        msg.lscReplyTo.tell(new ListSupportedCurrenciesCommand.Result(msg.response));
        return this;
    }
}
