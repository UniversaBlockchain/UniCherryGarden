package com.myodov.unicherrygarden.messages.cherrygardener;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
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

        final ArrayList<Currency> currencies = new ArrayList<Currency>() {{
            add(eth);
            add(erc20);
        }};

        assertJsonDeserialization(
                eth,
                "{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null}",
                Currency.class
        );

        assertJsonDeserialization(
                erc20,
                "{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"}",
                Currency.class
        );

        assertJsonDeserialization(
                new GetCurrencies.CurrenciesRequestResultPayload(currencies),
                "{\"@class\":\"com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies$CurrenciesRequestResultPayload\"," +
                        "\"currencies\":[" +
                        "{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null}," +
                        "{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"}]}",
                GetCurrencies.CurrenciesRequestResultPayload.class
        );

        assertJsonDeserialization(
                new GetCurrencies.Response(new GetCurrencies.CurrenciesRequestResultPayload(currencies)),
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies$CurrenciesRequestResultPayload\"," +
                        "\"currencies\":[" +
                        "{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"verified\":true,\"transferGasLimit\":\"21000\",\"type\":\"ETH\",\"dAppAddress\":null}," +
                        "{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"verified\":false,\"transferGasLimit\":\"70000\",\"type\":\"ERC20\",\"dAppAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"}]}}",
                GetCurrencies.Response.class
        );

        assertJsonDeserialization(
                GetCurrencies.Response.fromCommonFailure(FailurePayload.CommonFailurePayload.CANCELLATION_COMPLETION_FAILURE),
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$CancellationCompletionFailure\"}}",
                GetCurrencies.Response.class
        );

        assertJsonDeserialization(
                GetCurrencies.Response.fromCommonFailure(FailurePayload.CommonFailurePayload.NOT_AVAILABLE_IN_OFFLINE_MODE),
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$NotAvailableInOfflineMode\"}}",
                GetCurrencies.Response.class
        );

        assertJsonDeserialization(
                new GetCurrencies.Response(new GetCurrencies.CurrenciesRequestResultFailure()),
                "{\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies$CurrenciesRequestResultFailure\"}}",
                GetCurrencies.Response.class
        );
    }
}
