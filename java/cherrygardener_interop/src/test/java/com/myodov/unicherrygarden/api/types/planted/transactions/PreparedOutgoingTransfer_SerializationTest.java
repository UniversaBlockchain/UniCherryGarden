package com.myodov.unicherrygarden.api.types.planted.transactions;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.io.IOException;

public class PreparedOutgoingTransfer_SerializationTest extends AbstractJacksonSerializationTest {
    @Test
    public void testJacksonSerialization() throws IOException {
        assertJsonSerialization(
                "{\"bytes\":\"AuwEB4R3NZQAhO5rKACCUgiUQIpKwOgLpXIQ6mqa5qmntoelECOFF0h26ACAwA==\"}",
                new PreparedOutgoingTransaction(false, Hex.decode("02ec0407847735940084ee6b280082520894408a4ac0e80ba57210ea6a9ae6a9a7b687a5102385174876e80080c0")),
                PreparedOutgoingTransaction.class
        );
        assertJsonSerialization(
                "{\"bytes\":\"AvhvBAeEdzWUAITuaygAglIIlECKSsDoC6VyEOpqmuapp7aHpRAjhRdIdugAgMABoElWDqALD+yOKbNzIROr/7rXmqIghCFbmoDo56M3kKUDoF9GlTwI2Tb22U+6UXIEEcBwYMCYLmC+iUFhsvafSHL0\"}",
                new PreparedOutgoingTransaction(true, Hex.decode("02f86f0407847735940084ee6b280082520894408a4ac0e80ba57210ea6a9ae6a9a7b687a5102385174876e80080c001a049560ea00b0fec8e29b3732113abffbad79aa22084215b9a80e8e7a33790a503a05f46953c08d936f6d94fba51720411c07060c0982e60be894161b2f69f4872f4")),
                PreparedOutgoingTransaction.class
        );
    }
}
