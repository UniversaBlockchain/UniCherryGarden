package com.myodov.unicherrygarden.messages.connector.impl;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import com.myodov.unicherrygarden.connector.api.Sender;
import com.myodov.unicherrygarden.connector.impl.SenderImpl;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.impl.types.PrivateKeyImpl;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.tx.ChainIdLong;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import static com.myodov.unicherrygarden.SampleCredentials.CRED1;
import static com.myodov.unicherrygarden.SampleCredentials.CRED2;
import static org.junit.Assert.*;


/**
 * Test the {@link SenderImpl} class.
 * <p>
 * For better usage, you should have a testnet node available; and a HOCON config file containing
 * the test-specific data to utilize it.
 * <p>
 * Using config file: `-Dconfig.file=conf/conf/config-local-specs-nogit.conf`
 * <p>
 * Example of config file:
 *
 * <pre>
 *  unicherrygarden {
 *   tests {
 *     ethereum_testnet {
 *       chain_id = 4 // Rinkeby
 *       rpc_server = "http://localhost:8548" // Rinkeby
 *       private_keys = [
 *           "f6e28eb945e0835f7ac0f0ccdaaf717f5700554e42ed21fc23580450f5243540",
 *           "24004f4f7f73ef95152ab338f04ff89fecc36529ac827aaff779e65f8669f8f5"
 *       ]
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * The first address in `unicherrygarden.tests.ethereum_testnet.private_keys` should have some non-zero
 * amount of primary currency in this network (ETH/ETHRINKEBY/RIN/whatever), typically the one provided
 * by MyEtherWallet faucet or other faucets.
 */
public class SenderImplTest {
    final Logger logger = LoggerFactory.getLogger(SenderImplTest.class);

    public static final class TestnetSettings {
        public final long chainId;
        @NonNull
        public final String rpcServer;
        @NonNull
        public final List<PrivateKey> privateKeys;

        TestnetSettings(long chainId,
                        @NonNull String rpcServer,
                        @NonNull List<String> privateKeysStr) {
            assert rpcServer != null : rpcServer;
            assert privateKeysStr != null : privateKeysStr;

            this.chainId = chainId;
            this.rpcServer = rpcServer;
            this.privateKeys = privateKeysStr
                    .stream()
                    .map(s -> new PrivateKeyImpl(Hex.decode(s)))
                    .collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return String.format(
                    "TestnetSettings(chainId=%s, rpcServer=%s, privateKeys=%s)",
                    chainId, rpcServer, privateKeys);
        }
    }

    private static final Config config = ConfigFactory.load();
    private static final TestnetSettings testnetSettings = new TestnetSettings(
            config.getLong("unicherrygarden.tests.ethereum_testnet.chain_id"),
            config.getString("unicherrygarden.tests.ethereum_testnet.rpc_server"),
            config.getStringList("unicherrygarden.tests.ethereum_testnet.private_keys")
    );


    private static final BigInteger ethTransferGasLimit = EthUtils.ETH_TRANSFER_GAS_LIMIT_BIGINTEGER;
    private static final BigInteger utnpTransferGasLimit = BigInteger.valueOf(70_000);
    private static final BigInteger usdtTransferGasLimit = BigInteger.valueOf(100_000);

    private static final int ethDecimals = 18;
    private static final int utnpDecimals = 18;
    private static final int usdtDecimals = 6;

    private static final String ethCurrencyKey = "";
    private static final String utnpCurrencyKey = "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7";
    private static final String usdtCurrencyKey = "0xdac17f958d2ee523a2206206994597c13d831ec7";

    private static final BigDecimal maxPriorityFee = new BigDecimal("1.2345E-14");
    private static final BigDecimal maxFee = new BigDecimal("6.789E-14");

    private static final SenderImpl sender = new SenderImpl();

