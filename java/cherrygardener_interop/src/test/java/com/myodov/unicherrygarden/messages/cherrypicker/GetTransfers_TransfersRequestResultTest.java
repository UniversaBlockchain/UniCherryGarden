package com.myodov.unicherrygarden.messages.cherrypicker;

import com.myodov.unicherrygarden.api.types.BlockchainSyncStatus;
import com.myodov.unicherrygarden.api.types.MinedTransfer;
import com.myodov.unicherrygarden.api.types.dlt.Block;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.dlt.MinedTx;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

public class GetTransfers_TransfersRequestResultTest {
    static final Currency CUR_ETH = Currency.newEthCurrency();
    static final Currency CUR_UTNP = Currency.newErc20Token(
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
            "Universa Token",
            "UTNP",
            "UTNP comment",
            false,
            null
    );


    static final GetTransfers.TransfersRequestResult SAMPLE1 = new GetTransfers.TransfersRequestResult(
            true,
            15534104,
            new ArrayList<MinedTransfer>() {{
                add(new MinedTransfer(
                        new MinedTx(
                                "0x106a3c34bc0b8c7a1446df089a25b55f01f5648ead6055ef594dadf21120ef33",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new Block(13534104, Instant.ofEpochSecond(1635808774l))
                        ),
                        "0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24",
                        "0xc1133B6Fe141AbE3e53F8f977B36BD6F14EC0940",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new BigDecimal("30017.1499999952")));
            }},
            new BlockchainSyncStatus(
                    17534104,
                    16534104,
                    15534104)
    );

    @Test
    public void testBasicMethods() {

    }
}