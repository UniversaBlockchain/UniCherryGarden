package com.myodov.unicherrygarden.cherrygardener.connector.api;

import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The client connector part that is capable to confirm that somebody indeed owns (controls) some Ethereum address.
 * <p>
 * The typical procedure to validate the address is following:
 * <p>
 * <ol>
 *     <li>You should build a message you want the user (private key/Ethereum address owner) to sign.
 *     Typically this message should contain something that explicitly identifies your service
 *     (to prevent using this signature for other services); something that identifies the user (if you need it),
 *     for the KYC-like purposes; explanation of the signing process purpose; maybe, the time of signing operation;
 *     and other similar information.
 *     </li>
 *     <ul>
 *         <li>A typical message you build can look like this:
 *         “<b>I, John Doe, on 31/12/2021 confirm that I own and control the address
 *         0x34e1E4F805fCdC936068A760b2C17BC62135b5AE for the needs of CherryGarden service.</b>”.
 *         </li>
 *         <li>The message (which you build) should be either stored in your database (to validate
 *         it through equivalence) or be parseable on your side.
 *         You choose your own method how to validate the message contents – but you need to validate
 *         the contents too, not just the signature. And you need to validate the contents yourself, preferably
 *         even before the signature is verified.</li>
 *         <li>Note that the message can be multi-line, and each byte of it matters.
 *         An extra space or extra newline symbol leads to a different signature.</li>
 *     </ul>
 *     <li>You should show the message to the user, and ask user to sign this message using the user’s key
 *     for the address. A hint text could be like this (and of course it should include the text to sign,
 *     in such a way that the user can easily copy it to clipboard, not losing even a single symbol):
 *     “<b>Please sign the message below. In MyEtherWallet or MyCrypto, you should use the Sign Message dialog,
 *     the other Ethereum clients may be different.
 *     Copy the text of the message, from the first to the last symbol, and insert it in the Sign Message dialog
 *     of your Ethereum client. Sign it using the private key for the address, mentioned in the message
 *     (signing it using any other private key will make a mismatching signature). Copy the result signature.</b>”.
 *     </li>
 *     <li>Your workflow should then receive the result signature from the user’s Ethereum client.
 *     The signature is a JSON-like structure, which normally looks somewhat like this:
 *     <pre>
 * {
 *     "address": "0x34e1e4f805fcdc936068a760b2c17bc62135b5ae",
 *     "msg": "I, John Doe, on 31/12/2021 confirm that I own and control the address 0x34e1E4F805fCdC936068A760b2C17BC62135b5AE for the needs of CherryGarden service.",
 *     "sig": "0xb2f88840c6895a12c1ea114cf73fbfcf4b276470cf1165ee10f2397ae7006c882a3cfbd95080f0ff03d808e81d472305e2a95d3eb5f4316e55b4edfcb10b38581b",
 *     "version": "2"
 * }
 *     </pre>
 *     You should validate this signature using the methods of
 *     {@link AddressOwnershipConfirmator}. Please make sure to validate the <code>msg</code> field
 *     (using your preferred validation logic – either binary matching of the message you have stored before,
 *     or parsing the message body), the <code>address</code> field (that it matches the expected address),
 *     and also the address which has signed the message. That is, e.g. if you are expecting to check
 *     that the user controls the address <b>0xABCDEF…00</b>, your code must validate:
 *     <ol>
 *         <li>The <code>address</code> field of the signature is <b>0xabcdef…00</b> (normally it is in lowercase).</li>
 *         <li>If you mentioned the address in the signed message – the <code>msg</code> field contains
 *         the exact address you mentioned, and is valid in terms of your validation;</li>
 *         <li>{@link AddressOwnershipConfirmator} validation methods confirmed that the message
 *         is signed by <b>0xabcdef…00</b> private key.</li>
 *     </ol>
 *     </li>
 * </ol>
 */
public interface AddressOwnershipConfirmator {
    /**
     * Get the address that signed the <code>msg</code> message, from the according <code>sig</code> signature.
     *
     * @return The <code>null</code> if the signer cannot be retrieved: wrong or spoofed signature,
     * signature mismatches the message, or for some other error (most likely, on the customer side).
     */
    @Nullable
    String getMessageSigner(@NonNull String msg, @NonNull String sig);

    class AddressOwnershipMessageValidation {
        /**
         * The message to be signed.
         */
        @NonNull
        public final String message;

        /**
         * The address declared in the signature.
         */
        @NonNull
        public final String declaredAddress;

        /**
         * The address that actually .
         */
        @NonNull
        public final String signingAddress;

        public AddressOwnershipMessageValidation(@NonNull String message,
                                                 @NonNull String declaredAddress,
                                                 @NonNull String signingAddress) {
            assert message != null;

            assert declaredAddress != null;
            assert EthUtils.Addresses.isValidAddress(declaredAddress) : declaredAddress;

            assert signingAddress != null;
            assert EthUtils.Addresses.isValidAddress(signingAddress) : signingAddress;

            this.message = message;
            this.declaredAddress = declaredAddress;
            this.signingAddress = signingAddress;
        }

        /**
         * Whether the address declared in the signature actually matches the address
         * that signed the message.
         */
        public boolean addressIsMatching() {
            return signingAddress.equals(declaredAddress);
        }

        @Override
        public String toString() {
            return String.format("AddressOwnershipConfirmator.AddressOwnershipMessageValidation(%s)",
                    addressIsMatching() ? "valid" : "invalid");
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
     * Returns:
     *
     * <ul>
     *     <li>An {@link AddressOwnershipMessageValidation} structure where you can reach the details of the message.</li>
     *     <li><code>null</code> if the message cannot be properly parsed (and is probably malformed).</li>
     * </ul>
     *
     * <p>
     * You may want:
     * <ol>
     * <li>Check the {@link AddressOwnershipMessageValidation#message} field to be sure the message contents
     * contains what you need.</li>
     * <li>Check the {@link AddressOwnershipMessageValidation#declaredAddress} and/or
     * {@link AddressOwnershipMessageValidation#signingAddress}</li> that it matches the address you
     * </ol>
     */
    @Nullable
    AddressOwnershipMessageValidation validateMessage(@NonNull String signatureMessage);
}
