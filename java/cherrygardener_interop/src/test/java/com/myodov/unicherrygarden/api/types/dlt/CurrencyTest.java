package com.myodov.unicherrygarden.api.types.dlt;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CurrencyTest {
    static final Currency CUR_ETH = Currency.newEthCurrency();
    static final Currency CUR_UTNP = Currency.newErc20Token(
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
            "Universa Token",
            "UTNP",
            "UTNP comment",
            false,
            null,
            BigInteger.valueOf(70_000)
    );

    @Test
    public void testBasicEthBehavior() {
        assertEquals(
                "CurrencyImpl(ETH, ETH, Ether, null, verified=true, decimals=null, transferGasLimit=21000)",
                CUR_ETH.toString()
        );

        assertEquals(
                Currency.CurrencyType.ETH,
                CUR_ETH.getCurrencyType()
        );
        assertEquals(
                "",
                CUR_ETH.getKey()
        );
        assertEquals(
                "Ether",
                CUR_ETH.getName()
        );
        assertEquals(
                "ETH",
                CUR_ETH.getSymbol()
        );
        assertNull(
                CUR_ETH.getDAppAddress()
        );
        assertNull(
                CUR_ETH.getComment()
        );
    }

    @Test
    public void testBasicErc20Behavior() {
        assertEquals(
                "CurrencyImpl(ERC20, UTNP, Universa Token, 0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7, verified=false, decimals=null, transferGasLimit=70000)",
                CUR_UTNP.toString()
        );

        assertEquals(
                Currency.CurrencyType.ERC20,
                CUR_UTNP.getCurrencyType()
        );

        assertEquals(
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                CUR_UTNP.getKey()
        );
        assertEquals(
                "Universa Token",
                CUR_UTNP.getName()
        );
        assertEquals(
                "UTNP",
                CUR_UTNP.getSymbol()
        );
        assertEquals(
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                CUR_UTNP.getDAppAddress()
        );
        assertEquals(
                "UTNP comment",
                CUR_UTNP.getComment()
        );
    }
}
