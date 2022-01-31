package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.myodov.unicherrygarden.api.types.MinedTransfer;
import com.myodov.unicherrygarden.api.types.SystemSyncStatus;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.CommonFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import com.myodov.unicherrygarden.api.types.responseresult.SuccessPayload;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.CherryGardenResponseWithResult;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.stream.Collectors;


public class GetTransfers {
    @NonNull
    public static final ServiceKey<GetTransfers.Request> SERVICE_KEY =
            ServiceKey.create(GetTransfers.Request.class, "getTransfersService");

    public static final class GTRequestPayload
            implements RequestPayload {
        public final int confirmations;

        @Nullable
        public final String sender;

        @Nullable
        public final String receiver;

        @Nullable
        public final Integer startBlock;

        @Nullable
        public final Integer endBlock;

        @Nullable
        public final Set<String> filterCurrencyKeys;

        public final boolean includeBalances;

        @JsonCreator
        public GTRequestPayload(int confirmations,
                                @Nullable String sender,
                                @Nullable String receiver,
                                @Nullable Integer startBlock,
                                @Nullable Integer endBlock,
                                @Nullable Set<String> filterCurrencyKeys,
                                boolean includeBalances) {
            assert confirmations >= 0 : confirmations;
            assert sender == null || EthUtils.Addresses.isValidLowercasedAddress(sender) : sender;
            assert receiver == null || EthUtils.Addresses.isValidLowercasedAddress(receiver) : receiver;
            assert startBlock == null || startBlock >= 0 : startBlock;
            assert endBlock == null || endBlock >= 0 : endBlock;
            if (startBlock != null && endBlock != null) {
                assert startBlock <= endBlock : String.format("%s/%s", startBlock, endBlock);
            }

            this.confirmations = confirmations;
            this.sender = sender;
            this.receiver = receiver;
            this.startBlock = startBlock;
            this.endBlock = endBlock;
            this.filterCurrencyKeys = filterCurrencyKeys;
            this.includeBalances = includeBalances;
        }

        @Override
        public String toString() {
            return String.format("GetTransfers.GTRequestPayload(%s, %s, %s, %s, %s, %s, %s)",
                    confirmations, sender, receiver, startBlock, endBlock, filterCurrencyKeys, includeBalances);
        }
    }

    public static final class Request
            extends RequestWithReplyTo<GetTransfers.GTRequestPayload, GetTransfers.Response>
            implements CherryPickerRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<GetTransfers.Response> replyTo,
                       @NonNull GTRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static class TransfersRequestResultData implements SuccessPayload {
        /**
         * The total status of blockchain synchronization.
         */
        @NonNull
        public final SystemSyncStatus syncStatus;

        @NonNull
        public final List<MinedTransfer> transfers;

        // A (unmodifiable, for safety) map of transfers, indexed by `from`, then `to` addresses.
        final Map<String, Map<String, List<MinedTransfer>>> transfersIndexedByFromThenTo;

        // A (unmodifiable, for safety) map of transfers, indexed by `to`, then `from` addresses.
        final Map<String, Map<String, List<MinedTransfer>>> transfersIndexedByToThenFrom;

        /**
         * The balances of addresses mentioned in the query.
         */
        public final Map<String, List<GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact>> balances;


        /**
         * Constructor.
         */
        @JsonCreator
        public TransfersRequestResultData(@NonNull SystemSyncStatus syncStatus,
                                          @NonNull List<MinedTransfer> transfers,
                                          @NonNull Map<String, List<GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact>> balances) {
            assert syncStatus != null;
            assert transfers != null;
            assert balances != null;

            this.syncStatus = syncStatus;
            this.transfers = Collections.unmodifiableList(transfers);

            final Map<String, List<GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact>> tempModifiableMap =
                    balances.entrySet().stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> Collections.unmodifiableList(e.getValue())
                    ));
            this.balances = Collections.unmodifiableMap(tempModifiableMap);

