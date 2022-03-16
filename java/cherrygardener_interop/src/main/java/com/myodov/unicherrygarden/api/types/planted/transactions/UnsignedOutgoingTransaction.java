package com.myodov.unicherrygarden.api.types.planted.transactions;

import com.myodov.unicherrygarden.api.Validators;
import com.myodov.unicherrygarden.api.types.PrivateKey;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

public class UnsignedOutgoingTransaction extends PreparedOutgoingTransaction {
    /**
     * Primary constructor (from byte array).
     */
    @SuppressWarnings("unused")
    public UnsignedOutgoingTransaction(byte[] bytes) {
        super(false, bytes);
    }

    /**
     * Secondary constructor (from {@link RawTransaction}).
     */
    @SuppressWarnings("unused")
    private UnsignedOutgoingTransaction(@NonNull RawTransaction rawTransaction) {
        this(TransactionEncoder.encode(rawTransaction));
    }

    @Override
    public String toString() {
        return String.format("%s(bytes=\"%s\")",
                getClass().getSimpleName(),
                getBytesHexString());
    }


    /**
     * Sign this transaction, using some private key.
     */
    @NonNull
    public final SignedOutgoingTransaction sign(@NonNull PrivateKey privateKey) {
        final byte[] signed = TransactionEncoder.signMessage(
                getRawTransaction(), privateKey.getCredentials());
        return new SignedOutgoingTransaction(signed);
    }


    /**
     * Create a transaction to transfer the base currency of the blockchain
     * (in case of Ethereum Mainnet, this is ETH; may be different for other blockchains,
     * but for simplicity of referring to it let's call it “Ether Transaction”).
     *
     * @param receiver       the receiver of the transaction; i.e. the “to” field.
     *                       Should be a valid Ethereum address, upper or lower case,
     *                       e.g. <code>"0x34e1E4F805fCdC936068A760b2C17BC62135b5AE"</code>.
     * @param amount         amount of currency (ETH in case of Ethereum Mainnet) to be transferred.
     *                       This is the “end-user-interpretation” of the amount, i.e. the real number of ETH
     *                       with decimal point, rather than internal uint256-based number of weis.
     * @param nonce          nonce for the transaction.
     * @param maxPriorityFee (EIP-1559) Max Priority Fee; measured in ETH, but in real scenarios
     *                       it is often measured in Gweis; see the counterpart method.
     * @param maxFee         (EIP-1559) Max Fee; measured in ETH, but in real scenarios
     *                       it is often measured in Gweis; see the counterpart method.
     * @apiNote Read <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1559.md">EIP-1559</a>
     * to get more details about Max Priority Fee and Max Fee.
     * Also, see the <a href="https://notes.ethereum.org/@vbuterin/eip-1559-faq">EIP-1559 FAQ</a>
     * for practical explanation.
     */
    public static UnsignedOutgoingTransaction createEtherTransfer(
            @NonNull String receiver,
            @NonNull BigDecimal amount,
            long chainId,
            @NonNull BigInteger nonce,
            @NonNull BigDecimal maxPriorityFee,
            @NonNull BigDecimal maxFee
    ) {
        Validators.requireValidEthereumAddress(receiver);
        assert amount != null && amount.compareTo(BigDecimal.ZERO) >= 0 : amount; // amount >= 0
        Validators.requireValidNonce(nonce);
        assert maxPriorityFee != null && maxPriorityFee.compareTo(BigDecimal.ZERO) >= 0 : maxPriorityFee; // maxPriorityFee >= 0
        assert maxFee != null && maxFee.compareTo(BigDecimal.ZERO) >= 0 : maxFee; // maxFee >= 0

        return new UnsignedOutgoingTransaction(RawTransaction.createEtherTransaction(
                chainId,
                nonce,
                EthUtils.ETH_TRANSFER_GAS_LIMIT_BIGINTEGER,
                receiver.toLowerCase(),
                EthUtils.Wei.valueToWeis(amount),
                EthUtils.Wei.valueToWeis(maxPriorityFee),
                EthUtils.Wei.valueToWeis(maxFee)
        ));
    }