    private static final Sender.UnsignedOutgoingTransaction txMainnetEthTo1AUnsigned =
            sender.createOutgoingTransfer(
                    null,
                    CRED1.addr,
                    ethCurrencyKey,
                    new BigDecimal("0.0000001"),
                    ethDecimals,
                    ChainIdLong.MAINNET,
                    ethTransferGasLimit,
                    BigInteger.valueOf(0),
                    maxPriorityFee,
                    maxFee
            );
    private static final Sender.UnsignedOutgoingTransaction txRinkebyEthTo1AUnsigned =
            sender.createOutgoingTransfer(
                    null,
                    CRED1.addr,
                    ethCurrencyKey,
                    new BigDecimal("0.0000001"),
                    ethDecimals,
                    ChainIdLong.RINKEBY,
                    ethTransferGasLimit,
                    BigInteger.valueOf(0),
                    maxPriorityFee,
                    maxFee
            );

    private static final Sender.UnsignedOutgoingTransaction txMainnetEthTo1BUnsigned =
            sender.createOutgoingTransfer(
                    null,
                    CRED1.addr,
                    ethCurrencyKey,
                    new BigDecimal("12931298312"),
                    null, // testing that it may be omitted for ETH
                    ChainIdLong.MAINNET,
                    ethTransferGasLimit,
                    BigInteger.valueOf(0),
                    maxPriorityFee,
                    maxFee
            );
    private static final Sender.UnsignedOutgoingTransaction txMainnetTo2AUnsigned =
            sender.createOutgoingTransfer(
                    null,
                    CRED2.addr,
                    ethCurrencyKey,
                    new BigDecimal("0.0000001"),
                    null,
                    ChainIdLong.MAINNET,
                    ethTransferGasLimit,
                    BigInteger.valueOf(0),
                    maxPriorityFee,
                    maxFee
            );
    private static final Sender.UnsignedOutgoingTransaction txMainnetTo2BUnsigned =
            sender.createOutgoingTransfer(
                    null,
                    CRED2.addr,
                    ethCurrencyKey,
                    new BigDecimal("12931298312"),
                    ethDecimals,
                    ChainIdLong.MAINNET,
                    ethTransferGasLimit,
                    BigInteger.valueOf(0),
                    maxPriorityFee,
                    maxFee
            );

