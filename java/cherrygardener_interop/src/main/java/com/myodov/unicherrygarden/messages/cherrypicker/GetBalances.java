package com.myodov.unicherrygarden.messages.cherrypicker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.SystemSyncStatus;
import com.myodov.unicherrygarden.api.types.dlt.Currency;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import com.myodov.unicherrygarden.messages.CherryPickerRequest;
import com.myodov.unicherrygarden.messages.CherryPickerResponseWithResult;
import com.myodov.unicherrygarden.messages.RequestPayload;
import com.myodov.unicherrygarden.messages.RequestWithReplyTo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class GetBalances {
    @NonNull
    public static final ServiceKey<Request> SERVICE_KEY =
            ServiceKey.create(Request.class, "getBalancesService");


    public static final class GBRequestPayload
            implements RequestPayload {
        @NonNull
        public final int confirmations;

        @NonNull
        public final String address;

        @Nullable
        public final Set<String> filterCurrencyKeys;


        @JsonCreator
        public GBRequestPayload(int confirmations,
                                @NonNull String address,
                                @Nullable Set<String> filterCurrencyKeys) {
            assert confirmations >= 0 : confirmations;
            assert address != null && EthUtils.Addresses.isValidLowercasedAddress(address) : address;
            // If filterCurrencyKeys is present, it is either a `null`;
            // or, if it is not `null` - it is a set that doesn't contain nulls; and each item of the set
            // is either an empty string (for ETH) or valid lowercased address.
            assert filterCurrencyKeys == null ||
                    filterCurrencyKeys
                            .stream()
                            .allMatch(addr -> (addr != null) && (addr.isEmpty() || EthUtils.Addresses.isValidLowercasedAddress(addr)))
                    :
                    filterCurrencyKeys;

            this.confirmations = confirmations;
            this.address = address;
            this.filterCurrencyKeys = filterCurrencyKeys;
        }

        @Override
        public String toString() {
            return String.format("GetBalances.GBRequestPayload(%s, %s, %s)",
                    confirmations, address, filterCurrencyKeys);
        }
    }


    public static final class Request
            extends RequestWithReplyTo<GBRequestPayload, Response>
            implements CherryPickerRequest {
        @JsonCreator
        public Request(@NonNull ActorRef<Response> replyTo,
                       @NonNull GBRequestPayload payload) {
            super(replyTo, payload);
        }
    }


    public static class BalanceRequestResult {

        public static class CurrencyBalanceFact {
            /**
             * The currency for which the balance is retrieved.
             */
            @NonNull
            public final Currency currency;

            /**
             * The amount of the balance.
             * Always non-null; always non-negative.
             * Even if some error occurred, as a safe default it will contain <code>0</code>.
             */
            @NonNull
            public final BigDecimal amount;

            /**
             * The information is actual to this block number.
             * <p>
             * If present, always 0 or higher.
             */
            public final int blockNumber;


            @JsonCreator
            public CurrencyBalanceFact(@NonNull Currency currency,
                                       @NonNull BigDecimal amount,
                                       int blockNumber) {
                assert currency != null;
                assert blockNumber >= 0 : blockNumber;

                this.currency = currency;
                this.amount = amount;
                this.blockNumber = blockNumber;
            }

            @Override
            public String toString() {
                return String.format("CurrencyBalanceFact(%s: amount=%s at block %s)",
                        currency, amount, blockNumber);
            }
        }

        /**
         * The total status of blockchain synchronization.
         */
        @NonNull
        public final SystemSyncStatus syncStatus;

        @NonNull
        public final List<CurrencyBalanceFact> balances;

        /**
         * Constructor.
         */
        @JsonCreator
        public BalanceRequestResult(@NonNull SystemSyncStatus syncStatus,
                                    @NonNull List<CurrencyBalanceFact> balances) {
            assert syncStatus != null;
            assert balances != null;

            this.syncStatus = syncStatus;
            this.balances = Collections.unmodifiableList(balances);
        }

        @Override
        public String toString() {
            return String.format("BalanceRequestResult.BalanceRequestResult(%s, %s)",
                    syncStatus, balances);
        }
    }


    public static final class Response extends CherryPickerResponseWithResult<BalanceRequestResult> {
        @JsonCreator
        public Response(@Nullable BalanceRequestResult result) {
            super(result);
        }

        @NonNull
        public static Response failed() {
            return new Response(null);
        }
    }
}
