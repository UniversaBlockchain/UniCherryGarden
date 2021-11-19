package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.BlockchainSyncStatus;
import com.myodov.unicherrygarden.api.types.MinedTransfer;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.CherryPickerResponse;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.stream.Collectors;


public class GetTransfers {
    @NonNull
    public static final ServiceKey<GetBalances.Request> SERVICE_KEY =
            ServiceKey.create(GetBalances.Request.class, "getTransfersService");

    public static final class GTRequestPayload
            implements RequestPayload {

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


    public static class TransfersRequestResult {
        /**
         * Whether or not the transfers (stored in {@link #transfers}) have been retrieved,
         * at least partially, at least non-fully-synced. “Overall success” of getting the list of transfers.
         * <p>
         * If {@link #overallSuccess} is <code>false</code>, we should assume the result retrieval
         * failed completely, and {@link #transfers} will likely have no records at all.
         * <p>
         * There also may be some partial-fails (like, only the transfers for some specific token failed);
         * in this case, the balance will have {@link TransfersRequestResult::TransfersSyncState#NON_SYNCED} state.
         */
        public final boolean overallSuccess;

        /**
         * The number of the block for which the results have been provided.
         */
        public final int resultAtBlock;

        @NonNull
        public final List<MinedTransfer> transfers;

        /**
         * The total status of blockchain synchronization.
         */
        @NonNull
        public final BlockchainSyncStatus syncStatus;

        // A (unmodifiable, for safety) map of transfers, indexed by `from`, then `to` addresses.
        final Map<String, Map<String, List<MinedTransfer>>> transfersIndexedByFromThenTo;

        // A (unmodifiable, for safety) map of transfers, indexed by `to`, then `from` addresses.
        final Map<String, Map<String, List<MinedTransfer>>> transfersIndexedByToThenFrom;


        /**
         * Constructor.
         */
        @JsonCreator
        public TransfersRequestResult(boolean overallSuccess,
                                      int resultAtBlock,
                                      @NonNull List<MinedTransfer> transfers,
                                      @NonNull BlockchainSyncStatus syncStatus) {
            assert transfers != null;
            assert syncStatus != null;

            this.overallSuccess = overallSuccess;
            this.resultAtBlock = resultAtBlock;
            this.transfers = Collections.unmodifiableList(transfers);

            this.syncStatus = syncStatus;

            // Now generate convenient mappings by other keys
//            byFromThenTo = Collections.unmodifiableMap(new HashMap<String, Map<String, MinedTransfer>>() {{
//
//            }});

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
        public Set<String> getSenders() {
            return Collections.unmodifiableSet(transfersIndexedByFromThenTo.keySet());
        }

        /**
         * Get the list of all the senders of the received transfers (i.e. all the distinct values of
         * the <code>from</code> fields).
         */
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


    public static final class Response
            implements CherryPickerResponse {
        @NonNull
        public final TransfersRequestResult result;

        @JsonCreator
        public Response(@NonNull TransfersRequestResult result) {
            this.result = result;
        }

        @Override
        public String toString() {
            return String.format("GetTransfers.Response(%s)", result);
        }
    }
}