    @Test
    public void testBuildSignEthTransactionMainnet() {
        {
            // Test extra methods for mainnetTxTo1AUnsigned, to be sure
            assertFalse(txMainnetEthTo1AUnsigned.isSigned());
            assertEquals(
                    "02e90180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c0",
                    txMainnetEthTo1AUnsigned.getBytesHexString()
            );
        }

        // Test the public representations to be sure they differ

        assertEquals(
                "0x02e90180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c0",
                txMainnetEthTo1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f00180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae8c29c884ee257c3a548b20000080c0",
                txMainnetEthTo1BUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02e901808230398301093282520894408a4ac0e80ba57210ea6a9ae6a9a7b687a5102385174876e80080c0",
                txMainnetTo2AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f001808230398301093282520894408a4ac0e80ba57210ea6a9ae6a9a7b687a510238c29c884ee257c3a548b20000080c0",
                txMainnetTo2BUnsigned.getPublicRepresentation()
        );

        // Now let’s sign and see the differences

        final Sender.SignedOutgoingTransaction tx1To1ASigned = txMainnetEthTo1AUnsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction tx2To1ASigned = txMainnetEthTo1AUnsigned.sign(CRED2.privateKey);
        final Sender.SignedOutgoingTransaction tx1To1BSigned = txMainnetEthTo1BUnsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction tx2To1BSigned = txMainnetEthTo1BUnsigned.sign(CRED2.privateKey);

        {
            // Test extra methods for tx1To1ASigned, to be sure
            assertTrue(tx1To1ASigned.isSigned());
            assertEquals(
                    "02f86c0180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c001a08de2ae6deebeca74289e6f88515e1d903643be945027b0b3d0cb957e092f1688a008acab33b506eb7dcc5af2ffed22291a929fe1a064a3a51060f526542a168b65",
                    tx1To1ASigned.getBytesHexString()
            );
        }

        assertEquals(
                "0x02f86c0180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c001a08de2ae6deebeca74289e6f88515e1d903643be945027b0b3d0cb957e092f1688a008acab33b506eb7dcc5af2ffed22291a929fe1a064a3a51060f526542a168b65",
                tx1To1ASigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86c0180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c001a0d1d3033a3e9e1e61a1bffe4d9a1fb838bed0e00a375ff82ea8fa5726d4e34f39a075adeac52f3c9e78dffe4b2c309bd2b3c8235aaf3b639e6e2d59e1a789454c07",
                tx2To1ASigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f8730180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae8c29c884ee257c3a548b20000080c080a08ce0e73d50ad6ae6a5f694d5fd5711a6400b5828194e435ac64ff6cf2325282da065a920ede9de15d7aafc574474677faf80857c2d8d0e9abc7e77eb1e61f0d6fd",
                tx1To1BSigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f8730180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae8c29c884ee257c3a548b20000080c080a01f80ef15c3e27382003e6ed1624cbfad236fee4e9eb6dbdd5181a70697979083a00e3180b9ff541f837e4123bb987db9b087a39c86c694e4e465beb82b3c036cd7",
                tx2To1BSigned.getPublicRepresentation()
        );

        assertEquals(
                "0x7f2bdbf5d433eb905c0ca9509afc75e9f47482deec60a336b428b5212ccf55de",
                tx1To1ASigned.getHash()
        );
        assertEquals(
                "0xa888f02c0ce638e67a19c42420c792e03ef508f17a86319fa68bacfae3a50436",
                tx2To1ASigned.getHash()
        );
        assertEquals(
                "0xb7fe5b37eed2ab40ead0e5a5be1eedf27be54058d6f3f7325d345da33964f659",
                tx1To1BSigned.getHash()
        );
        assertEquals(
                "0xbd0ff5836bff870d7ca067a20195a3e2f1b7a61af7ee758e13cf17d44bcf7630",
                tx2To1BSigned.getHash()
        );

        final Sender.SignedOutgoingTransaction tx1To2ASigned = txMainnetTo2AUnsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction tx2To2ASigned = txMainnetTo2AUnsigned.sign(CRED2.privateKey);
        final Sender.SignedOutgoingTransaction tx1To2BSigned = txMainnetTo2BUnsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction tx2To2BSigned = txMainnetTo2BUnsigned.sign(CRED2.privateKey);

        assertEquals(
                "0x02f86c01808230398301093282520894408a4ac0e80ba57210ea6a9ae6a9a7b687a5102385174876e80080c080a0301a93fcab38f33c1d80263e1b2618d2443df0fee3747b3ced18fe189929ce25a06759dcb91bef2c2c1aa93c3e1b8e91336c2efb0643101bc18553734eb6cd2a4b",
                tx1To2ASigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86c01808230398301093282520894408a4ac0e80ba57210ea6a9ae6a9a7b687a5102385174876e80080c080a065d906789ef2ab1ca9ea3687f3f245b755c3c421628fcf0cd4175ddb4005fc0ba054a8145510b72b6ace19da10c8b070b7cc90b593c4d51a6419aac8fa606ccf1e",
                tx2To2ASigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f87301808230398301093282520894408a4ac0e80ba57210ea6a9ae6a9a7b687a510238c29c884ee257c3a548b20000080c080a01102d6f5b8b4ddaae8ce05509bc8e221172658fd5b4aa85779572799b1dea5d6a05c93b2498c4ae3059dae9233367d49026b9313a10aeacda5bf00d9e9e39b6778",
                tx1To2BSigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f87301808230398301093282520894408a4ac0e80ba57210ea6a9ae6a9a7b687a510238c29c884ee257c3a548b20000080c080a08ab0bb7ce1df84f97db89b68cefc3dc6c2874afd5bdf731c7bfeb727828a05eaa03e8e0e4ce04eaa86574273376ebad1d6a76d257671e111c762991853a60be365",
                tx2To2BSigned.getPublicRepresentation()
        );

        assertEquals(
                "0x01017a64b66132db65f7169b333a9992972dfe16a68d8ccfd84eb685c8dabd51",
                tx1To2ASigned.getHash()
        );
        assertEquals(
                "0x6c7dcc0f87803c35d784c6391e67e91fe2b239618d0f347ad9286082bc918a11",
                tx2To2ASigned.getHash()
        );
        assertEquals(
                "0xb221cff386a8920f5c7c16ccd81cd7342ebddb64dfe65826a08d8be490409ac8",
                tx1To2BSigned.getHash()
        );
        assertEquals(
                "0x9932b3ab1fb610abfdf0c1a147b88974d75e9f507934696e178a36d994325b43",
                tx2To2BSigned.getHash()
        );
    }

