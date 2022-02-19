package com.myodov.unicherrygarden.messages.cherrygardener;

import com.myodov.unicherrygarden.AbstractJacksonSerializationTest;
import com.myodov.unicherrygarden.messages.cherrypicker.GetAddressDetails;
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses;
import org.junit.Test;

import java.io.IOException;

public class GetAddressDetails_ResponseTest extends AbstractJacksonSerializationTest {
    @Test
    public void testJacksonSerialization() throws IOException {
        // Response
        assertJsonDeserialization(
                new GetAddressDetails.Response(new GetAddressDetails.AddressDetailsRequestResultPayload(
                        new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                new GetTrackedAddresses.TrackedAddressesRequestResultPayload.TrackedAddressInformation(
                                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                        "Some comment",
                                        18374
                                ),
                                new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.Nonces(
                                        18350,
                                        18352,
                                        18354
                                )
                        )
                )),
                "{\"payload\":{" +
                        "\"@class\":\"com.myodov.unicherrygarden.messages.cherrypicker.GetAddressDetails$AddressDetailsRequestResultPayload\"," +
                        "\"details\":{" +
                        "\"address\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24\"," +
                        "\"trackedAddressInformation\":{\"address\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24\",\"comment\":\"Some comment\",\"syncedFrom\":18374}," +
                        "\"nonces\":{\"nextInBlockchain\":18350,\"nextInPendingPool\":18352,\"nextPlanting\":18354}}" +
                        "}}",
                GetAddressDetails.Response.class
        );

        // Just the payload
        assertJsonDeserialization(
                new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                        new GetTrackedAddresses.TrackedAddressesRequestResultPayload.TrackedAddressInformation(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                                "Some comment",
                                18374
                        ),
                        new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.Nonces(
                                18350,
                                18352,
                                18354
                        )
                ),
                "{\"address\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24\"," +
                        "\"trackedAddressInformation\":{\"address\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24\",\"comment\":\"Some comment\",\"syncedFrom\":18374}," +
                        "\"nonces\":{\"nextInBlockchain\":18350,\"nextInPendingPool\":18352,\"nextPlanting\":18354}}",
                GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.class
        );

        // Payload with all nulls where nullable (but address is tracked)
        assertJsonDeserialization(
                new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d25",
                        new GetTrackedAddresses.TrackedAddressesRequestResultPayload.TrackedAddressInformation(
                                "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d25",
                                null,
                                null
                        ),
                        new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.Nonces(
                                18355,
                                null,
                                null
                        )
                ),
                "{\"address\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d25\"," +
                        "\"trackedAddressInformation\":{\"address\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d25\",\"comment\":null,\"syncedFrom\":null}," +
                        "\"nonces\":{\"nextInBlockchain\":18355,\"nextInPendingPool\":null,\"nextPlanting\":null}}",
                GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.class
        );

        // Payload with all nulls where nullable (and address is untracked)
        assertJsonDeserialization(
                new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails(
                        "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d26",
                        null,
                        new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.Nonces(
                                18360,
                                null,
                                null
                        )
                ),
                "{\"address\":\"0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d26\"," +
                        "\"trackedAddressInformation\":null," +
                        "\"nonces\":{\"nextInBlockchain\":18360,\"nextInPendingPool\":null,\"nextPlanting\":null}}",
                GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.class
        );
    }
}
