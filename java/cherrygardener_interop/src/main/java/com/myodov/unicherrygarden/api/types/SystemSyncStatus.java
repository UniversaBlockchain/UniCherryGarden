package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigInteger;
import java.time.Instant;

/**
 * Total information about overall available sync status (for Ethereum blockchain, node, and Unicherrygarden.)
 */
public class SystemSyncStatus {

    /**
     * The details about the the syncing status of Ethereum node
     * (how well the node is synced to the blockchain).
     */
    public static class Blockchain {
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
        public Blockchain(int currentBlock,
                          int highestBlock) {
            assert currentBlock >= 0 : currentBlock;
            assert highestBlock >= 0 : highestBlock;

            this.currentBlock = currentBlock;
            this.highestBlock = highestBlock;
        }

        public static Blockchain create(int currentBlock,
                                        int highestBlock) {
            return new Blockchain(currentBlock, highestBlock);
        }

        @Override
        public String toString() {
            return String.format("%s(%s, %s)",
                    getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                    highestBlock, currentBlock);
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
         * from {@link SystemSyncStatus.Blockchain}.
         */
        public final int latestKnownBlock;

        /**
         * The number of the block for which UniCherryPicker has at least some partial transactional data.
         * <p>
         * Even on ideal state, can momentarily lag some blocks behind the blocks
         * from {@link SystemSyncStatus.Blockchain}.
         * May be lower than {@link #latestKnownBlock}.
         */
        public final int latestPartiallySyncedBlock;

        /**
         * The number of the block for which UniCherryPicker has complete transactional data.
         * <p>
         * Even on ideal state, can momentarily lag some blocks behind the blocks
         * from {@link SystemSyncStatus.Blockchain}.
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
     * Various details useful to estimate the gas price for the future transactions.
     */
    public static class GasPriceData {

        @NonNull
        public final BigInteger baseFeePerGas;

        @JsonCreator
        public GasPriceData(@NonNull BigInteger baseFeePerGas) {
            assert baseFeePerGas != null : baseFeePerGas;
            assert baseFeePerGas.compareTo(BigInteger.ZERO) >= 0 : baseFeePerGas; // baseFeePerGas >= 0

            this.baseFeePerGas = baseFeePerGas;
        }

        public static GasPriceData create(@NonNull BigInteger baseFeePerGas) {
            return new GasPriceData(baseFeePerGas);
        }

        @Override
        public String toString() {
            return String.format("%s(%s)",
                    getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                    baseFeePerGas);
        }
    }

    /**
     * At what moment of time this data is actual.
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
     * Details about gas price estimations. <code>null</code> if unavailable.
     */
    @Nullable
    public final GasPriceData gasPriceData;

    /**
     * Constructor.
     */
    @JsonCreator
    public SystemSyncStatus(@NonNull Instant actualAt,
                            @Nullable Blockchain blockchain,
                            @Nullable CherryPicker cherryPicker,
                            @Nullable GasPriceData gasPriceData) {
        assert actualAt != null: actualAt;

        this.actualAt = actualAt;
        this.blockchain = blockchain;
        this.cherryPicker = cherryPicker;
        this.gasPriceData = gasPriceData;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s, %s)",
                this.getClass().getSimpleName(),
                actualAt, blockchain, cherryPicker, gasPriceData);
    }
}
