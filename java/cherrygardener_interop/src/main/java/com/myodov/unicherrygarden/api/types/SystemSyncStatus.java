package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.Nullable;

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
         * The number of the latest block the UniCherryGarden considers itself globally synced to.
         * <p>
         * Even on ideal state, can momentarily lag some blocks behind the blocks
         * from {@link SystemSyncStatus.Blockchain}.
         */
        public final int latestSyncedBlock;

        @JsonCreator
        public CherryPicker(int latestSyncedBlock) {
            assert latestSyncedBlock >= 0 : latestSyncedBlock;

            this.latestSyncedBlock = latestSyncedBlock;
        }

        public static CherryPicker create(int latestSyncedBlock) {
            return new CherryPicker(latestSyncedBlock);
        }

        @Override
        public String toString() {
            return String.format("%s(%s)",
                    getClass().getEnclosingClass().getSimpleName(), getClass().getSimpleName(),
                    latestSyncedBlock);
        }
    }


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
    public SystemSyncStatus(@Nullable Blockchain blockchain,
                            @Nullable CherryPicker cherryPicker) {
        this.blockchain = blockchain;
        this.cherryPicker = cherryPicker;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s)",
                this.getClass().getSimpleName(),
                blockchain, cherryPicker);
    }
}
