package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import com.myodov.unicherrygarden.cherrygardener.connector.api.AddressOwnershipConfirmator;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.security.SignatureException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AddressOwnershipConfirmatorImplTest {
    final AddressOwnershipConfirmator confirmator = new AddressOwnershipConfirmatorImpl();

    static final String PK1_STR = "f6e28eb945e0835f7ac0f0ccdaaf717f5700554e42ed21fc23580450f5243540";
    static final String PK1_ADDR_EIP55 = "0x34e1E4F805fCdC936068A760b2C17BC62135b5AE";
    static final String PK1_ADDR = PK1_ADDR_EIP55.toLowerCase();

    static final String MSG1 = "JohnDoe";
    static final String MSG1_SIG = "0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c";
//    {
//  "address": "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
//  "msg": "JohnDoe",
//  "sig": "0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c",
//  "version": "2"
//}

    static final String MSG2 = (
            "I am John Doe, confirming that I own and control the address 0x34e1E4F805fCdC936068A760b2C17BC62135b5AE for the needs of CherryGarden service.\n" +
                    "Name: John Doe\n" +
                    "Account id: JohnDoe42\n" +
                    "Date: July 5 2021\n" +
                    "Address: 0x34e1E4F805fCdC936068A760b2C17BC62135b5AE\n" +
                    "Service: CherryGarden");
    static final String MSG2_SIG = "0x6e5712d25d599587c667eb192614aec3e9eb3f80ac0240eb776d103b10ddeaca5440585ad6d67d233e7e565c51440a0b5a8c7a701db22f60f30caab7fed33d451c";


    @Test
    public void testGetMessageSigner() {
        assertEquals(
                "Unit test from MyCrypto source code",
                Optional.of("0x6980ba0ab378c2ed0efccd7ea6ab84d54615a2de"),
                confirmator.getMessageSigner(
                        "Testing your code is great!",
                        "0xf08688e9dddbb5e4e0d1fb685ee9f693accb3c9aac84fdcf327423ca4a1c50463ef7aeb70be3221fe028bc752e210a4c377db8090bc4efa5ea7d391049c3a4771c"
                )
        );

        assertEquals(
                "Malformed sig (no “0x”)",
                Optional.empty(),
                confirmator.getMessageSigner(
                        "Testing your code is great!",
                        "f08688e9dddbb5e4e0d1fb685ee9f693accb3c9aac84fdcf327423ca4a1c50463ef7aeb70be3221fe028bc752e210a4c377db8090bc4efa5ea7d391049c3a4771c"
                )
        );

        assertEquals(
                "Malformed sig (invalid symbols inside)",
                Optional.empty(),
                confirmator.getMessageSigner(
                        "Testing your code is great!",
                        "0xf08688e9dddbb5e4e0d1fb685ee9f693accb3c9aac84fdcf327423ca4a1c50463ef7aeb70be3221fe028bc752e210a4c377db8090bc4efa5ea7d391049c3a4771h"
                )
        );

        assertEquals(
                "Malformed sig (wrong length)",
                Optional.empty(),
                confirmator.getMessageSigner(
                        "Testing your code is great!",
                        "0xf08688e9dddbb5e4e0d1fb685ee9f693accb3c9aac84fdcf327423ca4a1c50463ef7aeb70be3221fe028bc752e210a4c377db8090bc4efa5ea7d391049c3a4771cdd"
                )
        );

        assertNotEquals(
                "Altered msg should have a different sig (not an Optional.empty though)",
                Optional.of("0x6980ba0ab378c2ed0efccd7ea6ab84d54615a2de"),
                confirmator.getMessageSigner(
                        "Testing your code is great?",
                        "0xf08688e9dddbb5e4e0d1fb685ee9f693accb3c9aac84fdcf327423ca4a1c50463ef7aeb70be3221fe028bc752e210a4c377db8090bc4efa5ea7d391049c3a4771c"
                )
        );

        assertEquals(
                "Some custom test message (1)",
                Optional.of(PK1_ADDR),
                confirmator.getMessageSigner(
                        MSG1,
                        MSG1_SIG
                )
        );

        assertEquals(
                "Some custom test message (2)",
                Optional.of(PK1_ADDR),
                confirmator.getMessageSigner(
                        MSG2,
                        MSG2_SIG
                )
        );
    }

    //    {
//        "address": "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
//            "msg": "I am John Doe, confirming that I own and control the address 0x34e1E4F805fCdC936068A760b2C17BC62135b5AE for the needs of CherryGarden service.\nName: John Doe\nAccount id: JohnDoe42\nDate: July 5 2021 \nAddress: 0x34e1E4F805fCdC936068A760b2C17BC62135b5AE\nService: CherryGarden",
//            "sig": "0x9af352d971b4ac4d524652790feb987a461d7b811e46faa56bc7da734bef0fcc6ad9151333f3d76d662654a9237ef1cade723010f3217673464200ede47802f41b",
//            "version": "2"
//    }
    @Test
    public void testValidateMessage() throws SignatureException {

    }
}
