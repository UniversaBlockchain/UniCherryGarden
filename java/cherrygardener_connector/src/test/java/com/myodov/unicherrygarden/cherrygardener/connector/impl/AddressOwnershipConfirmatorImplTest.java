package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import com.myodov.unicherrygarden.cherrygardener.connector.api.AddressOwnershipConfirmator;
import com.myodov.unicherrygarden.cherrygardener.connector.api.types.PrivateKey;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.security.SignatureException;
import java.util.Optional;

import static org.junit.Assert.*;

public class AddressOwnershipConfirmatorImplTest {
    final AddressOwnershipConfirmator confirmator = new AddressOwnershipConfirmatorImpl();

    static final String PK1_STR = "f6e28eb945e0835f7ac0f0ccdaaf717f5700554e42ed21fc23580450f5243540";
    static final String PK1_ADDR_EIP55 = "0x34e1E4F805fCdC936068A760b2C17BC62135b5AE";
    static final String PK1_ADDR = PK1_ADDR_EIP55.toLowerCase();

    static final String MSG1 = "JohnDoe";
    static final String MSG1_SIG = "0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c";

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

    @Test
    public void testValidateMessage() throws SignatureException {
        assertFalse(
                "Invalid JSON",
                confirmator.validateMessage(
                        "BAD JSON {\n" +
                                "  \"address\": \"0x34e1e4f805fcdc936068a760b2c17bc62135b5ae\",\n" +
                                "  \"msg\": \"JohnDoe\",\n" +
                                "  \"sig\": \"0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c\",\n" +
                                "  \"version\": \"2\"\n" +
                                "}"
                ).isPresent()
        );

        assertFalse(
                "Bad address in JSON",
                confirmator.validateMessage(
                        "{\n" +
                                "  \"address\": \"0x34e1e4f805fcdc936068a760b2c17bc62135b5aeDD\",\n" +
                                "  \"msg\": \"JohnDoe\",\n" +
                                "  \"sig\": \"0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c\",\n" +
                                "  \"version\": \"2\"\n" +
                                "}"
                ).isPresent()
        );

        assertFalse(
                "No address field",
                confirmator.validateMessage(
                        "{\n" +
                                "  \"msg\": \"JohnDoe\",\n" +
                                "  \"sig\": \"0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c\",\n" +
                                "  \"version\": \"2\"\n" +
                                "}"
                ).isPresent()
        );

        assertFalse(
                "No msg field",
                confirmator.validateMessage(
                        "{\n" +
                                "  \"address\": \"0x34e1e4f805fcdc936068a760b2c17bc62135b5ae\",\n" +
                                "  \"sig\": \"0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c\",\n" +
                                "  \"version\": \"2\"\n" +
                                "}"
                ).isPresent()
        );

        assertFalse(
                "No sig field",
                confirmator.validateMessage(
                        "{\n" +
                                "  \"address\": \"0x34e1e4f805fcdc936068a760b2c17bc62135b5ae\",\n" +
                                "  \"msg\": \"JohnDoe\",\n" +
                                "  \"version\": \"2\"\n" +
                                "}"
                ).isPresent()
        );

        assertFalse(
                "Unparseable signature",
                confirmator.validateMessage(
                        "{\n" +
                                "  \"address\": \"0x34e1e4f805fcdc936068a760b2c17bc62135b5ae\",\n" +
                                "  \"msg\": \"JohnDoe\",\n" +
                                "  \"sig\": \"0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241cGH\",\n" +
                                "  \"version\": \"2\"\n" +
                                "}"
                ).isPresent()
        );

        // Bad (incalculatable) signature
        {
            final Optional<AddressOwnershipConfirmator.AddressOwnershipMessageValidation> addressOwnershipMessageValidation = confirmator.validateMessage(
                    "{\n" +
                            "  \"address\": \"0x34e1e4f805fcdc936068a760b2c17bc62135b5ae\",\n" +
                            "  \"msg\": \"JohnDoe\",\n" +
                            "  \"sig\": \"0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241d\",\n" +
                            "  \"version\": \"2\"\n" +
                            "}"
            );
            assertFalse(addressOwnershipMessageValidation.isPresent());
        }

        // Mismatching signature
        {
            final Optional<AddressOwnershipConfirmator.AddressOwnershipMessageValidation> addressOwnershipMessageValidation = confirmator.validateMessage(
                    "{\n" +
                            "  \"address\": \"0x34e1e4f805fcdc936068a760b2c17bc62135b5ad\",\n" +
                            "  \"msg\": \"JohnDoe\",\n" +
                            "  \"sig\": \"0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c\",\n" +
                            "  \"version\": \"2\"\n" +
                            "}"
            );

            assertTrue(
                    addressOwnershipMessageValidation.isPresent()
            );

            assertEquals(
                    "0x34e1e4f805fcdc936068a760b2c17bc62135b5ad",
                    addressOwnershipMessageValidation.get().declaredAddress
            );
            assertEquals(
                    "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
                    addressOwnershipMessageValidation.get().signingAddress
            );
            assertEquals(
                    "JohnDoe",
                    addressOwnershipMessageValidation.get().message
            );
            assertFalse(
                    addressOwnershipMessageValidation.get().addressIsMatching()
            );
        }

        // Valid signature
        {
            final Optional<AddressOwnershipConfirmator.AddressOwnershipMessageValidation> addressOwnershipMessageValidation = confirmator.validateMessage(
                    "{\n" +
                            "  \"address\": \"0x34e1e4f805fcdc936068a760b2c17bc62135b5ae\",\n" +
                            "  \"msg\": \"JohnDoe\",\n" +
                            "  \"sig\": \"0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c\",\n" +
                            "  \"version\": \"2\"\n" +
                            "}"
            );

            assertTrue(
                    addressOwnershipMessageValidation.isPresent()
            );

            assertEquals(
                    "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
                    addressOwnershipMessageValidation.get().declaredAddress
            );
            assertEquals(
                    "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
                    addressOwnershipMessageValidation.get().signingAddress
            );
            assertEquals(
                    "JohnDoe",
                    addressOwnershipMessageValidation.get().message
            );
            assertTrue(
                    addressOwnershipMessageValidation.get().addressIsMatching()
            );
        }
    }

    @Test
    public void testPrivateKeySignOwnershipConfirmatorIntegration() {
        final PrivateKey pkCopy;
        try (final PrivateKey pk = new PrivateKeyImpl(Hex.decode(PK1_STR))) {
            assertEquals(
                    MSG1_SIG,
                    pk.signMessageToSig(MSG1)
            );

            final Optional<String> signer = confirmator.getMessageSigner(MSG1, pk.signMessageToSig(MSG1));
            assertTrue(signer.isPresent());

            assertEquals(
                    PK1_ADDR,
                    signer.get()
            );

            // And by the way, leaking the private key outside “try-with-resources” zone
            // should wipe the key, let’s test it too
            pkCopy = pk;
        }

        assertNotEquals(
                "the private key is wiped after try-with-resources zone and cannot be used for signing anymore",
                MSG1_SIG,
                pkCopy.signMessageToSig(MSG1)
        );
    }
}
