package com.myodov.unicherrygarden.messages.cherrypicker;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import com.myodov.unicherrygarden.api.types.MinedTransfer;
import com.myodov.unicherrygarden.api.types.SystemStatus;
import com.myodov.unicherrygarden.api.types.dlt.Block;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.dlt.MinedTx;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GetTransfers_ResponseTest extends AbstractJacksonSerializationTest {
    @Test
    public void testJacksonSerialization() throws IOException {
        final Currency eth = Currency.newEthCurrency();
        final Currency utnp = Currency.newErc20Token(
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                "Universa Token",
                "UTNP",
                "UTNP comment",
                false,
                null,
                BigInteger.valueOf(70_000)
        );

        final ArrayList<Currency> currencies = new ArrayList<Currency>() {{
            add(eth);
            add(utnp);
        }};

        assertEquals(
                "{\"payload\":{" +
                        "\"@class\":\"com.myodov.unicherrygarden.messages.cherrypicker.GetTransfers$TransfersRequestResultPayload\"," +
                        "\"systemStatus\":{\"actualAt\":{\"epochSecond\":1644848996,\"nano\":0}," +
                        "\"blockchain\":{\"syncingData\":{\"currentBlock\":14205560,\"highestBlock\":14205570},\"latestBlock\":{\"number\":14205545,\"gasLimit\":30087829,\"gasUsed\":4802463,\"baseFeePerGas\":\"81749786720\",\"timestamp\":{\"epochSecond\":1644858859,\"nano\":0}},\"maxPriorityFeePerGas\":\"1500000000\"}," +
                        "\"cherryPicker\":{\"latestKnownBlock\":17,\"latestPartiallySyncedBlock\":13,\"latestFullySyncedBlock\":11}" +
                        "}," +
                        "\"transfers\":[" +
                        "{\"from\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24\",\"to\":\"0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688\",\"currencyKey\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\",\"amount\":\"200000\",\"tx\":{\"txhash\":\"0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a\",\"from\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24\",\"to\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\",\"block\":{\"blockNumber\":13550555,\"hash\":\"0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a\",\"ts\":{\"epochSecond\":1636033366,\"nano\":0}},\"transactionIndex\":220,\"fees\":\"0.00455845\"},\"logIndex\":258}," +
                        "{\"from\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24\",\"to\":\"0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688\",\"currencyKey\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\",\"amount\":\"379912.034000027\",\"tx\":{\"txhash\":\"0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7\",\"from\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24\",\"to\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\",\"block\":{\"blockNumber\":13550616,\"hash\":\"0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7\",\"ts\":{\"epochSecond\":1636034174,\"nano\":0}},\"transactionIndex\":204,\"fees\":\"0.00456157\"},\"logIndex\":305}" +
                        "]," +
                        "\"balances\":{" +
                        "\"\":[" +
                        "{\"currency\":{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"decimals\":null,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null},\"amount\":\"123.45\",\"blockNumber\":7328}," +
                        "{\"currency\":{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"decimals\":null,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null},\"amount\":\"67.89\",\"blockNumber\":7751}]," +
                        "\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\":[" +
                        "{\"currency\":{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"decimals\":null,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"},\"amount\":\"1.23E-12\",\"blockNumber\":123}," +
                        "{\"currency\":{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"decimals\":null,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"},\"amount\":\"238954723985723982932342342342335.23\",\"blockNumber\":458392932}" +
                        "]}}}",
                makeJson(new GetTransfers.Response(
                        new GetTransfers.TransfersRequestResultPayload(
                                new SystemStatus(
                                        Instant.ofEpochSecond(1644848996L),
                                        SystemStatus.Blockchain.create(
                                                SystemStatus.Blockchain.SyncingData.create(14205560, 14205570),
                                                SystemStatus.Blockchain.LatestBlock.create(
                                                        14205545,
                                                        30087829L,
                                                        4802463L,
                                                        BigInteger.valueOf(0x1308aac060L),
                                                        Instant.ofEpochSecond(0x620a8debL)
                                                ),
                                                BigInteger.valueOf(0x59682f00L)
                                        ),
                                        SystemStatus.CherryPicker.create(17, 13, 11)
                                ),
                                new ArrayList<MinedTransfer>() {{
                                    add(new MinedTransfer(
                                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                            new BigDecimal("200000"),
                                            new MinedTx(
                                                    "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
                                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                                    new Block(
                                                            13550555,
                                                            "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                                                            Instant.ofEpochSecond(1636033366L)),
                                                    220,
                                                    new BigDecimal("0.00455845")
                                            ),
                                            258));
                                    // UTNP out #3 (same addr, transfer 2 of 3)
                                    add(new MinedTransfer(
                                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                            new BigDecimal("379912.034000027"),
                                            new MinedTx(
                                                    "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
                                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                                    new Block(
                                                            13550616,
                                                            "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                                                            Instant.ofEpochSecond(1636034174L)),
                                                    204,
                                                    new BigDecimal("0.00456157")
                                            ),
                                            305));
                                }},
                                new HashMap<String, List<GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact>>() {{
                                    put("", new ArrayList<GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact>() {{
                                        add(new GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact(eth, new BigDecimal("123.45"), 7328));
                                        add(new GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact(eth, new BigDecimal("67.89"), 7751));
                                    }});
                                    put("0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7", new ArrayList<GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact>() {{
                                        add(new GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact(utnp, new BigDecimal("0.00000000000123"), 123));
                                        add(new GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact(utnp, new BigDecimal("238954723985723982932342342342335.23"), 458392932));
                                    }});
                                }}
                        )
                ))
        );

        assertEquals(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$CancellationCompletionFailure\"}}",
                makeJson(GetTransfers.Response.fromCommonFailure(FailurePayload.CommonFailurePayload.CANCELLATION_COMPLETION_FAILURE))
        );

        assertEquals(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrypicker.GetTransfers$TransfersRequestResultFailure\"}}",
                makeJson(new GetTransfers.Response(new GetTransfers.TransfersRequestResultFailure()))
        );
    }
}
