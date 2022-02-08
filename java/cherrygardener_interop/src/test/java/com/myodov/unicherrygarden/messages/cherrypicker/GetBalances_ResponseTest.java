package com.myodov.unicherrygarden.messages.cherrypicker;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import com.myodov.unicherrygarden.api.types.SystemSyncStatus;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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

        final ArrayList<Currency> currencies = new ArrayList() {{
            add(eth);
            add(utnp);
        }};

        assertEquals(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrypicker.GetBalances$BalanceRequestResultPayload\",\"syncStatus\":{\"blockchain\":{\"currentBlock\":20,\"highestBlock\":25},\"cherryPicker\":{\"latestKnownBlock\":17,\"latestPartiallySyncedBlock\":13,\"latestFullySyncedBlock\":11}},\"balances\":[{\"currency\":{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"type\":\"ETH\",\"dAppAddress\":null},\"amount\":\"123.45\",\"blockNumber\":7328},{\"currency\":{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"},\"amount\":\"456.789\",\"blockNumber\":7932}]}}",
                makeJson(new GetBalances.Response(
                        new GetBalances.BalanceRequestResultPayload(
                                new SystemSyncStatus(
                                        new SystemSyncStatus.Blockchain(20, 25),
                                        new SystemSyncStatus.CherryPicker(17, 13, 11)),
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
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$NotAvailableInOfflineMode\"}}",
                makeJson(GetBalances.Response.fromCommonFailure(FailurePayload.CommonFailurePayload.NOT_AVAILABLE_IN_OFFLINE_MODE))
        );

        assertEquals(
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrypicker.GetBalances$BalanceRequestResultFailure\"}}",
                makeJson(new GetBalances.Response(new GetBalances.BalanceRequestResultFailure()))
        );
    }
}