            // Generate map `byFromThenTo`
            {
                final Map<String, Map<String, List<MinedTransfer>>> outerMapByFrom =
                        new HashMap<String, Map<String, List<MinedTransfer>>>();

                for (final MinedTransfer transfer : transfers) {
                    outerMapByFrom
                            .computeIfAbsent( // outerMapByTo[from]
                                    transfer.from,
                                    (key) -> new HashMap<String, List<MinedTransfer>>())
                            .computeIfAbsent( // outerMapByTo[from][to]
                                    transfer.to,
                                    (key) -> new ArrayList<MinedTransfer>())
                            .add(transfer);
                }

                transfersIndexedByFromThenTo = freezeBiIndex(outerMapByFrom);
            }
            // Generate map `byToThenFrom`
            {
                final Map<String, Map<String, List<MinedTransfer>>> outerMapByTo =
                        new HashMap<String, Map<String, List<MinedTransfer>>>();

                for (final MinedTransfer transfer : transfers) {
                    outerMapByTo
                            .computeIfAbsent( // outerMapByTo[to]
                                    transfer.to,
                                    (key) -> new HashMap<String, List<MinedTransfer>>())
                            .computeIfAbsent( // outerMapByTo[to][from]
                                    transfer.from,
                                    (key) -> new ArrayList<MinedTransfer>())
                            .add(transfer);
                }

                transfersIndexedByToThenFrom = freezeBiIndex(outerMapByTo);
            }
        }

        @Override
        public String toString() {
            return String.format("GetTransfers.TransfersRequestResult(%s, %s, %s)",
                    syncStatus, transfers, balances);
        }

        /**
         * Take modifiable maps-of-lists, turn it to unmodifiable.
         */
        @NonNull
        private static <K, T> Map<K, List<T>> freezeMonoIndex(
                @NonNull Map<K, List<T>> index
        ) {
            return Collections.unmodifiableMap(
                    index.entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey(),
                                    e -> Collections.unmodifiableList(e.getValue()))
                            )
            );
        }

        /**
         * Take modifiable map-of-maps-of-lists, turn it to unmodifiable.
         */
        @NonNull
        private static <K1, K2, T> Map<K1, Map<K2, List<T>>> freezeBiIndex(
                @NonNull Map<K1, Map<K2, List<T>>> index
        ) {
            return Collections.unmodifiableMap(
                    index.entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey(),
                                    e -> freezeMonoIndex(e.getValue()))
                            )
            );
        }

        /**
         * Get the list of all the senders of the received transfers (i.e. all the distinct values of
         * the <code>from</code> fields).
         */
        @JsonIgnore
        @NonNull
        public Set<String> getSenders() {
            return Collections.unmodifiableSet(transfersIndexedByFromThenTo.keySet());
        }

        /**
         * Get the list of all the senders of the received transfers (i.e. all the distinct values of
         * the <code>from</code> fields).
         */
        @JsonIgnore
        @NonNull
        public Set<String> getReceivers() {
            return Collections.unmodifiableSet(transfersIndexedByToThenFrom.keySet());
        }

        /**
         * Get the list of all the transfers (optionally filtered only) from <code>fromAddress</code>,
         * (optionally filtered only) to <code>toAddress</code>).
         * <p>
         * At least one of the filters must be present.
         *
         * @param fromAddress (Optional) Lowercased Ethereum address, from which the transfers are selected.
         *                    May be <code>null</code> if no filter by sender address required.
         * @param toAddress   (Optional) Lowercased Ethereum address, to which the transfers are selected.
         *                    May be <code>null</code> if no filter by receiver address required.
         */
        @NonNull
        public List<MinedTransfer> getTransfers(@Nullable String fromAddress,
                                                @Nullable String toAddress) {
            if (fromAddress == null && toAddress == null) {
                throw new RuntimeException("At least one of `fromAddress` or `toAddress` must be non-null!");
            }
            assert fromAddress == null || EthUtils.Addresses.isValidLowercasedAddress(fromAddress) : fromAddress;
            assert toAddress == null || EthUtils.Addresses.isValidLowercasedAddress(toAddress) : toAddress;

            if (fromAddress != null && toAddress == null) {
                // Search by fromAddress only
                return sortedTransfers(
                        transfersIndexedByFromThenTo
                                .getOrDefault(fromAddress, Collections.emptyMap())
                                .values()
                                .stream()
                                .flatMap(List::stream)
                                .collect(Collectors.toList())
                );
            } else if (fromAddress == null && toAddress != null) {
                // Search by toAddress only
                return sortedTransfers(
                        transfersIndexedByToThenFrom
                                .getOrDefault(toAddress, Collections.emptyMap())
                                .values()
                                .stream()
                                .flatMap(List::stream)
                                .collect(Collectors.toList())
                );
            } else { // if (fromAddress != null && toAddress != null)
                // Search by both fromAddress and toAddress
                return sortedTransfers(
                        transfersIndexedByFromThenTo
                                .getOrDefault(fromAddress, Collections.emptyMap())
                                .getOrDefault(toAddress, Collections.emptyList())
                );
            }
        }

        /**
         * Get the list of all the transfers from <code>fromAddress</code>.
         */
        public List<MinedTransfer> getTransfersFrom(@NonNull String fromAddress) {
            return getTransfers(fromAddress, null);
        }

        /**
         * Get the list of all the transfers to <code>toAddress</code>.
         */
        public List<MinedTransfer> getTransfersTo(@NonNull String toAddress) {
            return getTransfers(null, toAddress);
        }

        /**
         * Sort the transfers in a stable manner:
         * <ol>
         * <li>Smaller block numbers go earlier;</li>
         * <li>Inside a block, the transactions with smaller transaction index go earlier;</li>
         * <li>If a transaction has generated multiple ERC20 “Transfer” events (or any other EVM events),
         * the events with smaller log index go earlier.</li>
         * </ol>
         * <p>
         * Note this function doesn’t support (and currently, whole UniCherryPicker doesn’t support)
         * any so-called “internal transactions” (i.e. ETH transfers generated inside the smart contract transactions).
         */
        static List<MinedTransfer> sortedTransfers(@NonNull Collection<MinedTransfer> transfers) {
            assert transfers != null : transfers;
            return Collections.unmodifiableList(
                    transfers
                            .stream()
                            .sorted(Comparator
                                    .comparing((MinedTransfer tr) -> tr.tx.block.blockNumber)
                                    .thenComparing((MinedTransfer tr) -> tr.tx.transactionIndex)
                                    .thenComparing((MinedTransfer tr) -> tr.logIndex))
                            .collect(Collectors.toList())
            );
        }
    }

    public static class TransfersRequestResultFailure implements SpecificFailurePayload {
    }

    public static final class Response
            extends CherryGardenResponseWithResult<TransfersRequestResultData, TransfersRequestResultFailure> {
        @JsonCreator
        public Response(@Nullable TransfersRequestResultData payload,
                        @Nullable CommonFailurePayload commonFailure,
                        @Nullable TransfersRequestResultFailure specificFailure) {
            super(payload, commonFailure, specificFailure);
        }

        @NonNull
        public static Response fromCommonFailure(@NonNull CommonFailurePayload commonFailure) {
            assert commonFailure != null : commonFailure;
            return new Response(null, commonFailure, null);
        }
    }
}
