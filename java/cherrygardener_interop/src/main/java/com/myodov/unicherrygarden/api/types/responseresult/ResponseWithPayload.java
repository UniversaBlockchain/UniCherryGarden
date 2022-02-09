package com.myodov.unicherrygarden.api.types.responseresult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.api.types.UniCherryGardenFailure;
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload.SpecificFailurePayload;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Error handling policy in UniCherryGarden, and UniCherryGarden connector:
 *
 * <ol>
 * <li>
 * If something works locally (without any networking communication e.g. with UniCherryGarden cluster members),
 * it may return a regular Java type/class (it may even be nullable, depending on circumstances).<br>
 * If something works in networking configuration, it returns a instance of {@link ResponseWithPayload}.
 * {@link ResponseWithPayload} has enough methods to understand whether the result was successful or not,
 * and how to interpret the payload. But you may just use {@link ResponseWithPayload#getPayload} and,
 * depending on what class it is, use it accordingly.
 * <br>
 * Example 1: an operation to sign a message with a private key happens completely in client memory space,
 * hence it may return just the needed Java classes.<br>
 * Example 2: a request to get the list of currencies/assets, supported by the network instance of UniCherryGarden,
 * involves a network call to CherryGardener. Hence the result is provided via {@link ResponseWithPayload}.
 * </li>
 * <li>
 * If a problem occurred due to wrong nullability of arguments, you should except an NPE or {@link AssertionError}.
 * To prevent this, when using the code from Kotlin that is marked
 * with {@link NonNull} decorator, declare it as “<code>Type</code>”;<br>
 * When using the code from Kotlin that is marked
 * with {@link Nullable} decorator, declare it as “<code>Type?</code>”.
 * </li>
 * <li>
 * If a problem happened due to the bad arguments (violating the requirements usually defined in the Javadoc),
 * and could be prevented by changing the code,
 * then, according to the Java Error/Exception best practices, the code throws an {@link Error},
 * actually a subclass of {@link UniCherryGardenError}.
 * Do not catch it, fix the code!<br>
 * If a problem occurred deeper during the network processing/handling, probably on the server side,
 * the result will not throw an exception, but it will be {@link #isFailure()}, and payload will be an instance of
 * {@link FailurePayload}.
 * You may want to handle this problem, getting more specific or less specific into the details, as you wish.
 *   <ul>
 *     <li>
 *     Some problems can be common to many requests, e.g. calling UniCherryGarden which is in a state when it cannot
 *     handle the queries.
 *     The payload will be an instance of {@link FailurePayload.CommonFailurePayload}.
 *     You can get it directly as {@link #getCommonFailure()}, but only if {@link #isCommonFailure()}.
 *     </li>
 *     <li>
 *     Some problems can be specific to the request type.
 *     The payload will be an instance of {@link FailurePayload.SpecificFailurePayload}.
 *     You can get it directly as {@link #getSpecificFailure()}, but only if {@link #isSpecificFailure()}.
 *     </li>
 *   </ul>
 * </li>
 * <li>
 * If no problem occurred, the result payload will be an instance of {@link SuccessPayload}.
 * You can get it directly as {@link #getPayloadAsSuccessful()} but only if {@link #isSuccess()}.
 * <br>
 * Example 1: a request to add a tracked address resulted in UniCherryGarden (UniCherryPicker) replying
 * that such address is registered already. This is not a error or even a problem (you are not obliged to check
 * if the address is registered before an attempt to add – EAFP!); the payload will be a {@link SuccessPayload},
 * with {@link #getPayloadAsSuccessful()} informing you that the address was not actually added.<br>
 * Example 2: a request to get the balances came to UniCherryGarden which is not even configured yet
 * and not collecting the data. This is a common error which doesn’t allow almost any calls to be used;
 * the response will be an instance of {@link FailurePayload},
 * and even more specific, of {@link FailurePayload.CommonFailurePayload}.
 * </li>.
 * </ol>
 */
public interface ResponseWithPayload<
        Payload extends SuccessPayload,
        Failure extends SpecificFailurePayload> {

    /**
     * Type of the result.
     * <p>
     * Most of the times you don’t want to use this method, use {@link #isSuccess} or {@link #isFailure} instead.
     */
    @JsonIgnore
    default ResponsePayload.@NonNull Type getType() {
        return getPayload().getType();
    }

    /**
     * Whether request was successful.
     * Use {@link #getPayloadAsSuccessful()} to get more details if available.
     * Do not use {@link #getCommonFailure()}!
     * <p>
     * Antonym: {@link #isFailure()}.
     */
    @JsonIgnore
    default boolean isSuccess() {
        return getType() == ResponsePayload.Type.SUCCESS;
    }

    /**
     * Whether request has failed.
     * Use {@link #getCommonFailure()} to get more details if available.
     * Do not use {@link #getPayloadAsSuccessful()}!
     * <p>
     * Antonym: {@link #isSuccess()}.
     */
    @JsonIgnore
    default boolean isFailure() {
        final ResponsePayload.Type type = getType();
        return type == ResponsePayload.Type.FAILURE_COMMON || type == ResponsePayload.Type.FAILURE_SPECIFIC;
    }

    /**
     * Get the payload of request execution.
     * <p>
     * LBYL-way: response may be successful or not, it is your duty to check results of
     * {@link #isSuccess()} / {@link #isFailure()} or the class of payload object.
     */
    @NonNull
    ResponsePayload getPayload();

    /**
     * Get the payload of successful request execution; the payload contents is specific to the request.
     * <p>
     * Works only if {@link #isSuccess()}. Otherwise, use {@link #getFailure()}.
     *
     * @throws UniCherryGardenFailure (typed with the details of failure, common or specific)
     *                                if called not when {@link #isSuccess()}, so you can use it in EAFP way.
     */
    @JsonIgnore
    @NonNull
    Payload getPayloadAsSuccessful() throws UniCherryGardenFailure;

    /**
     * Shortcut to {@link #getPayloadAsSuccessful} – use it if you prefer to get the results in EAFP way,
     * and handle the exception if any failure occured.
     *
     * @throws UniCherryGardenFailure (typed with the details of failure, common or specific)
     *                                if called not when {@link #isSuccess()}.
     */
    @JsonIgnore
    @NonNull
    default Payload get() throws UniCherryGardenFailure {
        return getPayloadAsSuccessful();
    }

    /**
     * Get the payload of failed request execution;
     * the data is a “failure object”, either a common failure or specific one.
     * <p>
     * Works only if {@link #isFailure()}. Otherwise, use {@link #getPayloadAsSuccessful()}.
     *
     * @throws ClassCastException if called not when {@link #isFailure()}.
     */
    @JsonIgnore
    @NonNull
    default FailurePayload getFailure() {
        return (FailurePayload) getPayload();
    }

    /**
     * Whether request has failed, and failure is common (may happen with different requests).
     * Use {@link #getCommonFailure()} to get more details if available.
     * Do not use {@link #getPayloadAsSuccessful()}!
     */
    @JsonIgnore
    default boolean isCommonFailure() {
        return getType() == ResponsePayload.Type.FAILURE_COMMON;
    }

    /**
     * Whether request has failed, and failure is specific to the request.
     * Use {@link #getSpecificFailure()} to get more details if available.
     * Do not use {@link #getPayloadAsSuccessful()}!
     */
    @JsonIgnore
    default boolean isSpecificFailure() {
        return getType() == ResponsePayload.Type.FAILURE_SPECIFIC;
    }

    /**
     * Get the data of request failure; the data is common to the requests.
     * <p>
     * Works only if {@link #isFailure()}, and moreover, if not {@link #isSpecificFailure()}.
     *
     * @throws ClassCastException if called not when {@link #isCommonFailure()}.
     */
    @JsonIgnore
    FailurePayload.@NonNull CommonFailurePayload getCommonFailure();

    /**
     * Get the data of request failure; the data is specific to the request.
     * <p>
     * Available only if {@link #isFailure()} and moreover, if {@link #isSpecificFailure()}.
     *
     * @throws ClassCastException if called not when {@link #isSpecificFailure()} ()}.
     */
    @JsonIgnore
    @NonNull
    Failure getSpecificFailure();
}