    @Test
    public void testBuildSignEthTransactionTestnets() {
        final Sender.UnsignedOutgoingTransaction ropstenTxTo1Unsigned = sender.createOutgoingTransfer(
                null,
                CRED1.addr,
                ethCurrencyKey,
                new BigDecimal("0.0000001"),
                ethDecimals,
                ChainIdLong.ROPSTEN,
                ethTransferGasLimit,
                BigInteger.valueOf(0),
                maxPriorityFee,
                maxFee
        );
        final Sender.UnsignedOutgoingTransaction rinkebyTxTo1Unsigned = sender.createOutgoingTransfer(
                null,
                CRED1.addr,
                ethCurrencyKey,
                new BigDecimal("0.0000001"),
                ethDecimals,
                ChainIdLong.RINKEBY,
                ethTransferGasLimit,
                BigInteger.valueOf(0),
                maxPriorityFee,
                maxFee
        );

        assertEquals(
                "0x02e90380823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c0",
                ropstenTxTo1Unsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02e90480823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c0",
                rinkebyTxTo1Unsigned.getPublicRepresentation()
        );

        final Sender.SignedOutgoingTransaction ropstenTx1To1Signed = ropstenTxTo1Unsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction ropstenTx2To1Signed = ropstenTxTo1Unsigned.sign(CRED2.privateKey);
        final Sender.SignedOutgoingTransaction rinkebyTx1To1Signed = rinkebyTxTo1Unsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction rinkebyTx2To1Signed = rinkebyTxTo1Unsigned.sign(CRED2.privateKey);
        logger.debug("Created transactions:\n  {},\n  {},\n  {},\n  {}",
                ropstenTx1To1Signed, ropstenTx2To1Signed, rinkebyTx1To1Signed, rinkebyTx2To1Signed);

        assertEquals(
                "0x02f86c0380823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c001a0c745d7ba023123d358d462d6915ad1af84545a626d08d2b82df5fc5ef166d715a065f68d46a47c5035f5b3f29c6174fc920636c3aee348e38e8b764ae08180f1b3",
                ropstenTx1To1Signed.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86c0380823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c080a0e68aeff0b6de9c1b3e22d2a9586900c1731d0906af0124dae4f270eb9a737f36a05ecff433e0f5faff59fce3076c1f923b9d65deb8d98eeb503f849e457816c173",
                ropstenTx2To1Signed.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86c0480823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c080a0c07a8331b3b083ceaa4424497e21021e2b110106a6531d59ea84ca6a3444afc2a02938bb7b1df708f9eb4879dc8f8b4ef0efee23e6f18cb32301720bb0b89d468c",
                rinkebyTx1To1Signed.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86c0480823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c080a0cb1bd2302e95c701eef292d4c3da3801576f8250290ba78392d617a5b2e4c0f9a013fabd2e5eb72e780093c78c311d83ad6e41aa949d0e68e0e943480d52b851ab",
                rinkebyTx2To1Signed.getPublicRepresentation()
        );

        assertEquals(
                "0x4cce1096b288516ea7eeaf7ade61c640bdc1f483a4ca546ab2983cbaefe5dca6",
                ropstenTx1To1Signed.getHash()
        );
        assertEquals(
                "0x3dfc62876c5a4469f002314c755786926f88eb5f1ca22400284edd84ae650c1c",
                ropstenTx2To1Signed.getHash()
        );
        assertEquals(
                "0x9a01acfbf4ccb7bfd99ea28f9007669af4c972a830bb580f6e2e110a1ffeb8c5",
                rinkebyTx1To1Signed.getHash()
        );
        assertEquals(
                "0x17f9443a44e213a3f55ad2902fd64bb4f91c0e5417796833ac22282c063de013",
                rinkebyTx2To1Signed.getHash()
        );
    }

