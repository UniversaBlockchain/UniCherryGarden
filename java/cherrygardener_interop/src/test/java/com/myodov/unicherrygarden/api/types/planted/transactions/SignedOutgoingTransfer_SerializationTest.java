package com.myodov.unicherrygarden.api.types.planted.transactions;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.tx.ChainIdLong;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.myodov.unicherrygarden.SampleCredentials.CRED1;
import static com.myodov.unicherrygarden.SampleCredentials.CRED2;
import static org.junit.Assert.assertEquals;


public class SignedOutgoingTransfer_SerializationTest extends AbstractJacksonSerializationTest {
    final Logger logger = LoggerFactory.getLogger(SignedOutgoingTransfer_SerializationTest.class);

    private static final BigInteger ethTransferGasLimit = EthUtils.ETH_TRANSFER_GAS_LIMIT_BIGINTEGER;
    private static final BigInteger utnpTransferGasLimit = BigInteger.valueOf(70_000);

    private static final String ethCurrencyKey = "";
    private static final String utnpCurrencyKey = "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7";

    private static final BigDecimal maxPriorityFee = new BigDecimal("1.2345E-14");
    private static final BigDecimal maxFee = new BigDecimal("6.789E-14");

    @Test
    public void testJacksonSerializationETH() throws IOException {
        // ETH
        final UnsignedOutgoingTransfer ethTransferUnsigned = new UnsignedOutgoingTransfer(
                Hex.decode("02e90380823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c0"),
                CRED1.addr.toLowerCase(),
                new BigDecimal("0.0000001"),
                ethCurrencyKey,
                ChainIdLong.ROPSTEN,
                BigInteger.valueOf(0),
                ethTransferGasLimit,
                maxPriorityFee,
                maxFee
        );

        final SignedOutgoingTransfer transferSigned = ethTransferUnsigned.sign(CRED2.privateKey);
        logger.debug("Signed ETH: {}", transferSigned);

        assertEquals(
                "02f86c0380823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c080a0e68aeff0b6de9c1b3e22d2a9586900c1731d0906af0124dae4f270eb9a737f36a05ecff433e0f5faff59fce3076c1f923b9d65deb8d98eeb503f849e457816c173",
                transferSigned.getBytesHexString()
        );
        assertEquals(
                "0x02f86c0380823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c080a0e68aeff0b6de9c1b3e22d2a9586900c1731d0906af0124dae4f270eb9a737f36a05ecff433e0f5faff59fce3076c1f923b9d65deb8d98eeb503f849e457816c173",
                transferSigned.getPublicRepresentation()
        );
        assertEquals(
                "0x3dfc62876c5a4469f002314c755786926f88eb5f1ca22400284edd84ae650c1c",
                transferSigned.getHash()
        );

        // Components

        assertEquals(
                "0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023",
                transferSigned.sender
        );
        assertEquals(
                "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
                transferSigned.receiver
        );
        assertEquals(
                new BigDecimal("0.0000001"),
                transferSigned.amount
        );
        assertEquals(
                "",
                transferSigned.currencyKey
        );
        assertEquals(
                3L,
                transferSigned.chainId
        );
        assertEquals(
                BigInteger.valueOf(0),
                transferSigned.nonce
        );
        assertEquals(
                BigInteger.valueOf(21_000),
                transferSigned.gasLimit
        );
        assertEquals(
                new BigDecimal("1.2345E-14"),
                transferSigned.maxPriorityFee
        );
        assertEquals(
                new BigDecimal("6.789E-14"),
                transferSigned.maxFee
        );

        // Serialization

        assertJsonSerialization(
                "{" +
                        "\"bytes\":\"AvhsA4CCMDmDAQkyglIIlDTh5PgF/NyTYGinYLLBe8YhNbWuhRdIdugAgMCAoOaK7/C23pwbPiLSqVhpAMFzHQkGrwEk2uTycOuac382oF7P9DPg9fr/WfzjB2wfkjudZd642Y7rUD+EnkV4FsFz\"," +
                        "\"receiver\":\"0x34e1e4f805fcdc936068a760b2c17bc62135b5ae\"," +
                        "\"amount\":\"1E-7\"," +
                        "\"currencyKey\":\"\"," +
                        "\"chainId\":3," +
                        "\"nonce\":\"0\"," +
                        "\"gasLimit\":\"21000\"," +
                        "\"maxPriorityFee\":\"1.2345E-14\"," +
                        "\"maxFee\":\"6.789E-14\"," +
                        "\"sender\":\"0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023\"," +
                        "\"hash\":\"0x3dfc62876c5a4469f002314c755786926f88eb5f1ca22400284edd84ae650c1c\"" +
                        "}",
                transferSigned,
                SignedOutgoingTransfer.class
        );
    }

