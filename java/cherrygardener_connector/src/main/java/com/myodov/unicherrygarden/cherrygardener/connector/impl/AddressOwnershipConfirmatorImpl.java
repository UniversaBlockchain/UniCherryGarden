package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import com.myodov.unicherrygarden.cherrygardener.connector.api.AddressOwnershipConfirmator;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.security.SignatureException;
import java.util.Optional;

public class AddressOwnershipConfirmatorImpl implements AddressOwnershipConfirmator {
    final Logger logger = LoggerFactory.getLogger(AddressOwnershipConfirmatorImpl.class);

    //    public boolean validateMessage(@NonNull String message, @NonNull String address) {
//        return false;
////        Sign.SignatureData
//    }

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

        final byte[] r = Hex.decode(sig.substring(2 + 0, 2 + 64));
        final byte[] s = Hex.decode(sig.substring(2 + 64, 2 + 128));
        final byte[] v = Hex.decode(sig.substring(2 + 128));


//        final String msg = "Testing your code is great!";
//        final String address = "0x6980ba0ab378c2ed0efccd7ea6ab84d54615a2de";
//        final String sig = "0xf08688e9dddbb5e4e0d1fb685ee9f693accb3c9aac84fdcf327423ca4a1c50463ef7aeb70be3221fe028bc752e210a4c377db8090bc4efa5ea7d391049c3a4771c";

        final Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);

        try {
            return Optional.of(
                    "0x" + Keys.getAddress(Sign.signedPrefixedMessageToKey(msg.getBytes(), signatureData))
            );
        } catch (SignatureException e) {
            logger.error("Cannot get message signer for msg {}, sig {}", msg, sig);
            logger.error("Error:", e);
            return Optional.empty();
        }
    }
}
