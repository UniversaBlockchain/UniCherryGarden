package com.myodov.unicherrygarden.messages.connector.impl;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import com.myodov.unicherrygarden.connector.api.Keygen;
import com.myodov.unicherrygarden.connector.impl.KeygenImpl;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.Assert.*;

public class KeygenImplTest {
    final Logger logger = LoggerFactory.getLogger(KeygenImplTest.class);

    @Test
    public void testGeneratePrivateKey() {
        {
            final PrivateKey pk = KeygenImpl._generatePrivateKey();
            logger.debug("Private key is {}; bytes is {}", pk, pk.getBytesHex());

            assertNotNull(pk);

            assertTrue("Private key created using a static method.",
                    EthUtils.Addresses.isValidLowercasedAddress(pk.getAddress()));
        }

        {
            final PrivateKey pk = new KeygenImpl().generatePrivateKey();
            logger.debug("Private key is {}; bytes is {}", pk, pk.getBytesHex());

            assertNotNull(pk);

            assertTrue("Private key created using a class method.",
                    EthUtils.Addresses.isValidLowercasedAddress(pk.getAddress()));
        }
    }

    @Test
    public void testGeneratePrivateKeyInTryWithResources() {
        final Keygen keygen = new KeygenImpl();

        {  // private key generation
            final PrivateKey pkCopy;
            final String addressCopy;
            final byte[] bytesCopy;
            final String bytesHexCopy;

            // Using via try-with-resources
            try (final PrivateKey pk = keygen.generatePrivateKey()) {
                final PrivateKey pk2 = keygen.generatePrivateKey();

                assertNotEquals("Key generator generates different private keys",
                        pk2.toString(),
                        pk.toString()
                );

                assertNotEquals("Key generator generates private keys with different addresses",
                        pk2.getAddress(),
                        pk.getAddress()
                );

                assertNotEquals("Key generator generates private keys with different addresses (in EIP55 form)",
                        pk2.getAddressEip55(),
                        pk.getAddressEip55()
                );

                assertFalse("Key generator generates private keys with different bytes",
                        Arrays.equals(pk2.getBytes(), pk.getBytes())
                );

                assertNotEquals("Key generator generates private keys with different bytes hex representation",
                        pk2.getBytesHex(),
                        pk.getBytesHex()
                );

                // the object leaks outside of try-with-resources zone;
                // for now it still has the private key inside. Will the private key survive leaving
                // the try-with-resources zone?
                pkCopy = pk;
                addressCopy = pk.getAddress();
                bytesCopy = pk.getBytes().clone();
                bytesHexCopy = pk.getBytesHex();

                assertEquals("Still equal: toString",
                        pk.toString(),
                        pkCopy.toString()
                );

                assertEquals("Still equal: getAddress",
                        pk.getAddress(),
                        pkCopy.getAddress()
                );
                assertEquals("Still equal: getAddress",
                        addressCopy,
                        pkCopy.getAddress()
                );

                assertEquals("Still equal: getAddressEip55",
                        pk.getAddressEip55(),
                        pkCopy.getAddressEip55()
                );

                assertArrayEquals("Still equal: getBytes",
                        pk.getBytes(),
                        pkCopy.getBytes()
                );
                assertArrayEquals("Still equal: getBytes",
                        bytesCopy,
                        pkCopy.getBytes()
                );

                assertEquals("Still equal: getBytesHex",
                        pk.getBytesHex(),
                        pkCopy.getBytesHex()
                );
                assertEquals("Still equal: getBytesHex",
                        bytesHexCopy,
                        pkCopy.getBytesHex()
                );
            }

            // We are leaving the try-with-resources zone so all the private key contents is no more valid

            assertNotEquals("Private key contents is wiped on exiting: getAddress",
                    addressCopy,
                    pkCopy.getAddress()
            );

            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(bytesCopy, pkCopy.getBytes())
            );

            assertNotEquals("Private key contents is wiped on exiting: getBytesHex",
                    bytesHexCopy,
                    pkCopy.getBytesHex()
            );
        }
    }
}
