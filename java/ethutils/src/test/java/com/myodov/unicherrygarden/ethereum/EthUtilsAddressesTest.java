package com.myodov.unicherrygarden.ethereum;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EthUtilsAddressesTest {
    @Test
    public void testIsValidAddressHash() {
        assertTrue(
                "Regular address",
                EthUtils.Addresses.isValidAddress("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );

        assertFalse(
                "Too short",
                EthUtils.Addresses.isValidAddress("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2")
        );
        assertFalse(
                "Too long",
                EthUtils.Addresses.isValidAddress("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d247")
        );
    }

    @Test
    public void testIsValidLowercasedAddressHash() {
        assertTrue(
                "Regular address",
                EthUtils.Addresses.isValidLowercasedAddress("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );

        assertFalse(
                "Too short",
                EthUtils.Addresses.isValidLowercasedAddress("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d2")
        );
        assertFalse(
                "Too long",
                EthUtils.Addresses.isValidLowercasedAddress("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d247")
        );
        assertFalse(
                "Has uppercase",
                EthUtils.Addresses.isValidLowercasedAddress("0xD701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );
        assertFalse(
                "Has uppercase (even though is proper EIP55)",
                EthUtils.Addresses.isValidLowercasedAddress("0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24")
        );
    }

    @Test
    public void testIsValidEip55AddressHash() {
        assertTrue(
                "Good: Proper EIP55 address",
                EthUtils.Addresses.isValidEip55Address("0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24")
        );
        assertFalse(
                "Bad: Mismatched case",
                EthUtils.Addresses.isValidEip55Address("0xd701eDF8f9c5d834Bcb9Add73ddefF2D6B9C3D24")
        );
        assertFalse(
                "Bad: All lowercased",
                EthUtils.Addresses.isValidEip55Address("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
        );

        assertFalse(
                "Too short",
                EthUtils.Addresses.isValidAddress("0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D2")
        );
        assertFalse(
                "Too long",
                EthUtils.Addresses.isValidAddress("0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D247")
        );
    }
}
