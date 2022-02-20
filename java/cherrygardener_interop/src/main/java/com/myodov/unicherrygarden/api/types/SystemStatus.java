package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigInteger;
import java.time.Instant;

/**
 * Total information about overall system status (for Ethereum blockchain, node, and UniCherryGarden.)
 */
public class SystemStatus {

    /**
     * The details about the the syncing status of Ethereum node
     * (how well the node is synced to the blockchain).
     */
    public static class Blockchain {
        /**
         * The information about the status of blockchain syncing;
         * contains the data normally available through `eth.syncing` and `eth.blockNumber` calls.
         */
        public static class SyncingData {
            /**
             * The number of the latest block the Ethereum node is synced to.
             * Analog of calling <code>eth.syncing.currentBlock</code>.
             * <p>
             * Normally <code>{@link #currentBlock} <= {@link #highestBlock}</code>.
             * <p>
             * In ideal sync state, <code>{@link #currentBlock} = {@link #highestBlock}</code>;
             * but if it momentarily lags 1 or 2 blocks behind {@link #highestBlock}, it is also okay.
             */
            public final int currentBlock;

            /**
             * The number of the latest block known to the Ethereum node (but probably not yet synced).
             * <p>
             * Analog of calling <code>eth.syncing.highestBlock</code>.
             */
            public final int highestBlock;

            @JsonCreator
            public SyncingData(int currentBlock, int highestBlock) {
                assert currentBlock >= 0 : currentBlock;
                assert highestBlock >= 0 : highestBlock;

                this.currentBlock = currentBlock;
                this.highestBlock = highestBlock;
            }

            public static SyncingData create(int currentBlock, int highestBlock) {
                return new SyncingData(currentBlock, highestBlock);
            }

            @Override
            public String toString() {
                return String.format("%s(%s, %s)",
                        getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                        currentBlock, highestBlock);
            }
        }


        /**
         * The information about/from the latest retrieved blockchain block
         * (note - if the node is not yet fully synced, its number may be different from both `syncing.currentBlock`
         * and `syncing.highestBlock`!).
         */
        public static class LatestBlock {
            /**
             * Block number in the blockchain.
             */
            public final int number; // int instead of long or BigInt saves space, but must be fixed in 1657 years.

            public final long gasLimit;

            public final long gasUsed;

            /**
             * `baseFeePerGas`, as per EIP-1559. May be null in pre-London blocks
             * (but if the Ethereum node is well synced, it won’t be null).
             */
            @Nullable
            public final BigInteger baseFeePerGas;

            /**
             * Timestamp of the block. Note that this timestamp can be different from the timestamp
             * of the whole {@link SystemStatus} object.
             */
            @NonNull
            public final Instant timestamp;

            @JsonCreator
            public LatestBlock(int number,
                               long gasLimit,
                               long gasUsed,
                               @Nullable BigInteger baseFeePerGas,
                               @NonNull Instant timestamp) {
                assert number >= 0 : number;
                assert gasLimit >= 0 : gasLimit;
                assert gasUsed >= 0 : gasUsed;
                assert baseFeePerGas == null || baseFeePerGas.compareTo(BigInteger.ZERO) >= 0 :
                        baseFeePerGas; // baseFeePerGas >= 0
                assert timestamp != null;

                this.number = number;
                this.gasLimit = gasLimit;
                this.gasUsed = gasUsed;
                this.baseFeePerGas = baseFeePerGas;
                this.timestamp = timestamp;
            }

            public static LatestBlock create(int number,
                                             long gasLimit,
                                             long gasUsed,
                                             @Nullable BigInteger baseFeePerGas,
                                             @NonNull Instant timestamp) {
                return new LatestBlock(
                        number,
                        gasLimit,
                        gasUsed,
                        baseFeePerGas,
                        timestamp);
            }

            @Override
            public String toString() {
                return String.format("%s(%s, %s, %s, %s, %s)",
                        getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                        number, gasLimit, gasUsed, baseFeePerGas, timestamp);
            }
        }

        /**
         * Details about the latest Ethereum node syncing status.
         * Cannot be <code>null</code> – we either have the data, or the whole “blockchain“ part is unavailable.
         */
        @NonNull
        public final SyncingData syncingData;

        /**
         * Details about the latest blockchain block retrieved from the Ethereum node.
         * Cannot be <code>null</code> – we either have the data, or the whole “blockchain“ part is unavailable.
         * <p>
         * If using the data from this block, please check that the block is really recent
         * (compare its number to the `syncingData.highest`).
         */
        @NonNull
        public final LatestBlock latestBlock;

