package com.myodov.unicherrygarden.api.types;

import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Any known failure occured during the call.
 */
public class UniCherryGardenFailure extends Exception {
    @NonNull
    private final FailurePayload failure;

    /**
     * Default constructor.
     */
    public UniCherryGardenFailure(@NonNull FailurePayload failure) {
        this.failure = failure;
    }

    @Override
    public String toString() {
        return String.format("UniCherryGardenFailure(failure=%s)", failure);
    }

    @NonNull
    public final FailurePayload getFailure() {
        return failure;
    }
}