    @Test
    public void testJacksonSerializationERC20() throws IOException {
        // ERC20
        final UnsignedOutgoingTransfer erc20TransferUnsigned = new UnsignedOutgoingTransfer(
                Hex.decode("02f86a01808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c0"),
                CRED1.addr.toLowerCase(),
                new BigDecimal("0.0000001"),
                utnpCurrencyKey,
                ChainIdLong.MAINNET,
                BigInteger.valueOf(0),
                utnpTransferGasLimit,
                maxPriorityFee,
                maxFee
        );
        final SignedOutgoingTransfer transferSigned = erc20TransferUnsigned.sign(CRED2.privateKey);
        logger.debug("Signed ERC20: {}", transferSigned);

        assertEquals(
                "02f8ad01808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c001a0ddfb3ff318baf80df41288a40adcbbb21d69eb814459d857ab91c6f1e91032bea05c8236981ff3f3e33b7a1b1be342ac03633fc301980068dbe38e9c9ca2cf66fe",
                transferSigned.getBytesHexString()
        );
        assertEquals(
                "0x02f8ad01808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c001a0ddfb3ff318baf80df41288a40adcbbb21d69eb814459d857ab91c6f1e91032bea05c8236981ff3f3e33b7a1b1be342ac03633fc301980068dbe38e9c9ca2cf66fe",
                transferSigned.getPublicRepresentation()
        );
        assertEquals(
                "0x959ef3149fadba69e301999f90e950e221cc51392e781b9a57d42eb4f20b57c5",
                transferSigned.getHash()
        );

        // Components

        assertEquals(
                "0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023",
                transferSigned.sender
        );
        assertEquals(
                "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
                transferSigned.receiver
        );
        assertEquals(
                new BigDecimal("0.0000001"),
                transferSigned.amount
        );
        assertEquals(
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                transferSigned.currencyKey
        );
        assertEquals(
                1L,
                transferSigned.chainId
        );
        assertEquals(
                BigInteger.valueOf(0),
                transferSigned.nonce
        );
        assertEquals(
                BigInteger.valueOf(70_000),
                transferSigned.gasLimit
        );
        assertEquals(
                new BigDecimal("1.2345E-14"),
                transferSigned.maxPriorityFee
        );
        assertEquals(
                new BigDecimal("6.789E-14"),
                transferSigned.maxFee
        );

        // Serialization

        assertJsonSerialization(
                "{" +
                        "\"bytes\":\"AvitAYCCMDmDAQkygwERcJSeMxljbiEm48C8njE0rsXhUIpGx4C4RKkFnLsAAAAAAAAAAAAAAAA04eT4Bfzck2Bop2CywXvGITW1rgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOjUpRAAwAGg3fs/8xi6+A30EoikCty7sh1p64FEWdhXq5HG8ekQMr6gXII2mB/z8+M7ehsb40KsA2M/wwGYAGjb446cnKLPZv4=\"," +
                        "\"receiver\":\"0x34e1e4f805fcdc936068a760b2c17bc62135b5ae\"," +
                        "\"amount\":\"1E-7\"," +
                        "\"currencyKey\":\"0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7\"," +
                        "\"chainId\":1," +
                        "\"nonce\":\"0\"," +
                        "\"gasLimit\":\"70000\"," +
                        "\"maxPriorityFee\":\"1.2345E-14\"," +
                        "\"maxFee\":\"6.789E-14\"," +
                        "\"sender\":\"0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023\"," +
                        "\"hash\":\"0x959ef3149fadba69e301999f90e950e221cc51392e781b9a57d42eb4f20b57c5\"" +
                        "}",
                transferSigned,
                SignedOutgoingTransfer.class
        );
    }
}
