package com.myodov.unicherrygarden.messages.cherrygardener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class GetCurrencies_ResponseTest {
    private static String makeJson(Object value) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(value);
    }

    @Test
    public void testJacksonSerialization() throws IOException {
        final Currency eth = Currency.newEthCurrency();
        final Currency utnp = Currency.newErc20Token(
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                "Universa Token",
                "UTNP",
                "UTNP comment",
                false,
                null
        );

        final ArrayList<Currency> currencies = new ArrayList() {{
            add(eth);
            add(utnp);
        }};

        assertEquals(
                "{\"commonFailure\":null,\"specificFailure\":null,\"payload\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies$CurrenciesRequestResultPayload\",\"currencies\":[{\"name\":\"Ether\",\"symbol\":\"ETH\",\"comment\":null,\"key\":\"\",\"currencyType\":\"ETH\",\"dappAddress\":null},{\"name\":\"Universa Token\",\"symbol\":\"UTNP\",\"comment\":\"UTNP comment\",\"key\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\",\"currencyType\":\"ERC20\",\"dappAddress\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"}]}}",
                makeJson(new GetCurrencies.Response(new GetCurrencies.CurrenciesRequestResultPayload(currencies), null, null))
        );

        assertEquals(
                "{\"commonFailure\":{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$CancellationCompletionFailure\"},\"specificFailure\":null,\"payload\":null}",
                makeJson(GetCurrencies.Response.fromCommonFailure(FailurePayload.CommonFailurePayload.CANCELLATION_COMPLETION_FAILURE))
        );

        assertEquals(
                "{\"commonFailure\":{\"@class\":\"com.myodov.unicherrygarden.api.types.responseresult.FailurePayload$NotAvailableInOfflineMode\"},\"specificFailure\":null,\"payload\":null}",
                makeJson(GetCurrencies.Response.fromCommonFailure(FailurePayload.CommonFailurePayload.NOT_AVAILABLE_IN_OFFLINE_MODE))
        );

        assertEquals(
                "{\"commonFailure\":null,\"specificFailure\":{\"@class\":\"com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies$CurrenciesRequestResultFailure\"},\"payload\":null}",
                makeJson(new GetCurrencies.Response(null, null, new GetCurrencies.CurrenciesRequestResultFailure()))
        );
    }
}