    @Test
    public void testBuildSignERC20Transaction() {
        final Sender.UnsignedOutgoingTransaction txMainnetUtnpTo1AUnsigned =
                sender.createOutgoingTransfer(
                        null,
                        CRED1.addr,
                        utnpCurrencyKey,
                        new BigDecimal("0.000001"),
                        utnpDecimals,
                        ChainIdLong.MAINNET,
                        utnpTransferGasLimit,
                        BigInteger.valueOf(0),
                        maxPriorityFee,
                        maxFee
                );
        final Sender.UnsignedOutgoingTransaction txMainnetUsdtTo1AUnsigned =
                sender.createOutgoingTransfer(
                        null,
                        CRED1.addr,
                        usdtCurrencyKey,
                        new BigDecimal("0.000001"),
                        usdtDecimals,
                        ChainIdLong.MAINNET,
                        usdtTransferGasLimit,
                        BigInteger.valueOf(1),
                        maxPriorityFee,
                        maxFee
                );
        final Sender.UnsignedOutgoingTransaction txRinkebyUtnpTo1AUnsigned =
                sender.createOutgoingTransfer(
                        null,
                        CRED1.addr,
                        utnpCurrencyKey,
                        new BigDecimal("0.000001"),
                        utnpDecimals,
                        ChainIdLong.RINKEBY,
                        utnpTransferGasLimit,
                        BigInteger.valueOf(0),
                        maxPriorityFee,
                        maxFee
                );
        final Sender.UnsignedOutgoingTransaction txRinkebyUtnpTo1BUnsigned =
                sender.createOutgoingTransfer(
                        null,
                        CRED1.addr,
                        utnpCurrencyKey,
                        new BigDecimal("12931298312"),
                        utnpDecimals,
                        ChainIdLong.RINKEBY,
                        utnpTransferGasLimit,
                        BigInteger.valueOf(0),
                        maxPriorityFee,
                        maxFee
                );

        assertEquals(
                "0x02f86a01808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c0",
                txMainnetUtnpTo1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86a010182303983010932830186a094dac17f958d2ee523a2206206994597c13d831ec780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae0000000000000000000000000000000000000000000000000000000000000001c0",
                txMainnetUsdtTo1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86a04808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c0",
                txRinkebyUtnpTo1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86a04808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000029c884ee257c3a548b200000c0",
                txRinkebyUtnpTo1BUnsigned.getPublicRepresentation()
        );

        assertEquals(
                "02f86a01808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c0",
                txMainnetUtnpTo1AUnsigned.getBytesHexString()
        );
        assertEquals(
                "02f86a010182303983010932830186a094dac17f958d2ee523a2206206994597c13d831ec780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae0000000000000000000000000000000000000000000000000000000000000001c0",
                txMainnetUsdtTo1AUnsigned.getBytesHexString()
        );
        assertEquals(
                "02f86a04808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c0",
                txRinkebyUtnpTo1AUnsigned.getBytesHexString()
        );
        assertEquals(
                "02f86a04808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000029c884ee257c3a548b200000c0",
                txRinkebyUtnpTo1BUnsigned.getBytesHexString()
        );

        // Sign and check the signed

        final Sender.SignedOutgoingTransaction txMainnetUtnp1To1AUnsigned = txMainnetUtnpTo1AUnsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction txMainnetUsdt1To1AUnsigned = txMainnetUsdtTo1AUnsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction txRinkebyUtnp1To1AUnsigned = txRinkebyUtnpTo1AUnsigned.sign(CRED1.privateKey);
        final Sender.SignedOutgoingTransaction txRinkebyUtnp1To1BUnsigned = txRinkebyUtnpTo1BUnsigned.sign(CRED1.privateKey);

        final Sender.SignedOutgoingTransaction txMainnetUtnp2To1AUnsigned = txMainnetUtnpTo1AUnsigned.sign(CRED2.privateKey);
        final Sender.SignedOutgoingTransaction txMainnetUsdt2To1AUnsigned = txMainnetUsdtTo1AUnsigned.sign(CRED2.privateKey);
        final Sender.SignedOutgoingTransaction txRinkebyUtnp2To1AUnsigned = txRinkebyUtnpTo1AUnsigned.sign(CRED2.privateKey);
        final Sender.SignedOutgoingTransaction txRinkebyUtnp2To1BUnsigned = txRinkebyUtnpTo1BUnsigned.sign(CRED2.privateKey);

        assertEquals(
                "0x02f8ad01808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c001a03ba146568d631f28370366df935e35b107522601b6e212ab6b6b79a772e288bba007d9be0351a3ee715c26f8692e07c5cbde122284519a36ce279dd335f0ce5cbc",
                txMainnetUtnp1To1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f8ad010182303983010932830186a094dac17f958d2ee523a2206206994597c13d831ec780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae0000000000000000000000000000000000000000000000000000000000000001c080a0e27f9337da23d4fe88c20df949cbfb60f39e8f2475a37a2929d313cc321bd28ea04762e68120033982dc9c81562636523aacf963dcf024fc3f17db93b073e252c1",
                txMainnetUsdt1To1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f8ad04808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c080a0d842a7ff3569dab459d1d4def13a7b1fc38417bc8f7516b0efb95ad371254cbfa02c6514a690b2e049e27f40ebaff0a2693812145c2307abbac77c6bd6b69d32fe",
                txRinkebyUtnp1To1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f8ad04808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000029c884ee257c3a548b200000c001a078149cafd0f4bd23daf5b35a71201b0941b9b79db3d981e3afc28d08cd4167eaa01239a2d99441977f82f67fd72beabb23db4b136cadfe4eebbebc61d9069d8f94",
                txRinkebyUtnp1To1BUnsigned.getPublicRepresentation()
        );

        assertEquals(
                "0x02f8ad01808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c001a0ddfb3ff318baf80df41288a40adcbbb21d69eb814459d857ab91c6f1e91032bea05c8236981ff3f3e33b7a1b1be342ac03633fc301980068dbe38e9c9ca2cf66fe",
                txMainnetUtnp2To1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f8ad010182303983010932830186a094dac17f958d2ee523a2206206994597c13d831ec780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae0000000000000000000000000000000000000000000000000000000000000001c080a052a8d5035d5fde6aebc70736e91cc3ff20e2e1d7f036166dd88c3094a55a4796a04a15f3e183cc27254b19161a5cb58e5e714ba468e0d534b30e85aef6593e933c",
                txMainnetUsdt2To1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f8ad04808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000000000000000000e8d4a51000c001a0ae33e2d2c1bf6edbc91bf8c6a30556f078c1738faf12b4dac5e94076cb7ec1eaa07f9dbba42176b0c95819cf6c992f8596186a4d011755d45df1f07814e81769d3",
                txRinkebyUtnp2To1AUnsigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f8ad04808230398301093283011170949e3319636e2126e3c0bc9e3134aec5e1508a46c780b844a9059cbb00000000000000000000000034e1e4f805fcdc936068a760b2c17bc62135b5ae000000000000000000000000000000000000000029c884ee257c3a548b200000c001a0dcb1a273023425eaad212515c19a37c490581d302d9eaa0801ddeacdb3f1680ca0204524dcff3488583b3502792f894b20a9645570a261f53a76c484c4d5cc82c7",
                txRinkebyUtnp2To1BUnsigned.getPublicRepresentation()
        );


        assertEquals(
                "0x24b423e70cbd47b4df44d011909572bc9f77f8efa6859ecaeb5b497c99ba85f7",
                txMainnetUtnp1To1AUnsigned.getHash()
        );
        assertEquals(
                "0x8fc23289e3580cf352eb290ce7717e7c711211971334d48d6f13af0f634738ed",
                txMainnetUsdt1To1AUnsigned.getHash()
        );
        assertEquals(
                "0x58e4ffd8a8f39be238acef479cc4cc1a4ec1415eeb6250fa7ccabd47fdec765d",
                txRinkebyUtnp1To1AUnsigned.getHash()
        );
        assertEquals(
                "0x4b67af764f911db97bfe3bff85a11f2425f7df8791a7b2271e7d1041cd996249",
                txRinkebyUtnp1To1BUnsigned.getHash()
        );

        assertEquals(
                "0x959ef3149fadba69e301999f90e950e221cc51392e781b9a57d42eb4f20b57c5",
                txMainnetUtnp2To1AUnsigned.getHash()
        );
        assertEquals(
                "0x3183867617e0dc39ccbf152888a066093b09de796a123394cd27497f56d9d77a",
                txMainnetUsdt2To1AUnsigned.getHash()
        );
        assertEquals(
                "0x183fdf253667833ae44d11210ff5940ad74b65eac1bfce62b5b3ca00b873c998",
                txRinkebyUtnp2To1AUnsigned.getHash()
        );
        assertEquals(
                "0x285148b5f29adfe8ebd78a1e11c5c3fdf2f04a773323334035b53ce0a44dc528",
                txRinkebyUtnp2To1BUnsigned.getHash()
        );
    }

