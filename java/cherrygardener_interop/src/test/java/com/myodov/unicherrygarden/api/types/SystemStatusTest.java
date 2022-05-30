package com.myodov.unicherrygarden.api.types;

import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class SystemStatusTest {
    @Test
    public void testToString() throws IOException {
        final SystemStatus.Blockchain.SyncingData syncingData = SystemStatus.Blockchain.SyncingData.create(14205560, 14205570);
        final SystemStatus.Blockchain.LatestBlock latestBlock = SystemStatus.Blockchain.LatestBlock.create(
                14205590,
                30029295L,
                3063440L,
                BigInteger.valueOf(0x15d3c1b812L),
                BigInteger.valueOf(0x15d3c1b813L),
                Instant.ofEpochSecond(0x620a9050L)
        );
        final SystemStatus.Blockchain blockchain = SystemStatus.Blockchain.create(
                syncingData,
                latestBlock,
                BigInteger.valueOf(0x3b9aca00L)
        );
        final SystemStatus.CherryPicker cherryPicker = SystemStatus.CherryPicker.create(
                19, 15, 13);
        final SystemStatus systemStatus = new SystemStatus(
                Instant.ofEpochSecond(1644936903L),
                blockchain,
                cherryPicker
        );

        assertEquals(
                "Blockchain.SyncingData(14205560, 14205570)",
                syncingData.toString()
        );
        assertEquals(
                "Blockchain.LatestBlock(14205590, 30029295, 3063440, 93747001362, 93747001363, 2022-02-14T17:24:32Z)",
                latestBlock.toString()
        );
        assertEquals(
                "SystemStatus.Blockchain(" +
                        "Blockchain.SyncingData(14205560, 14205570), " +
                        "Blockchain.LatestBlock(14205590, 30029295, 3063440, 93747001362, 93747001363, 2022-02-14T17:24:32Z), " +
                        "1000000000" +
                        ")",
                blockchain.toString()
        );
        assertEquals(
                "SystemStatus.CherryPicker(19, 15, 13)",
                cherryPicker.toString()
        );
        assertEquals(
                "SystemStatus(" +
                        "2022-02-15T14:55:03Z, " +
                        "SystemStatus.Blockchain(" +
                        "Blockchain.SyncingData(14205560, 14205570), " +
                        "Blockchain.LatestBlock(14205590, 30029295, 3063440, 93747001362, 93747001363, 2022-02-14T17:24:32Z), " +
                        "1000000000), " +
                        "SystemStatus.CherryPicker(19, 15, 13)" +
                        ")",
                systemStatus.toString()
        );
    }
}
