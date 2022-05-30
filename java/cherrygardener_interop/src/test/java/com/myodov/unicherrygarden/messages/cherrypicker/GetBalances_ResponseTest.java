package com.myodov.unicherrygarden.messages.cherrypicker;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import com.myodov.unicherrygarden.api.types.SystemStatus;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class GetBalances_ResponseTest extends AbstractJacksonSerializationTest {
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
                        "\"@class\":\"com.myodov.unicherrygarden.messages.cherrypicker.GetBalances$BalanceRequestResultPayload\"," +
                        "\"systemStatus\":{\"actualAt\":{\"epochSecond\":1644850591,\"nano\":0}," +
                        "\"blockchain\":{\"syncingData\":{\"currentBlock\":14205560,\"highestBlock\":14205570},\"latestBlock\":{\"number\":14205550,\"gasLimit\":30135653,\"gasUsed\":16327740,\"baseFeePerGas\":\"71470304869\",\"nextBaseFeePerGas\":\"71470304870\",\"timestamp\":{\"epochSecond\":1644858896,\"nano\":0}},\"maxPriorityFeePerGas\":\"1000000000\"}," +
                        "\"cherryPicker\":{\"latestKnownBlock\":17,\"latestPartiallySyncedBlock\":13,\"latestFullySyncedBlock\":11}" +
                        "}," +
                        "\"balances\":[" +
                        "{\"currency\":{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"decimals\":null,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null},\"amount\":\"123.45\",\"blockNumber\":7328}," +
                        "{\"currency\":{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"decimals\":null,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"},\"amount\":\"456.789\",\"blockNumber\":7932}" +
                        "]}}",
                makeJson(new GetBalances.Response(
                        new GetBalances.BalanceRequestResultPayload(
                                new SystemStatus(
                                        Instant.ofEpochSecond(1644850591L),
                                        SystemStatus.Blockchain.create(
                                                SystemStatus.Blockchain.SyncingData.create(14205560, 14205570),
                                                SystemStatus.Blockchain.LatestBlock.create(
                                                        14205550,
                                                        30135653L,
                                                        16327740L,
                                                        BigInteger.valueOf(0x10a3f64e65L),
                                                        BigInteger.valueOf(0x10a3f64e66L),
                                                        Instant.ofEpochSecond(0x620a8e10L)
                                                ),
                                                BigInteger.valueOf(0x3b9aca00L)
                                        ),
                                        SystemStatus.CherryPicker.create(17, 13, 11)
                                ),
                                new ArrayList<GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact>() {{
                                    add(new GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact(eth, new BigDecimal("123.45"), 7328));
                                    add(new GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact(utnp, new BigDecimal("456.789"), 7932));
                                }})
                ))
        );

        assertEquals(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$CancellationCompletionFailure\"}}",
                makeJson(GetBalances.Response.fromCommonFailure(FailurePayload.CommonFailurePayload.CANCELLATION_COMPLETION_FAILURE))
        );

        assertEquals(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrypicker.GetBalances$BalanceRequestResultFailure\"}}",
                makeJson(new GetBalances.Response(new GetBalances.BalanceRequestResultFailure()))
        );
    }
}