    @Test
    public void testSignTransaction() {
        // Most of the tests for binary signing correctness are performed in
        // testBuildEthTransactionMainnet and testBuildEthTransactionTestnets;
        // Here we just double-check the other path to sign transactions.
        final Sender.SignedOutgoingTransaction mainnetTx2To1ASigned =
                sender.signTransaction(txMainnetEthTo1AUnsigned, CRED2.bytes);
        final Sender.SignedOutgoingTransaction rinkebyTx2To1ASigned =
                sender.signTransaction(txRinkebyEthTo1AUnsigned, CRED2.bytes);

        assertEquals(
                "0x02f86c0180823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c001a0d1d3033a3e9e1e61a1bffe4d9a1fb838bed0e00a375ff82ea8fa5726d4e34f39a075adeac52f3c9e78dffe4b2c309bd2b3c8235aaf3b639e6e2d59e1a789454c07",
                mainnetTx2To1ASigned.getPublicRepresentation()
        );
        assertEquals(
                "0x02f86c0480823039830109328252089434e1e4f805fcdc936068a760b2c17bc62135b5ae85174876e80080c080a0cb1bd2302e95c701eef292d4c3da3801576f8250290ba78392d617a5b2e4c0f9a013fabd2e5eb72e780093c78c311d83ad6e41aa949d0e68e0e943480d52b851ab",
                rinkebyTx2To1ASigned.getPublicRepresentation()
        );

        assertEquals(
                "0xa888f02c0ce638e67a19c42420c792e03ef508f17a86319fa68bacfae3a50436",
                mainnetTx2To1ASigned.getHash()
        );
        assertEquals(
                "0x17f9443a44e213a3f55ad2902fd64bb4f91c0e5417796833ac22282c063de013",
                rinkebyTx2To1ASigned.getHash()
        );
    }
}
