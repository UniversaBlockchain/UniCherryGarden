package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Total information about overall available sync status (for Ethereum blockchain, node, and Unicherrygarden.)
 */
public class BlockchainSyncStatus {
    /**
     * The number of the latest block known to the Ethereum node.
     */
    public final int latestBlockchainKnownBlock;

    /**
     * The number of the latest block the Ethereum node is synced to.
     * <p>
     * Normally <code>{@link #latestBlockchainSyncedBlock} <= {@link #latestBlockchainKnownBlock}</code>.
     * <p>
     * In ideal sync state, <code>{@link #latestBlockchainSyncedBlock} = {@link #latestBlockchainKnownBlock}</code>;
     * but if it momentarily lags 1 or 2 blocks behind {@link #latestBlockchainKnownBlock}, it is also okay.
     */
    public final int latestBlockchainSyncedBlock;

    /**
     * The number of the latest block the UniCherryGarden considers itself globally synced to.
     * <p>
     * Can momentarily lag some blocks behind {@link #latestBlockchainSyncedBlock}
     * or {@link #latestBlockchainKnownBlock}.
     */
    public final int latestUniCherryGardenSyncedBlock;

    /**
     * Constructor.
     */
    @JsonCreator
    public BlockchainSyncStatus(int latestBlockchainKnownBlock,
                                int latestBlockchainSyncedBlock,
                                int latestUniCherryGardenSyncedBlock) {
        assert latestBlockchainKnownBlock >= 0 : latestBlockchainKnownBlock;
        assert latestBlockchainSyncedBlock >= 0 : latestBlockchainSyncedBlock;
        assert latestUniCherryGardenSyncedBlock >= 0 : latestUniCherryGardenSyncedBlock;

        this.latestBlockchainKnownBlock = latestBlockchainKnownBlock;
        this.latestBlockchainSyncedBlock = latestBlockchainSyncedBlock;
        this.latestUniCherryGardenSyncedBlock = latestUniCherryGardenSyncedBlock;
    }

    @Override
    public String toString() {
        return String.format("BlockchainSyncStatus(%s/%s/%s)",
                latestBlockchainKnownBlock,
                latestBlockchainSyncedBlock,
                latestUniCherryGardenSyncedBlock);
    }
}
