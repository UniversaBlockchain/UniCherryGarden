package com.myodov.unicherrygarden.messages.cherrygardener;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import com.myodov.unicherrygarden.api.types.SystemStatus;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;

public class GetCurrencies_ResponseTest extends AbstractJacksonSerializationTest {
    @Test
    public void testJacksonSerialization() throws IOException {
        final Currency eth = Currency.newEthCurrency();
        final Currency erc20 = Currency.newErc20Token(
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                "Universa Token",
                "UTNP",
                "UTNP comment",
                false,
                null,
                BigInteger.valueOf(70_000)
        );
        final SystemStatus systemStatus = new SystemStatus(
                Instant.ofEpochSecond(1644936903L),
                SystemStatus.Blockchain.create(
                        SystemStatus.Blockchain.SyncingData.create(14205560, 14205570),
                        SystemStatus.Blockchain.LatestBlock.create(
                                14205590,
                                30029295L,
                                3063440L,
                                BigInteger.valueOf(0x15d3c1b812L),
                                BigInteger.valueOf(0x15d3c1b813L),
                                Instant.ofEpochSecond(0x620a9050L)
                        ),
                        BigInteger.valueOf(0x3b9aca00L)
                ),
                SystemStatus.CherryPicker.create(19, 15, 13)
        );

        final ArrayList<Currency> currencies = new ArrayList<Currency>() {{
            add(eth);
            add(erc20);
        }};

        assertJsonSerialization(
                "{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"decimals\":null,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null}",
                eth,
                Currency.class
        );

        assertJsonSerialization(
                "{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"decimals\":null,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"}",
                erc20,
                Currency.class
        );

        assertJsonSerialization(
                "{\"@class\":\"com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies$CurrenciesRequestResultPayload\"," +
                        "\"systemStatus\":{" +
                        "\"actualAt\":{\"epochSecond\":1644936903,\"nano\":0}," +
                        "\"blockchain\":{\"syncingData\":{\"currentBlock\":14205560,\"highestBlock\":14205570},\"latestBlock\":{\"number\":14205590,\"gasLimit\":30029295,\"gasUsed\":3063440,\"baseFeePerGas\":\"93747001362\",\"nextBaseFeePerGas\":\"93747001363\",\"timestamp\":{\"epochSecond\":1644859472,\"nano\":0}},\"maxPriorityFeePerGas\":\"1000000000\"}," +
                        "\"cherryPicker\":{\"latestKnownBlock\":19,\"latestPartiallySyncedBlock\":15,\"latestFullySyncedBlock\":13}" +
                        "}," +
                        "\"currencies\":[" +
                        "{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"decimals\":null,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null}," +
                        "{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"decimals\":null,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"}]}",
                new GetCurrencies.CurrenciesRequestResultPayload(systemStatus, currencies),
                GetCurrencies.CurrenciesRequestResultPayload.class
        );

        assertJsonSerialization(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies$CurrenciesRequestResultPayload\"," +
                        "\"systemStatus\":{" +
                        "\"actualAt\":{\"epochSecond\":1644936903,\"nano\":0}," +
                        "\"blockchain\":{\"syncingData\":{\"currentBlock\":14205560,\"highestBlock\":14205570},\"latestBlock\":{\"number\":14205590,\"gasLimit\":30029295,\"gasUsed\":3063440,\"baseFeePerGas\":\"93747001362\",\"nextBaseFeePerGas\":\"93747001363\",\"timestamp\":{\"epochSecond\":1644859472,\"nano\":0}},\"maxPriorityFeePerGas\":\"1000000000\"}," +
                        "\"cherryPicker\":{\"latestKnownBlock\":19,\"latestPartiallySyncedBlock\":15,\"latestFullySyncedBlock\":13}" +
                        "}," +
                        "\"currencies\":[" +
                        "{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"decimals\":null,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null}," +
                        "{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"decimals\":null,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"}]}}",
                new GetCurrencies.Response(new GetCurrencies.CurrenciesRequestResultPayload(systemStatus, currencies)),
                GetCurrencies.Response.class
        );

        assertJsonSerialization(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$CancellationCompletionFailure\"}}",
                GetCurrencies.Response.fromCommonFailure(FailurePayload.CommonFailurePayload.CANCELLATION_COMPLETION_FAILURE),
                GetCurrencies.Response.class
        );

        assertJsonSerialization(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies$CurrenciesRequestResultFailure\"}}",
                new GetCurrencies.Response(new GetCurrencies.CurrenciesRequestResultFailure()),
                GetCurrencies.Response.class
        );
    }
}