    /**
     * Create a transaction to transfer the ERC20-formed token.
     *
     * @param receiver       the receiver of the transaction; i.e. the “to” field.
     *                       Should be a valid Ethereum address, upper or lower case,
     *                       e.g. <code>"0x34e1E4F805fCdC936068A760b2C17BC62135b5AE"</code>.
     * @param amountUint256  amount of token (not corrected for decimals) to be transferred.
     *                       This is low-level uint256-based number to be recorded.
     * @param nonce          nonce for the transaction.
     * @param gasLimit       gas limit to use.
     * @param maxPriorityFee (EIP-1559) Max Priority Fee; measured in ETH, but in real scenarios
     *                       it is often measured in Gweis.
     * @param maxFee         (EIP-1559) Max Fee; measured in ETH, but in real scenarios
     *                       it is often measured in Gweis.
     * @apiNote Read <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1559.md">EIP-1559</a>
     * to get more details about Max Priority Fee and Max Fee.
     * Also, see the <a href="https://notes.ethereum.org/@vbuterin/eip-1559-faq">EIP-1559 FAQ</a>
     * for practical explanation.
     */
    static UnsignedOutgoingTransaction createERC20Transfer(
            @NonNull String receiver,
            @NonNull BigInteger amountUint256,
            @NonNull String erc20TokenAddress,
            long chainId,
            @NonNull BigInteger nonce,
            @NonNull BigInteger gasLimit,
            @NonNull BigDecimal maxPriorityFee,
            @NonNull BigDecimal maxFee
    ) {
        Validators.requireValidEthereumAddress(receiver);
        assert amountUint256 != null && amountUint256.compareTo(BigInteger.ZERO) >= 0 : amountUint256; // amountUint256 >= 0
        Validators.requireValidLowercasedEthereumAddresses(erc20TokenAddress);
        Validators.requireValidNonce(nonce);
        assert maxPriorityFee != null && maxPriorityFee.compareTo(BigDecimal.ZERO) >= 0 : maxPriorityFee; // maxPriorityFee >= 0
        assert maxFee != null && maxFee.compareTo(BigDecimal.ZERO) >= 0 : maxFee; // maxFee >= 0

        final Function transferFunction = new Function(
                ERC20.FUNC_TRANSFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(receiver.toLowerCase()),
                        new org.web3j.abi.datatypes.generated.Uint256(amountUint256)),
                Collections.<TypeReference<?>>emptyList());

        return new UnsignedOutgoingTransaction(RawTransaction.createTransaction(
                chainId,
                nonce,
                gasLimit,
                erc20TokenAddress,
                BigInteger.ZERO,
                FunctionEncoder.encode(transferFunction),
                EthUtils.Wei.valueToWeis(maxPriorityFee),
                EthUtils.Wei.valueToWeis(maxFee)
        ));
    }

    /**
     * Create a transaction to transfer the ERC20-formed token.
     *
     * @param receiver       the receiver of the transaction; i.e. the “to” field.
     *                       Should be a valid Ethereum address, upper or lower case,
     *                       e.g. <code>"0x34e1E4F805fCdC936068A760b2C17BC62135b5AE"</code>.
     * @param amount         amount of token (not corrected for decimals) to be transferred.
     *                       This is “end-user-interpretation” of the amount, i.e. the real number of token
     *                       with decimal point.
     * @param decimals       number of “decimals” for amount decimal point correction.
     * @param nonce          nonce for the transaction.
     * @param maxPriorityFee (EIP-1559) Max Priority Fee; measured in ETH, but in real scenarios
     *                       it is often measured in Gweis.
     * @param maxFee         (EIP-1559) Max Fee; measured in ETH, but in real scenarios
     *                       it is often measured in Gweis.
     * @apiNote Read <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-1559.md">EIP-1559</a>
     * to get more details about Max Priority Fee and Max Fee.
     * Also, see the <a href="https://notes.ethereum.org/@vbuterin/eip-1559-faq">EIP-1559 FAQ</a>
     * for practical explanation.
     */
    public static UnsignedOutgoingTransaction createERC20Transfer(
            @NonNull String receiver,
            @NonNull BigDecimal amount,
            int decimals,
            @NonNull String erc20TokenAddress,
            long chainId,
            @NonNull BigInteger nonce,
            @NonNull BigInteger gasLimit,
            @NonNull BigDecimal maxPriorityFee,
            @NonNull BigDecimal maxFee
    ) {
        assert decimals >= 0 : decimals;
        return createERC20Transfer(
                receiver,
                EthUtils.Uint256.valueToUint256(amount, decimals),
                erc20TokenAddress,
                chainId,
                nonce,
                gasLimit,
                maxPriorityFee,
                maxFee
        );
    }
}
