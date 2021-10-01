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

/**
 * The default implementation for {@link AddressOwnershipConfirmator} interface.
 */
public class AddressOwnershipConfirmatorImpl implements AddressOwnershipConfirmator {
    final Logger logger = LoggerFactory.getLogger(AddressOwnershipConfirmatorImpl.class);

    private static Sign.SignatureData getSignatureData(@NonNull String sig) {
        final byte[] r = Hex.decode(sig.substring(2 + 0, 2 + 64));
        final byte[] s = Hex.decode(sig.substring(2 + 64, 2 + 128));
        final byte[] v = Hex.decode(sig.substring(2 + 128));
        return new Sign.SignatureData(v, r, s);
    }

    @Nullable
    public String getMessageSigner(@NonNull String msg, @NonNull String sig) {
        if ((msg == null)
                || (sig == null)
                || (sig.length() != 132)
                || !sig.matches("^0x\\p{XDigit}+$")
        ) {
            logger.error("Malformed input to getMessageSigner: msg {}, sig {}", msg, sig);
            return null;
        }

        try {
            final Sign.SignatureData signatureData = getSignatureData(sig);
            return "0x" + Keys.getAddress(Sign.signedPrefixedMessageToKey(msg.getBytes(), signatureData));
        } catch (DecoderException e) {
            logger.error("Bad hex data in signature message", msg, sig);
            logger.error("Error:", e);
            return null;
        } catch (SignatureException e) {
            logger.error("Cannot get message signer for msg {}, sig {}", msg, sig);
            logger.error("Error:", e);
            return null;
        }
    }

    @Override
    @Nullable
    public AddressOwnershipMessageValidation validateMessage(@NonNull String signatureMessage) {
        final JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(signatureMessage);
        } catch (JsonProcessingException e) {
            logger.error("Cannot parse signature message: {}", signatureMessage);
            logger.error("Error is:", e);
            return null;
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
            return null;
        } else {
            final String addressAsText = address.asText();
            if (!EthUtils.Addresses.isValidAddress(addressAsText)) {
                logger.error("Address field in {} doesn't contain a valid address", signatureMessage);
                return null;
            } else {
                final String
                        msgText = msg.asText(),
                        sigText = sig.asText();

                final @Nullable String signingAddress = getMessageSigner(msgText, sigText);
                if (signingAddress == null) {
                    return null;
                } else {
                    return new AddressOwnershipMessageValidation(
                            msgText,
                            addressAsText,
                            signingAddress
                    );
                }
            }
        }
    }
}
