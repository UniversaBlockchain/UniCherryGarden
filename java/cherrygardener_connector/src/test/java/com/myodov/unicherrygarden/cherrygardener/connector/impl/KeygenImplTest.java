package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import com.myodov.unicherrygarden.cherrygardener.connector.api.types.PrivateKey;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class KeygenImplTest {
    final Logger logger = LoggerFactory.getLogger(KeygenImplTest.class);

    @Test
    public void testGeneratePrivateKey() {
        final PrivateKey pk = KeygenImpl.generatePrivateKey();
        logger.debug("Private key is {}; bytes is {}", pk, pk.getBytesHex());

        assertNotNull(pk);

        assertTrue(EthUtils.Addresses.isValidAddress(pk.getAddress()));
    }
}