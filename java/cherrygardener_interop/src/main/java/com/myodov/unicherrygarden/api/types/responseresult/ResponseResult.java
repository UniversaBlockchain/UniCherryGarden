package com.myodov.unicherrygarden.api.types.responseresult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Error handling policy in UniCherryGarden, and UniCherryGarden connector:
 *
 * <ol>
 * <li>
 * If something works locally (without any networking communication e.g. with UniCherryGarden cluster members),
 * it may return a regular Java type/class (it may even be nullable, depending on circumstances).<br>
 * If something works in networking configuration, it returns a instance of {@link ResponseResult}.
 * <br>
 * Example 1: an operation to sign a message with a private key happens completely in client memory space,
 * hence it may return just the needed Java classes.<br>
 * Example 2: a request to get the list of currencies/assets, supported by the network instance of UniCherryGarden,
 * involves a network call to CherryGardener. Hence the result is provided via {@link ResponseResult}.
 * </li>
 * <li>
 * If a problem occurred due to wrong nullability of arguments, you should except an NPE or {@link AssertionError}.
 * To prevent this, when using the code from Kotlin that is marked
 * with {@link org.checkerframework.checker.nullness.qual.NonNull} decorator, declare it as “<code>Type</code>”;<br>
 * When using the code from Kotlin that is marked
 * with {@link org.checkerframework.checker.nullness.qual.Nullable} decorator, declare it as “<code>Type?</code>”.
 * </li>
 * <li>
 * If a problem happened due to the bad arguments, and could be prevented by changing the code,
 * then, according to the Java Error/Exception best practices, the code throws an {@link Error},
 * actually a subclass of {@link UniCherryGardenError}.
 * Do not catch it, fix the code!<br>
 * If a problem occurred deeper during the network processing/handling, probably on the server side,
 * the result will not throw an exception, but it will be an instance of {@link FailureResponse}.
 * {@link #getCommonFailure()} call will provide the details.
 * You may want to handle this problem, getting more specific or less specific into the details, as you wish.
 *   <ul>
 *     <li>
 *     Some problems can be common to many requests, e.g. calling UniCherryGarden which is in a state when it cannot
 *     handle the queries.
 *     The result will be an instance of {@link FailureResponse.CommonFailureResponse}.
 *     </li>
 *     <li>
 *     Some problems can be specific to the request type.
 *     The result will be an instance of {@link FailureResponse.SpecificFailureResponse}.
 *     </li>
 *   </ul>
 * </li>
 * <li>
 * If no problem occurred, the result will be an instance of {@link SuccessResponse}.
 * {@link #getPayload()} call will provide the details.
 * You will want to handle this as success.
 * <br>
 * Example 1: a request to add a tracked address resulted in UniCherryGarden (UniCherryPicker) replying
 * that such address is registered already. This is not a error or even a problem (you are not obliged to check
 * if the address is registered before an attempt to add – EAFP!); the response will be a {@link SuccessResponse},
 * with {@link #getPayload()} informing you that the address was not actually added.<br>
 * Example 2: a request to get the balances came to UniCherryGarden which is not even configured yet
 * and not collecting the data. This is a common error which doesn’t allow almost any calls to be used;
 * the response will be an instance of {@link FailureResponse}, and even more specific, of {@link FailureResponse.CommonFailureResponse}.
 * </li>.
 * </ol>
 */
public interface ResponseResult<
        Payload extends SuccessPayload,
        Failure extends SpecificFailurePayload> {
    enum Type {
        SUCCESS,
        FAILURE_COMMON,
        FAILURE_SPECIFIC
    }

    /**
     * Type of the result.
     * <p>
     * Most of the times you don’t want to use this method, use {@link #isSuccess} or {@link #isFailure} instead.
     */
    @JsonIgnore
    @NonNull
    Type getType();

    /**
     * Whether request was successful.
     * Use {@link #getPayload()} to get more details if available.
     * Do not use {@link #getCommonFailure()}!
     * <p>
     * Antonym: {@link #isFailure()}.
     */
    @JsonIgnore
    default boolean isSuccess() {
        return getType() == Type.SUCCESS;
    }

    /**
     * Get the payload of successful request execution; the payload contents is specific to the request.
     * <p>
     * Available only if {@link #isSuccess()}.
     */
    Payload getPayload();

    /**
     * Whether request has failed.
     * Use {@link #getCommonFailure()} to get more details if available.
     * Do not use {@link #getPayload()}!
     * <p>
     * Antonym: {@link #isSuccess()}.
     */
    @JsonIgnore
    default boolean isFailure() {
        return getType() == Type.FAILURE_COMMON || getType() == Type.FAILURE_SPECIFIC;
    }

    /**
     * Whether request has failed, and failure is common (may happen with different requests).
     * Use {@link #getCommonFailure()} to get more details if available.
     * Do not use {@link #getPayload()}!
     */
    default boolean isCommonFailure() {
        return getType() == Type.FAILURE_COMMON;
    }

    /**
     * Whether request has failed, and failure is specific to the request.
     * Use {@link #getSpecificFailure()} to get more details if available.
     * Do not use {@link #getPayload()}!
     */
    default boolean isSpecificFailure() {
        return getType() == Type.FAILURE_SPECIFIC;
    }

    /**
     * Get the data of request failure; the data is common to the requests.
     * <p>
     * Available only if {@link #isFailure()}.
     */
    FailurePayload getCommonFailure();

    /**
     * Get the data of request failure; the data is specific to the request.
     * <p>
     * Available only if {@link #isFailure()} and moreover, if {@link #isSpecificFailure()}.
     */
    Failure getSpecificFailure();
}
