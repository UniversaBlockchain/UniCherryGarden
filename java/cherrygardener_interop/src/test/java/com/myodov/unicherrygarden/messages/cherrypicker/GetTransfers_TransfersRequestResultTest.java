package com.myodov.unicherrygarden.messages.cherrypicker;

import com.myodov.unicherrygarden.api.types.MinedTransfer;
import com.myodov.unicherrygarden.api.types.SystemSyncStatus;
import com.myodov.unicherrygarden.api.types.dlt.Block;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.api.types.dlt.MinedTx;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class GetTransfers_TransfersRequestResultTest {
    static final Currency CUR_ETH = Currency.newEthCurrency();
    static final Currency CUR_UTNP = Currency.newErc20Token(
            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
            "Universa Token",
            "UTNP",
            "UTNP comment",
            false,
            null
    );


    // This sample contains a subset of real data related to address 0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24,
    // aka “Universa UTNP token hot-swap wallet”.
    // Contains a variety of transfers such as:
    // 1. Transfer-in of UTNP token (once)
    // 2. Transfer-outs of UTNP tokens (several)
    // 2.1. Including three transfers of UTNP tokens to the same address.
    // 3. Transfer-in of ETH (several).
    static final GetTransfers.TransfersRequestResult SAMPLE1 = new GetTransfers.TransfersRequestResult(
            new SystemSyncStatus(
                    new SystemSyncStatus.Blockchain(15631007, 14631007),
                    new SystemSyncStatus.CherryPicker(13631007)
            ),
            new ArrayList<MinedTransfer>() {{
                // UTNP in #1
                add(new MinedTransfer(
                        "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new BigDecimal("275000"),
                        new MinedTx(
                                "0xe960770d6dbbb14c01567490fca7ac6b4f4fff7ace21a52e466e60dfd89a7fe9",
                                "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new Block(
                                        12088227,
                                        "0x3e56ce3b76fc22ab36d0d9527d1b5669f90c625228a6e81303bf7d1ed57f76bd",
                                        Instant.ofEpochSecond(1616410837)),
                                130
                        ),
                        107));
                // ETH in #1
                add(new MinedTransfer(
                        "0x956aa9fec25a346009edaaabf378f94d81128b15",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "",
                        new BigDecimal("0.09"),
                        new MinedTx(
                                "0x77fd81f138e22d6b06954a0e863598547f865c9584f94497148985cace0e6b1c",
                                "0x956aa9fec25a346009edaaabf378f94d81128b15",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                new Block(
                                        13315889,
                                        "0x3ab6c594478ed4ccf5bec6ba63eecc5f03826f3677a865fb77737cd09d5ed9be",
                                        Instant.ofEpochSecond(1632853922)),
                                381
                        ),
                        null));
                // ETH in #2
                add(new MinedTransfer(
                        "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "",
                        new BigDecimal("0.2"),
                        new MinedTx(
                                "0x848b45ffe1cefe1f8f75fd413e308157443c5fc16f54049164ba1ee67bf01c16",
                                "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                new Block(
                                        13482556,
                                        "0x318d8b71a5a4950034815c1d625aac7f67dc9bd2713a43704163ccecc130bbb8",
                                        Instant.ofEpochSecond(1635111142)),
                                17
                        ),
                        null));
                // UTNP out #1
                add(new MinedTransfer(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x6cf94eed6f7025088a909844ac73b037335604e6",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new BigDecimal("99910.067000013"),
                        new MinedTx(
                                "0xd67d7bc775e3cbd052bb94ebf1744cbbaa3663d2a9208423baa129652dc1754e",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new Block(
                                        13537227,
                                        "0x7fe32b0d67fc180534870ac95a406e588e71f951f8ac9859a7cfb41f25022ce0",
                                        Instant.ofEpochSecond(1635852034)),
                                132
                        ),
                        126));
                // Some token in #1
                add(new MinedTransfer(
                        "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                        new BigDecimal("300000"),
                        new MinedTx(
                                "0x97413f924214bd2e6a196ef75909fa7ac6b065078c5b422c818806ba58620ab3",
                                "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                                "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                                new Block(
                                        13548591,
                                        "0x26dc66b6d13aa23f79768abfb766f62de9e0e77b701811ebf7fa586fdf43acc3",
                                        Instant.ofEpochSecond(1636006619)),
                                75
                        ),
                        138));
                // UTNP out #2 (same addr, transfer 1 of 3)
                add(new MinedTransfer(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new BigDecimal("200000"),
                        new MinedTx(
                                "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new Block(
                                        13550555,
                                        "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                                        Instant.ofEpochSecond(1636033366)),
                                220
                        ),
                        258));
                // UTNP out #3 (same addr, transfer 2 of 3)
                add(new MinedTransfer(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new BigDecimal("379912.034000027"),
                        new MinedTx(
                                "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new Block(
                                        13550616,
                                        "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                                        Instant.ofEpochSecond(1636034174)),
                                204
                        ),
                        305));
                // UTNP out #4 (same addr, transfer 3 of 3)
                add(new MinedTransfer(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new BigDecimal("3420000.0000000002"),
                        new MinedTx(
                                "0x4c08150533d59ed6b013793ae6887ce5a33e703a64dcf6557883870b08e8d6eb",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new Block(
                                        13550669,
                                        "0x02f05a9b028e163872ba600b406d6701dd8506afc0e9f31fa4b2d885e9185044",
                                        Instant.ofEpochSecond(1636034986)),
                                373
                        ),
                        399));
                // UTNP out #5
                add(new MinedTransfer(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0xedcc6f8f20962e6747369a71a5b89256289da87f",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new BigDecimal("10045.6909000003"),
                        new MinedTx(
                                "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new Block(
                                        13628884,
                                        "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
                                        Instant.ofEpochSecond(1637096843)),
                                111
                        ),
                        144));
                // UTNP out #6
                add(new MinedTransfer(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x74644fd700c11dcc262eed1c59715ee874f65251",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new BigDecimal("30000"),
                        new MinedTx(
                                "0x3f0c1e4f1e903381c1e8ad2ad909482db20a747e212fbc32a4c626cad6bb14ab",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new Block(
                                        13631007,
                                        "0x57e6c79ffcbcc1d77d9d9debb1f7bbe1042e685e0d2f5bb7e7bf37df0494e096",
                                        Instant.ofEpochSecond(1637125704)),
                                133
                        ),
                        173));
            }},
            Collections.emptyMap()
    );
    static final List<MinedTransfer> SAMPLE1_UNSORTED_TRANSFERS = new ArrayList<MinedTransfer>() {{
        // UTNP out #3 (same addr, transfer 2 of 3)
        add(new MinedTransfer(
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                new BigDecimal("379912.034000027"),
                new MinedTx(
                        "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new Block(
                                13550616,
                                "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                                Instant.ofEpochSecond(1636034174)),
                        204
                ),
                305));
        // UTNP out #4 (same addr, transfer 3 of 3)
        add(new MinedTransfer(
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                new BigDecimal("3420000.0000000002"),
                new MinedTx(
                        "0x4c08150533d59ed6b013793ae6887ce5a33e703a64dcf6557883870b08e8d6eb",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new Block(
                                13550669,
                                "0x02f05a9b028e163872ba600b406d6701dd8506afc0e9f31fa4b2d885e9185044",
                                Instant.ofEpochSecond(1636034986)),
                        373
                ),
                399));
        // ETH in #1
        add(new MinedTransfer(
                "0x956aa9fec25a346009edaaabf378f94d81128b15",
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "",
                new BigDecimal("0.09"),
                new MinedTx(
                        "0x77fd81f138e22d6b06954a0e863598547f865c9584f94497148985cace0e6b1c",
                        "0x956aa9fec25a346009edaaabf378f94d81128b15",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        new Block(
                                13315889,
                                "0x3ab6c594478ed4ccf5bec6ba63eecc5f03826f3677a865fb77737cd09d5ed9be",
                                Instant.ofEpochSecond(1632853922)),
                        381
                ),
                null));
        // ETH in #2
        add(new MinedTransfer(
                "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "",
                new BigDecimal("0.2"),
                new MinedTx(
                        "0x848b45ffe1cefe1f8f75fd413e308157443c5fc16f54049164ba1ee67bf01c16",
                        "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        new Block(
                                13482556,
                                "0x318d8b71a5a4950034815c1d625aac7f67dc9bd2713a43704163ccecc130bbb8",
                                Instant.ofEpochSecond(1635111142)),
                        17
                ),
                null));
        // UTNP out #1
        add(new MinedTransfer(
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "0x6cf94eed6f7025088a909844ac73b037335604e6",
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                new BigDecimal("99910.067000013"),
                new MinedTx(
                        "0xd67d7bc775e3cbd052bb94ebf1744cbbaa3663d2a9208423baa129652dc1754e",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new Block(
                                13537227,
                                "0x7fe32b0d67fc180534870ac95a406e588e71f951f8ac9859a7cfb41f25022ce0",
                                Instant.ofEpochSecond(1635852034)),
                        132
                ),
                126));
        // UTNP in #1
        add(new MinedTransfer(
                "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                new BigDecimal("275000"),
                new MinedTx(
                        "0xe960770d6dbbb14c01567490fca7ac6b4f4fff7ace21a52e466e60dfd89a7fe9",
                        "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new Block(
                                12088227,
                                "0x3e56ce3b76fc22ab36d0d9527d1b5669f90c625228a6e81303bf7d1ed57f76bd",
                                Instant.ofEpochSecond(1616410837)),
                        130
                ),
                107));
        // Some token in #1
        add(new MinedTransfer(
                "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                new BigDecimal("300000"),
                new MinedTx(
                        "0x97413f924214bd2e6a196ef75909fa7ac6b065078c5b422c818806ba58620ab3",
                        "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                        "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                        new Block(
                                13548591,
                                "0x26dc66b6d13aa23f79768abfb766f62de9e0e77b701811ebf7fa586fdf43acc3",
                                Instant.ofEpochSecond(1636006619)),
                        75
                ),
                138));
        // UTNP out #2 (same addr, transfer 1 of 3)
        add(new MinedTransfer(
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                new BigDecimal("200000"),
                new MinedTx(
                        "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new Block(
                                13550555,
                                "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                                Instant.ofEpochSecond(1636033366)),
                        220
                ),
                258));
        // UTNP out #5
        add(new MinedTransfer(
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "0xedcc6f8f20962e6747369a71a5b89256289da87f",
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                new BigDecimal("10045.6909000003"),
                new MinedTx(
                        "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new Block(
                                13628884,
                                "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
                                Instant.ofEpochSecond(1637096843)),
                        111
                ),
                144));
        // UTNP out #6
        add(new MinedTransfer(
                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                "0x74644fd700c11dcc262eed1c59715ee874f65251",
                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                new BigDecimal("30000"),
                new MinedTx(
                        "0x3f0c1e4f1e903381c1e8ad2ad909482db20a747e212fbc32a4c626cad6bb14ab",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                        new Block(
                                13631007,
                                "0x57e6c79ffcbcc1d77d9d9debb1f7bbe1042e685e0d2f5bb7e7bf37df0494e096",
                                Instant.ofEpochSecond(1637125704)),
                        133
                ),
                173));
    }};

    @Test
    public void testSortedTransfers() {
        assertEquals("Unsorted SAMPLE1 gets sorted properly",
                SAMPLE1.transfers,
                GetTransfers.TransfersRequestResult.sortedTransfers(SAMPLE1_UNSORTED_TRANSFERS)
        );
    }

    @Test
    public void testBasicMethods() {
        assertEquals(
                "Distinct transfer senders",
                new HashSet<String>() {{
                    // Our hot swap wallet
                    add("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24");
                    // Sent in some airdrop
                    add("0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725");
                    // Sent in some ETH
                    add("0x4ec8654aa6a412846562d168f6183cf7667fd88a");
                    // Sent in some more ETH
                    add("0x956aa9fec25a346009edaaabf378f94d81128b15");
                    // Sent in UTNP
                    add("0x2329171f066ac1b4be777e8d4232ba34488803f5");
                }},
                SAMPLE1.getSenders()
        );

        assertEquals(
                "Distinct transfer receivers",
                new HashSet<String>() {{
                    // Our hot swap wallet
                    add("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24");
                    // UTNP out 1
                    add("0x6cf94eed6f7025088a909844ac73b037335604e6");
                    // UTNP out 2–4 (3 transactions)
                    add("0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688");
                    // UTNP out 5
                    add("0xedcc6f8f20962e6747369a71a5b89256289da87f");
                    // UTNP out 6
                    add("0x74644fd700c11dcc262eed1c59715ee874f65251");
                }},
                SAMPLE1.getReceivers()
        );
    }

    @Test
    public void testGetTransfersFromValid() {
        assertEquals(
                "From hotswap",
                new ArrayList<MinedTransfer>() {{
                    // UTNP out #1
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0x6cf94eed6f7025088a909844ac73b037335604e6",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("99910.067000013"),
                            new MinedTx(
                                    "0xd67d7bc775e3cbd052bb94ebf1744cbbaa3663d2a9208423baa129652dc1754e",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13537227,
                                            "0x7fe32b0d67fc180534870ac95a406e588e71f951f8ac9859a7cfb41f25022ce0",
                                            Instant.ofEpochSecond(1635852034)),
                                    132
                            ),
                            126));
                    // UTNP out #2 (same addr, transfer 1 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("200000"),
                            new MinedTx(
                                    "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550555,
                                            "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                                            Instant.ofEpochSecond(1636033366)),
                                    220
                            ),
                            258));
                    // UTNP out #3 (same addr, transfer 2 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("379912.034000027"),
                            new MinedTx(
                                    "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550616,
                                            "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                                            Instant.ofEpochSecond(1636034174)),
                                    204
                            ),
                            305));
                    // UTNP out #4 (same addr, transfer 3 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("3420000.0000000002"),
                            new MinedTx(
                                    "0x4c08150533d59ed6b013793ae6887ce5a33e703a64dcf6557883870b08e8d6eb",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550669,
                                            "0x02f05a9b028e163872ba600b406d6701dd8506afc0e9f31fa4b2d885e9185044",
                                            Instant.ofEpochSecond(1636034986)),
                                    373
                            ),
                            399));
                    // UTNP out #5
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xedcc6f8f20962e6747369a71a5b89256289da87f",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("10045.6909000003"),
                            new MinedTx(
                                    "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13628884,
                                            "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
                                            Instant.ofEpochSecond(1637096843)),
                                    111
                            ),
                            144));
                    // UTNP out #6
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0x74644fd700c11dcc262eed1c59715ee874f65251",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("30000"),
                            new MinedTx(
                                    "0x3f0c1e4f1e903381c1e8ad2ad909482db20a747e212fbc32a4c626cad6bb14ab",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13631007,
                                            "0x57e6c79ffcbcc1d77d9d9debb1f7bbe1042e685e0d2f5bb7e7bf37df0494e096",
                                            Instant.ofEpochSecond(1637125704)),
                                    133
                            ),
                            173));
                }},
                SAMPLE1.getTransfersFrom("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );
        assertEquals(
                "From some airdrop",
                new ArrayList<MinedTransfer>() {{
                    add(new MinedTransfer(
                            "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                            new BigDecimal("300000"),
                            new MinedTx(
                                    "0x97413f924214bd2e6a196ef75909fa7ac6b065078c5b422c818806ba58620ab3",
                                    "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                                    "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                                    new Block(
                                            13548591,
                                            "0x26dc66b6d13aa23f79768abfb766f62de9e0e77b701811ebf7fa586fdf43acc3",
                                            Instant.ofEpochSecond(1636006619)),
                                    75
                            ),
                            138));
                }},
                SAMPLE1.getTransfersFrom("0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725")
        );
        assertEquals(
                "From some ETH sender",
                new ArrayList<MinedTransfer>() {{
                    add(new MinedTransfer(
                            "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "",
                            new BigDecimal("0.2"),
                            new MinedTx(
                                    "0x848b45ffe1cefe1f8f75fd413e308157443c5fc16f54049164ba1ee67bf01c16",
                                    "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    new Block(
                                            13482556,
                                            "0x318d8b71a5a4950034815c1d625aac7f67dc9bd2713a43704163ccecc130bbb8",
                                            Instant.ofEpochSecond(1635111142)),
                                    17
                            ),
                            null));
                }},
                SAMPLE1.getTransfersFrom("0x4ec8654aa6a412846562d168f6183cf7667fd88a")
        );
        assertEquals(
                "From some other ETH sender",
                new ArrayList<MinedTransfer>() {{
                    add(new MinedTransfer(
                            "0x956aa9fec25a346009edaaabf378f94d81128b15",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "",
                            new BigDecimal("0.09"),
                            new MinedTx(
                                    "0x77fd81f138e22d6b06954a0e863598547f865c9584f94497148985cace0e6b1c",
                                    "0x956aa9fec25a346009edaaabf378f94d81128b15",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    new Block(
                                            13315889,
                                            "0x3ab6c594478ed4ccf5bec6ba63eecc5f03826f3677a865fb77737cd09d5ed9be",
                                            Instant.ofEpochSecond(1632853922)),
                                    381
                            ),
                            null));
                }},
                SAMPLE1.getTransfersFrom("0x956aa9fec25a346009edaaabf378f94d81128b15")
        );
        assertEquals(
                "From some UTNP sender",
                new ArrayList<MinedTransfer>() {{
                    add(new MinedTransfer(
                            "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("275000"),
                            new MinedTx(
                                    "0xe960770d6dbbb14c01567490fca7ac6b4f4fff7ace21a52e466e60dfd89a7fe9",
                                    "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            12088227,
                                            "0x3e56ce3b76fc22ab36d0d9527d1b5669f90c625228a6e81303bf7d1ed57f76bd",
                                            Instant.ofEpochSecond(1616410837)),
                                    130
                            ),
                            107));
                }},
                SAMPLE1.getTransfersFrom("0x2329171f066ac1b4be777e8d4232ba34488803f5")
        );
    }

    @Test
    public void testGetTransfersToValid() {
        assertEquals("To hot swap",
                new ArrayList<MinedTransfer>() {{
                    // UTNP in #1
                    add(new MinedTransfer(
                            "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("275000"),
                            new MinedTx(
                                    "0xe960770d6dbbb14c01567490fca7ac6b4f4fff7ace21a52e466e60dfd89a7fe9",
                                    "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            12088227,
                                            "0x3e56ce3b76fc22ab36d0d9527d1b5669f90c625228a6e81303bf7d1ed57f76bd",
                                            Instant.ofEpochSecond(1616410837)),
                                    130
                            ),
                            107));
                    // ETH in #1
                    add(new MinedTransfer(
                            "0x956aa9fec25a346009edaaabf378f94d81128b15",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "",
                            new BigDecimal("0.09"),
                            new MinedTx(
                                    "0x77fd81f138e22d6b06954a0e863598547f865c9584f94497148985cace0e6b1c",
                                    "0x956aa9fec25a346009edaaabf378f94d81128b15",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    new Block(
                                            13315889,
                                            "0x3ab6c594478ed4ccf5bec6ba63eecc5f03826f3677a865fb77737cd09d5ed9be",
                                            Instant.ofEpochSecond(1632853922)),
                                    381
                            ),
                            null));
                    // ETH in #2
                    add(new MinedTransfer(
                            "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "",
                            new BigDecimal("0.2"),
                            new MinedTx(
                                    "0x848b45ffe1cefe1f8f75fd413e308157443c5fc16f54049164ba1ee67bf01c16",
                                    "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    new Block(
                                            13482556,
                                            "0x318d8b71a5a4950034815c1d625aac7f67dc9bd2713a43704163ccecc130bbb8",
                                            Instant.ofEpochSecond(1635111142)),
                                    17
                            ),
                            null));
                    // Some token in #1
                    add(new MinedTransfer(
                            "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                            new BigDecimal("300000"),
                            new MinedTx(
                                    "0x97413f924214bd2e6a196ef75909fa7ac6b065078c5b422c818806ba58620ab3",
                                    "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                                    "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                                    new Block(
                                            13548591,
                                            "0x26dc66b6d13aa23f79768abfb766f62de9e0e77b701811ebf7fa586fdf43acc3",
                                            Instant.ofEpochSecond(1636006619)),
                                    75
                            ),
                            138));
                }},
                SAMPLE1.getTransfersTo("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );
        assertEquals("To UTNP out 1",
                new ArrayList<MinedTransfer>() {{
                    // UTNP out #1
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0x6cf94eed6f7025088a909844ac73b037335604e6",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("99910.067000013"),
                            new MinedTx(
                                    "0xd67d7bc775e3cbd052bb94ebf1744cbbaa3663d2a9208423baa129652dc1754e",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13537227,
                                            "0x7fe32b0d67fc180534870ac95a406e588e71f951f8ac9859a7cfb41f25022ce0",
                                            Instant.ofEpochSecond(1635852034)),
                                    132
                            ),
                            126));
                }},
                SAMPLE1.getTransfersTo("0x6cf94eed6f7025088a909844ac73b037335604e6")
        );
        assertEquals("To UTNP out 2–4 (3 transactions)",
                new ArrayList<MinedTransfer>() {{
                    // UTNP out #2 (same addr, transfer 1 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("200000"),
                            new MinedTx(
                                    "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550555,
                                            "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                                            Instant.ofEpochSecond(1636033366)),
                                    220
                            ),
                            258));
                    // UTNP out #3 (same addr, transfer 2 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("379912.034000027"),
                            new MinedTx(
                                    "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550616,
                                            "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                                            Instant.ofEpochSecond(1636034174)),
                                    204
                            ),
                            305));
                    // UTNP out #4 (same addr, transfer 3 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("3420000.0000000002"),
                            new MinedTx(
                                    "0x4c08150533d59ed6b013793ae6887ce5a33e703a64dcf6557883870b08e8d6eb",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550669,
                                            "0x02f05a9b028e163872ba600b406d6701dd8506afc0e9f31fa4b2d885e9185044",
                                            Instant.ofEpochSecond(1636034986)),
                                    373
                            ),
                            399));
                }},
                SAMPLE1.getTransfersTo("0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688")
        );
        assertEquals("To UTNP out 5",
                new ArrayList<MinedTransfer>() {{
                    // UTNP out #5
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xedcc6f8f20962e6747369a71a5b89256289da87f",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("10045.6909000003"),
                            new MinedTx(
                                    "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13628884,
                                            "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
                                            Instant.ofEpochSecond(1637096843)),
                                    111
                            ),
                            144));
                }},
                SAMPLE1.getTransfersTo("0xedcc6f8f20962e6747369a71a5b89256289da87f")
        );
        assertEquals("To UTNP out 6",
                new ArrayList<MinedTransfer>() {{
                    // UTNP out #6
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0x74644fd700c11dcc262eed1c59715ee874f65251",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("30000"),
                            new MinedTx(
                                    "0x3f0c1e4f1e903381c1e8ad2ad909482db20a747e212fbc32a4c626cad6bb14ab",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13631007,
                                            "0x57e6c79ffcbcc1d77d9d9debb1f7bbe1042e685e0d2f5bb7e7bf37df0494e096",
                                            Instant.ofEpochSecond(1637125704)),
                                    133
                            ),
                            173));
                }},
                SAMPLE1.getTransfersTo("0x74644fd700c11dcc262eed1c59715ee874f65251")
        );
    }

    @Test
    public void testGetTransfersFromInvalidArgs() {
        assertThrows("Empty string instead of address is wrong",
                AssertionError.class,
                () -> SAMPLE1.getTransfersFrom("")
        );
        assertThrows("Non-lowercase address is wrong (even EIP55)",
                AssertionError.class,
                () -> SAMPLE1.getTransfersFrom("0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24")
        );
        assertThrows("Wrong-length string is wrong",
                AssertionError.class,
                () -> SAMPLE1.getTransfersFrom("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2")
        );
        assertThrows("Non-hex-format is wrong, even if just a whitespace",
                AssertionError.class,
                () -> SAMPLE1.getTransfersFrom("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2 ")
        );
    }

    @Test
    public void testGetTransfersToInvalidArgs() {
        assertThrows("Empty string instead of address is wrong",
                AssertionError.class,
                () -> SAMPLE1.getTransfersTo("")
        );
        assertThrows("Non-lowercase address is wrong (even EIP55)",
                AssertionError.class,
                () -> SAMPLE1.getTransfersTo("0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24")
        );
        assertThrows("Wrong-length string is wrong",
                AssertionError.class,
                () -> SAMPLE1.getTransfersTo("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2")
        );
        assertThrows("Non-hex-format is wrong, even if just a whitespace",
                AssertionError.class,
                () -> SAMPLE1.getTransfersTo("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2 ")
        );
    }

    @Test
    public void testGetTransfersFromEmptySearches() {
        assertEquals("This address is present as a to-address; but not present as a from-address",
                Collections.emptyList(),
                SAMPLE1.getTransfersFrom("0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688")
        );
        assertEquals("This address is completely missing from the data",
                Collections.emptyList(),
                SAMPLE1.getTransfersFrom("0x0000000000000000000000000000000000000000")
        );
    }

    @Test
    public void testGetTransfersToEmptySearches() {
        assertEquals("This address is present as a from-address; but not present as a to-address",
                Collections.emptyList(),
                SAMPLE1.getTransfersTo("0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725")
        );
        assertEquals("This address is completely missing from the data",
                Collections.emptyList(),
                SAMPLE1.getTransfersTo("0x0000000000000000000000000000000000000000")
        );
    }

    @Test
    public void testGetTransfersValidSingleKey() {
        {
            assertEquals(
                    "From hotswap",
                    new ArrayList<MinedTransfer>() {{
                        // UTNP out #1
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x6cf94eed6f7025088a909844ac73b037335604e6",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("99910.067000013"),
                                new MinedTx(
                                        "0xd67d7bc775e3cbd052bb94ebf1744cbbaa3663d2a9208423baa129652dc1754e",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13537227,
                                                "0x7fe32b0d67fc180534870ac95a406e588e71f951f8ac9859a7cfb41f25022ce0",
                                                Instant.ofEpochSecond(1635852034)),
                                        132
                                ),
                                126));
                        // UTNP out #2 (same addr, transfer 1 of 3)
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("200000"),
                                new MinedTx(
                                        "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13550555,
                                                "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                                                Instant.ofEpochSecond(1636033366)),
                                        220
                                ),
                                258));
                        // UTNP out #3 (same addr, transfer 2 of 3)
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("379912.034000027"),
                                new MinedTx(
                                        "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13550616,
                                                "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                                                Instant.ofEpochSecond(1636034174)),
                                        204
                                ),
                                305));
                        // UTNP out #4 (same addr, transfer 3 of 3)
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("3420000.0000000002"),
                                new MinedTx(
                                        "0x4c08150533d59ed6b013793ae6887ce5a33e703a64dcf6557883870b08e8d6eb",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13550669,
                                                "0x02f05a9b028e163872ba600b406d6701dd8506afc0e9f31fa4b2d885e9185044",
                                                Instant.ofEpochSecond(1636034986)),
                                        373
                                ),
                                399));
                        // UTNP out #5
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xedcc6f8f20962e6747369a71a5b89256289da87f",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("10045.6909000003"),
                                new MinedTx(
                                        "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13628884,
                                                "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
                                                Instant.ofEpochSecond(1637096843)),
                                        111
                                ),
                                144));
                        // UTNP out #6
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x74644fd700c11dcc262eed1c59715ee874f65251",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("30000"),
                                new MinedTx(
                                        "0x3f0c1e4f1e903381c1e8ad2ad909482db20a747e212fbc32a4c626cad6bb14ab",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13631007,
                                                "0x57e6c79ffcbcc1d77d9d9debb1f7bbe1042e685e0d2f5bb7e7bf37df0494e096",
                                                Instant.ofEpochSecond(1637125704)),
                                        133
                                ),
                                173));
                    }},
                    SAMPLE1.getTransfers("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24", null)
            );
            assertEquals(
                    "From some airdrop",
                    new ArrayList<MinedTransfer>() {{
                        add(new MinedTransfer(
                                "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                                new BigDecimal("300000"),
                                new MinedTx(
                                        "0x97413f924214bd2e6a196ef75909fa7ac6b065078c5b422c818806ba58620ab3",
                                        "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                                        "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                                        new Block(
                                                13548591,
                                                "0x26dc66b6d13aa23f79768abfb766f62de9e0e77b701811ebf7fa586fdf43acc3",
                                                Instant.ofEpochSecond(1636006619)),
                                        75
                                ),
                                138));
                    }},
                    SAMPLE1.getTransfers("0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725", null)
            );
            assertEquals(
                    "From some ETH sender",
                    new ArrayList<MinedTransfer>() {{
                        add(new MinedTransfer(
                                "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "",
                                new BigDecimal("0.2"),
                                new MinedTx(
                                        "0x848b45ffe1cefe1f8f75fd413e308157443c5fc16f54049164ba1ee67bf01c16",
                                        "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        new Block(
                                                13482556,
                                                "0x318d8b71a5a4950034815c1d625aac7f67dc9bd2713a43704163ccecc130bbb8",
                                                Instant.ofEpochSecond(1635111142)),
                                        17
                                ),
                                null));
                    }},
                    SAMPLE1.getTransfers("0x4ec8654aa6a412846562d168f6183cf7667fd88a", null)
            );
            assertEquals(
                    "From some other ETH sender",
                    new ArrayList<MinedTransfer>() {{
                        add(new MinedTransfer(
                                "0x956aa9fec25a346009edaaabf378f94d81128b15",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "",
                                new BigDecimal("0.09"),
                                new MinedTx(
                                        "0x77fd81f138e22d6b06954a0e863598547f865c9584f94497148985cace0e6b1c",
                                        "0x956aa9fec25a346009edaaabf378f94d81128b15",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        new Block(
                                                13315889,
                                                "0x3ab6c594478ed4ccf5bec6ba63eecc5f03826f3677a865fb77737cd09d5ed9be",
                                                Instant.ofEpochSecond(1632853922)),
                                        381
                                ),
                                null));
                    }},
                    SAMPLE1.getTransfers("0x956aa9fec25a346009edaaabf378f94d81128b15", null)
            );
            assertEquals(
                    "From some UTNP sender",
                    new ArrayList<MinedTransfer>() {{
                        add(new MinedTransfer(
                                "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("275000"),
                                new MinedTx(
                                        "0xe960770d6dbbb14c01567490fca7ac6b4f4fff7ace21a52e466e60dfd89a7fe9",
                                        "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                12088227,
                                                "0x3e56ce3b76fc22ab36d0d9527d1b5669f90c625228a6e81303bf7d1ed57f76bd",
                                                Instant.ofEpochSecond(1616410837)),
                                        130
                                ),
                                107));
                    }},
                    SAMPLE1.getTransfers("0x2329171f066ac1b4be777e8d4232ba34488803f5", null)
            );
        }

        {
            assertEquals("To hot swap",
                    new ArrayList<MinedTransfer>() {{
                        // UTNP in #1
                        add(new MinedTransfer(
                                "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("275000"),
                                new MinedTx(
                                        "0xe960770d6dbbb14c01567490fca7ac6b4f4fff7ace21a52e466e60dfd89a7fe9",
                                        "0x2329171f066ac1b4be777e8d4232ba34488803f5",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                12088227,
                                                "0x3e56ce3b76fc22ab36d0d9527d1b5669f90c625228a6e81303bf7d1ed57f76bd",
                                                Instant.ofEpochSecond(1616410837)),
                                        130
                                ),
                                107));
                        // ETH in #1
                        add(new MinedTransfer(
                                "0x956aa9fec25a346009edaaabf378f94d81128b15",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "",
                                new BigDecimal("0.09"),
                                new MinedTx(
                                        "0x77fd81f138e22d6b06954a0e863598547f865c9584f94497148985cace0e6b1c",
                                        "0x956aa9fec25a346009edaaabf378f94d81128b15",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        new Block(
                                                13315889,
                                                "0x3ab6c594478ed4ccf5bec6ba63eecc5f03826f3677a865fb77737cd09d5ed9be",
                                                Instant.ofEpochSecond(1632853922)),
                                        381
                                ),
                                null));
                        // ETH in #2
                        add(new MinedTransfer(
                                "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "",
                                new BigDecimal("0.2"),
                                new MinedTx(
                                        "0x848b45ffe1cefe1f8f75fd413e308157443c5fc16f54049164ba1ee67bf01c16",
                                        "0x4ec8654aa6a412846562d168f6183cf7667fd88a",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        new Block(
                                                13482556,
                                                "0x318d8b71a5a4950034815c1d625aac7f67dc9bd2713a43704163ccecc130bbb8",
                                                Instant.ofEpochSecond(1635111142)),
                                        17
                                ),
                                null));
                        // Some token in #1
                        add(new MinedTransfer(
                                "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                                new BigDecimal("300000"),
                                new MinedTx(
                                        "0x97413f924214bd2e6a196ef75909fa7ac6b065078c5b422c818806ba58620ab3",
                                        "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                                        "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                                        new Block(
                                                13548591,
                                                "0x26dc66b6d13aa23f79768abfb766f62de9e0e77b701811ebf7fa586fdf43acc3",
                                                Instant.ofEpochSecond(1636006619)),
                                        75
                                ),
                                138));
                    }},
                    SAMPLE1.getTransfers(null, "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
            );
            assertEquals("To UTNP out 1",
                    new ArrayList<MinedTransfer>() {{
                        // UTNP out #1
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x6cf94eed6f7025088a909844ac73b037335604e6",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("99910.067000013"),
                                new MinedTx(
                                        "0xd67d7bc775e3cbd052bb94ebf1744cbbaa3663d2a9208423baa129652dc1754e",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13537227,
                                                "0x7fe32b0d67fc180534870ac95a406e588e71f951f8ac9859a7cfb41f25022ce0",
                                                Instant.ofEpochSecond(1635852034)),
                                        132
                                ),
                                126));
                    }},
                    SAMPLE1.getTransfers(null, "0x6cf94eed6f7025088a909844ac73b037335604e6")
            );
            assertEquals("To UTNP out 2–4 (3 transactions)",
                    new ArrayList<MinedTransfer>() {{
                        // UTNP out #2 (same addr, transfer 1 of 3)
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("200000"),
                                new MinedTx(
                                        "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13550555,
                                                "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                                                Instant.ofEpochSecond(1636033366)),
                                        220
                                ),
                                258));
                        // UTNP out #3 (same addr, transfer 2 of 3)
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("379912.034000027"),
                                new MinedTx(
                                        "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13550616,
                                                "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                                                Instant.ofEpochSecond(1636034174)),
                                        204
                                ),
                                305));
                        // UTNP out #4 (same addr, transfer 3 of 3)
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("3420000.0000000002"),
                                new MinedTx(
                                        "0x4c08150533d59ed6b013793ae6887ce5a33e703a64dcf6557883870b08e8d6eb",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13550669,
                                                "0x02f05a9b028e163872ba600b406d6701dd8506afc0e9f31fa4b2d885e9185044",
                                                Instant.ofEpochSecond(1636034986)),
                                        373
                                ),
                                399));
                    }},
                    SAMPLE1.getTransfers(null, "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688")
            );
            assertEquals("To UTNP out 5",
                    new ArrayList<MinedTransfer>() {{
                        // UTNP out #5
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0xedcc6f8f20962e6747369a71a5b89256289da87f",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("10045.6909000003"),
                                new MinedTx(
                                        "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13628884,
                                                "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
                                                Instant.ofEpochSecond(1637096843)),
                                        111
                                ),
                                144));
                    }},
                    SAMPLE1.getTransfers(null, "0xedcc6f8f20962e6747369a71a5b89256289da87f")
            );
            assertEquals("To UTNP out 6",
                    new ArrayList<MinedTransfer>() {{
                        // UTNP out #6
                        add(new MinedTransfer(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "0x74644fd700c11dcc262eed1c59715ee874f65251",
                                "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                new BigDecimal("30000"),
                                new MinedTx(
                                        "0x3f0c1e4f1e903381c1e8ad2ad909482db20a747e212fbc32a4c626cad6bb14ab",
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                        new Block(
                                                13631007,
                                                "0x57e6c79ffcbcc1d77d9d9debb1f7bbe1042e685e0d2f5bb7e7bf37df0494e096",
                                                Instant.ofEpochSecond(1637125704)),
                                        133
                                ),
                                173));
                    }},
                    SAMPLE1.getTransfers(null, "0x74644fd700c11dcc262eed1c59715ee874f65251")
            );
        }
    }

    @Test
    public void testGetTransfersValidMultiKey() {
        assertEquals(
                "From some airdrop to hotswap",
                new ArrayList<MinedTransfer>() {{
                    add(new MinedTransfer(
                            "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                            new BigDecimal("300000"),
                            new MinedTx(
                                    "0x97413f924214bd2e6a196ef75909fa7ac6b065078c5b422c818806ba58620ab3",
                                    "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                                    "0xc7870abf8b0d927e40b96bc8125ed2644691b27e",
                                    new Block(
                                            13548591,
                                            "0x26dc66b6d13aa23f79768abfb766f62de9e0e77b701811ebf7fa586fdf43acc3",
                                            Instant.ofEpochSecond(1636006619)),
                                    75
                            ),
                            138));
                }},
                SAMPLE1.getTransfers(
                        "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );
        assertEquals(
                "From some other ETH sender to hotswap",
                new ArrayList<MinedTransfer>() {{
                    add(new MinedTransfer(
                            "0x956aa9fec25a346009edaaabf378f94d81128b15",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "",
                            new BigDecimal("0.09"),
                            new MinedTx(
                                    "0x77fd81f138e22d6b06954a0e863598547f865c9584f94497148985cace0e6b1c",
                                    "0x956aa9fec25a346009edaaabf378f94d81128b15",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    new Block(
                                            13315889,
                                            "0x3ab6c594478ed4ccf5bec6ba63eecc5f03826f3677a865fb77737cd09d5ed9be",
                                            Instant.ofEpochSecond(1632853922)),
                                    381
                            ),
                            null));
                }},
                SAMPLE1.getTransfers(
                        "0x956aa9fec25a346009edaaabf378f94d81128b15",
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );

        assertEquals(
                "From hotswap, single result",
                new ArrayList<MinedTransfer>() {{
                    // UTNP out #5
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xedcc6f8f20962e6747369a71a5b89256289da87f",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("10045.6909000003"),
                            new MinedTx(
                                    "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13628884,
                                            "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
                                            Instant.ofEpochSecond(1637096843)),
                                    111
                            ),
                            144));
                }},
                SAMPLE1.getTransfers(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0xedcc6f8f20962e6747369a71a5b89256289da87f")
        );
        assertEquals(
                "From hotswap, multiple results",
                new ArrayList<MinedTransfer>() {{
                    // UTNP out #2 (same addr, transfer 1 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("200000"),
                            new MinedTx(
                                    "0xb0b3d18c67857c30829e348987899026ee08232c989d60e47ccd78dca375d79a",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550555,
                                            "0x4246574f55f6bb00326e17fa5ed6724df0b821babd3bf456cee2fd6a7b4dd25a",
                                            Instant.ofEpochSecond(1636033366)),
                                    220
                            ),
                            258));
                    // UTNP out #3 (same addr, transfer 2 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("379912.034000027"),
                            new MinedTx(
                                    "0x17dd446c6901b78351a22007218391ead9d1a3c97ba6f0fa27c4b027ed099fd7",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550616,
                                            "0xdc73aa4e8b3109e5215d541d4604b1fb700f4dcdb65efe3ac68b49568ddb1da7",
                                            Instant.ofEpochSecond(1636034174)),
                                    204
                            ),
                            305));
                    // UTNP out #4 (same addr, transfer 3 of 3)
                    add(new MinedTransfer(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                            new BigDecimal("3420000.0000000002"),
                            new MinedTx(
                                    "0x4c08150533d59ed6b013793ae6887ce5a33e703a64dcf6557883870b08e8d6eb",
                                    "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                    "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                                    new Block(
                                            13550669,
                                            "0x02f05a9b028e163872ba600b406d6701dd8506afc0e9f31fa4b2d885e9185044",
                                            Instant.ofEpochSecond(1636034986)),
                                    373
                            ),
                            399));
                }},
                SAMPLE1.getTransfers(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688")
        );
    }

    @Test
    public void testGetTransfersInvalidArguments() {
        // “From” is bad, “to” is null
        {
            assertThrows("Empty string instead of address is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers("", null)
            );
            assertThrows("Non-lowercase address is wrong (even EIP55)",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers("0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24", null)
            );
            assertThrows("Wrong-length string is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2", null)
            );
            assertThrows("Non-hex-format is wrong, even if just a whitespace",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2 ", null)
            );
        }
        // “From” is bad, “to” is good
        {
            assertThrows("Empty string instead of address is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
            );
            assertThrows("Non-lowercase address is wrong (even EIP55)",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
            );
            assertThrows("Wrong-length string is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
            );
            assertThrows("Non-hex-format is wrong, even if just a whitespace",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2 ",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
            );
        }
        // “From” is null, “to” is bad
        {
            assertThrows("Empty string instead of address is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(null, "")
            );
            assertThrows("Non-lowercase address is wrong (even EIP55)",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(null, "0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24")
            );
            assertThrows("Wrong-length string is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(null, "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2")
            );
            assertThrows("Non-hex-format is wrong, even if just a whitespace",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(null, "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2 ")
            );
        }
        // “From” is good, “to” is bad
        {
            assertThrows("Empty string instead of address is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "")
            );
            assertThrows("Non-lowercase address is wrong (even EIP55)",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24")
            );
            assertThrows("Wrong-length string is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2")
            );
            assertThrows("Non-hex-format is wrong, even if just a whitespace",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2 ")
            );
        }
        // Both “From” and “to” are null
        {
            assertThrows("At least one search must be provided!",
                    RuntimeException.class,
                    () -> SAMPLE1.getTransfers(null, null)
            );
        }
        // Both “From” and “to” are bad
        {
            assertThrows("From: Empty string instead of address is wrong; to: Non-lowercase address is wrong (even EIP55)",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "",
                            "0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24")
            );
            assertThrows("From: Non-lowercase address is wrong (even EIP55); to: Wrong-length string is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2")
            );
            assertThrows("From: Wrong-length string is wrong; to: Non-hex-format is wrong, even if just a whitespace",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2 ")
            );
            assertThrows("From: Non-hex-format is wrong, even if just a whitespace; to: Empty string instead of address is wrong",
                    AssertionError.class,
                    () -> SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2 ",
                            "")
            );

        }
    }

    @Test
    public void testGetTransfersEmptySearches() {
        // Just “from”
        {
            assertEquals("This address is present as a to-address; but not present as a from-address",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers("0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688", null)
            );
            assertEquals("This address is completely missing from the data",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers("0x0000000000000000000000000000000000000000", null)
            );
        }

        // Just “to”
        {
            assertEquals("This address is present as a from-address; but not present as a to-address",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(null, "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725")
            );
            assertEquals("This address is completely missing from the data",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(null, "0x0000000000000000000000000000000000000000")
            );
        }

        // Both “from” and “to”, both keys missing
        {
            assertEquals("This address is present as a from-address; but not present as a to-address",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0x0000000000000000000000000000000000000000",
                            "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725")
            );
            assertEquals("This address is present as a to-address; but not present as a from-address",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0x0000000000000000000000000000000000000000")
            );
        }
        {
            assertEquals("From-address matches; but to-address is completely missing",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                            "0x0000000000000000000000000000000000000000")
            );
            assertEquals("To-address matches; but from-address is completely missing",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0x0000000000000000000000000000000000000000",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688")
            );
            assertEquals("From-address matches (and is hotswap); but to-address is completely missing",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0x0000000000000000000000000000000000000000")
            );
            assertEquals("To-address (and is hotswap) matches; but from-address is completely missing",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0x0000000000000000000000000000000000000000",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
            );
        }
        {
            assertEquals("From-address matches; but to-address is different",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0x3fc862aa399a0ab491b39eb4a9e811b1e8a30725",
                            "0x6cf94eed6f7025088a909844ac73b037335604e6")
            );
            assertEquals("To-address matches; but from-address is different",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0x956aa9fec25a346009edaaabf378f94d81128b15",
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688")
            );
            assertEquals("From-address matches (and is hotswap); but to-address is different",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0x2329171f066ac1b4be777e8d4232ba34488803f5")
            );
            assertEquals("To-address matches (and is hotswap); but from-address is different",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0xaa9f5344e0a207b4d5d59cb00ea939a97e81c688",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
            );
        }
        {
            assertEquals("From hotswap to hotswap",
                    Collections.emptyList(),
                    SAMPLE1.getTransfers(
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                            "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
            );
        }
    }
}
