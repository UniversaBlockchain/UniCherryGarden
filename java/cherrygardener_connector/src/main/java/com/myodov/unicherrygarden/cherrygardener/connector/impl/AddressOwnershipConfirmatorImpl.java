package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myodov.unicherrygarden.cherrygardener.connector.api.AddressOwnershipConfirmator;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.security.SignatureException;
import java.util.Optional;

public class AddressOwnershipConfirmatorImpl implements AddressOwnershipConfirmator {
    final Logger logger = LoggerFactory.getLogger(AddressOwnershipConfirmatorImpl.class);

    private static Sign.SignatureData getSignatureData(@NonNull String sig) {
        final byte[] r = Hex.decode(sig.substring(2 + 0, 2 + 64));
        final byte[] s = Hex.decode(sig.substring(2 + 64, 2 + 128));
        final byte[] v = Hex.decode(sig.substring(2 + 128));
        return new Sign.SignatureData(v, r, s);
    }

    @NonNull
    public Optional<String> getMessageSigner(@NonNull String msg, @NonNull String sig) {
        if ((msg == null)
                || (sig == null)
                || (sig.length() != 132)
                || !sig.matches("^0x\\p{XDigit}+$")
        ) {
            logger.error("Malformed input to getMessageSigner: msg {}, sig {}", msg, sig);
            return Optional.empty();
        }

        try {
            final Sign.SignatureData signatureData = getSignatureData(sig);
            return Optional.of(
                    "0x" + Keys.getAddress(Sign.signedPrefixedMessageToKey(msg.getBytes(), signatureData))
            );
        } catch (DecoderException e) {
            logger.error("Bad hex data in signature message", msg, sig);
            logger.error("Error:", e);
            return Optional.empty();
        } catch (SignatureException e) {
            logger.error("Cannot get message signer for msg {}, sig {}", msg, sig);
            logger.error("Error:", e);
            return Optional.empty();
        }
    }

    /**
     * Check if the <code>signatureMessage</code> is signed by the owner of <code>isSignedByAddress</code>.`
     * <code>signatureMessage</code> should have the contents like MyEtherWallet/MyCrypto generates,
     * a JSON-like structure like this:
     * <pre>
     * {
     *     "address": "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
     *     "msg": "I, John Doe, on 31/12/2021 confirm that I own and control the address 0x34e1E4F805fCdC936068A760b2C17BC62135b5AE for the needs of CherryGarden service.",
     *     "sig": "0xb2f88840c6895a12c1ea114cf73fbfcf4b276470cf1165ee10f2397ae7006c882a3cfbd95080f0ff03d808e81d472305e2a95d3eb5f4316e55b4edfcb10b38581b",
     *     "version": "2"
     * }
     * </pre>
     * <p>
     * Returns an {@link Optional> that is valid if the message is parsed successfully, and invalid if the message
     * could not be even parsed.
     * The {@link Optional> contains the {@link AddressOwnershipMessageValidation} structure where you can reach
     * the details of the message.
     * <p>
     * You may want:
     * <ol>
     * <li>Check the {@link AddressOwnershipMessageValidation#message} field to be sure the message contents
     * contains what you need.</li>
     * <li>Check the {@link AddressOwnershipMessageValidation#declaredAddress} and/or
     * {@link AddressOwnershipMessageValidation#signingAddress}</li> that it matches the address you
     * <li></li>
     * <li></li>
     * </ol>
     *
     * @param signatureMessage the message received from the user; typically something resembling a JSON.
     */
    @Override
    public @NonNull Optional<AddressOwnershipMessageValidation> validateMessage(@NonNull String signatureMessage) {
        final JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(signatureMessage);
        } catch (JsonProcessingException e) {
            logger.error("Cannot parse signature message: {}", signatureMessage);
            logger.error("Error is:", e);
            return Optional.empty();
        }

        @Nullable final JsonNode address = jsonNode.get("address");
        @Nullable final JsonNode sig = jsonNode.get("sig");
        @Nullable final JsonNode msg = jsonNode.get("msg");

        if (address == null) {
            logger.error("In signature message address should not be null! {}", signatureMessage);
        }
        if (sig == null) {
            logger.error("In signature message sig should not be null! {}", signatureMessage);
        }
        if (msg == null) {
            logger.error("In signature message address should not be null! {}", signatureMessage);
        }

        if (address == null || sig == null || msg == null) {
            return Optional.empty();
        } else {
            final String addressAsText = address.asText();
            if (!EthUtils.Addresses.isValidAddress(addressAsText)) {
                logger.error("Address field in {} doesn't contain a valid address", signatureMessage);
                return Optional.empty();
            } else {
                final String msgText = msg.asText();

                final String signingAddress;
                try {
                    final Sign.SignatureData signatureData = getSignatureData(sig.asText());
                    signingAddress = "0x" + Keys.getAddress(Sign.signedPrefixedMessageToKey(msgText.getBytes(), signatureData));
                } catch (DecoderException e) {
                    logger.error("Bad hex data in signature message", msg, sig);
                    logger.error("Error:", e);
                    return Optional.empty();
                } catch (SignatureException e) {
                    logger.error("Cannot find signing address for {}", signatureMessage);
                    logger.error("Error is:", e);
                    return Optional.empty();
                }

                return Optional.of(new AddressOwnershipMessageValidation(
                        msgText,
                        addressAsText,
                        signingAddress
                ));
            }
        }
    }
}