        /**
         * The node’s estimating of a gas tip sufficient to ensure a transaction is mined in a timely fashion.
         */
        @NonNull
        public final BigInteger maxPriorityFeePerGas;


        @JsonCreator
        public Blockchain(@NonNull SyncingData syncingData,
                          @NonNull LatestBlock latestBlock,
                          @NonNull BigInteger maxPriorityFeePerGas) {
            assert syncingData != null;
            assert latestBlock != null;
            // maxPriorityFeePerGas >= 0
            assert maxPriorityFeePerGas != null && maxPriorityFeePerGas.compareTo(BigInteger.ZERO) >= 0
                    :
                    maxPriorityFeePerGas;

            this.syncingData = syncingData;
            this.latestBlock = latestBlock;
            this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        }

        public static Blockchain create(@NonNull SyncingData syncingData,
                                        @NonNull LatestBlock latestBlock,
                                        @NonNull BigInteger maxPriorityFeePerGas) {
            return new Blockchain(syncingData, latestBlock, maxPriorityFeePerGas);
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s)",
                    getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                    syncingData, latestBlock);
        }
    }

    /**
     * The details about the the syncing status of UniCherryPicker
     * (how well UniCherryPicker is synced to the Ethereum node).
     */
    public static class CherryPicker {
        /**
         * The number of the latest block known to UniCherryPicker (for which it has the overall block data).
         * <p>
         * Even on ideal state, can momentarily lag some blocks behind the blocks
         * from {@link SystemStatus.Blockchain}.
         */
        public final int latestKnownBlock;

        /**
         * The number of the block for which UniCherryPicker has at least some partial transactional data.
         * <p>
         * Even on ideal state, can momentarily lag some blocks behind the blocks
         * from {@link SystemStatus.Blockchain}.
         * May be lower than {@link #latestKnownBlock}.
         */
        public final int latestPartiallySyncedBlock;

        /**
         * The number of the block for which UniCherryPicker has complete transactional data.
         * <p>
         * Even on ideal state, can momentarily lag some blocks behind the blocks
         * from {@link SystemStatus.Blockchain}.
         * May be lower than {@link #latestPartiallySyncedBlock}.
         */
        public final int latestFullySyncedBlock;

        @JsonCreator
        public CherryPicker(int latestKnownBlock,
                            int latestPartiallySyncedBlock,
                            int latestFullySyncedBlock) {
            assert latestKnownBlock >= 0 : latestKnownBlock;
            assert latestPartiallySyncedBlock >= 0 : latestPartiallySyncedBlock;
            assert latestFullySyncedBlock >= latestFullySyncedBlock : latestFullySyncedBlock;
            assert latestKnownBlock >= latestPartiallySyncedBlock :
                    String.format("%s/%s", latestKnownBlock, latestPartiallySyncedBlock);
            assert latestPartiallySyncedBlock >= latestFullySyncedBlock :
                    String.format("%s/%s", latestPartiallySyncedBlock, latestFullySyncedBlock);

            this.latestKnownBlock = latestKnownBlock;
            this.latestPartiallySyncedBlock = latestPartiallySyncedBlock;
            this.latestFullySyncedBlock = latestFullySyncedBlock;
        }

        public static CherryPicker create(int latestSyncedBlock,
                                          int latestPartiallySyncedBlock,
                                          int latestFullySyncedBlock) {
            return new CherryPicker(latestSyncedBlock, latestPartiallySyncedBlock, latestFullySyncedBlock);
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s, %s)",
                    getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                    latestKnownBlock, latestPartiallySyncedBlock, latestFullySyncedBlock);
        }
    }

    /**
     * At what moment of time this data is actual (and was generated by UniCherryGarden).
     * <p>
     * Note that the time is provided by UniCherryGarden, not the local.
     */
    @NonNull
    public final Instant actualAt;


    /**
     * Details about the blockchain sync status. <code>null</code> if unavailable.
     */
    @Nullable
    public final Blockchain blockchain;

    /**
     * Details about UniCherryPicker sync status. <code>null</code> if unavailable.
     */
    @Nullable
    public final CherryPicker cherryPicker;

    /**
     * Constructor.
     */
    @JsonCreator
    public SystemStatus(@NonNull Instant actualAt,
                        @Nullable Blockchain blockchain,
                        @Nullable CherryPicker cherryPicker) {
        assert actualAt != null : actualAt;

        this.actualAt = actualAt;
        this.blockchain = blockchain;
        this.cherryPicker = cherryPicker;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s)",
                this.getClass().getSimpleName(),
                actualAt, blockchain, cherryPicker);
    }
}
