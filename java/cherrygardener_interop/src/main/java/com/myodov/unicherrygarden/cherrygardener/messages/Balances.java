package com.myodov.unicherrygarden.cherrygardener.messages;

import akka.actor.typed.ActorRef;

import java.math.BigDecimal;

import static com.myodov.unicherrygarden.ethereum.EthUtils.Addresses;

public class Balances {
    public static final class GetBalance implements CherryGardenerRequest {
        public final ActorRef<GetBalanceResp> sender;
        public final String address;

        public GetBalance(ActorRef<GetBalanceResp> sender, String address) {
            assert Addresses.isValidAddress(address) : address;
            this.sender = sender;
            this.address = address;
        }

        public String toString() {
            return String.format("Balances.GetBalance(%s, %s)",
                    sender, address);
        }
    }

    public static final class GetBalanceResp implements CherryGardenerResponse {
        public final ActorRef<GetBalance> sender;
        public final BigDecimal value;

        public GetBalanceResp(ActorRef<GetBalance> sender, BigDecimal value) {
            this.sender = sender;
            this.value = value;
        }

        public String toString() {
            return String.format("Balances.GetBalanceResp(%s, %s)",
                    sender, value);
        }
    }
}