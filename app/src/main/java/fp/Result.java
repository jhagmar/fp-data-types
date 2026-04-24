package fp;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A discriminated union representing the result of an operation that can either
 * succeed with a value of type {@code T}, or fail with an error of type
 * {@code E}.
 * <p>
 * This interface is heavily inspired by Rust's {@code Result} type and provides
 * a functional, type-safe alternative to throwing exceptions. By wrapping both
 * the success and failure paths in a single return type, it forces the caller
 * to explicitly handle the error case.
 * <p>
 * {@code Result} instances are immutable and do not permit {@code null} values
 * or errors.
 *
 * @param <T> the type of the value in the case of success
 * @param <E> the type of the error in the case of failure
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    /**
     * Creates a successful {@code Result} containing the provided value.
     *
     * @param value the success value; must not be null
     * @param <T>   the type of the success value
     * @param <E>   the type of the error
     * @return an {@code Ok} variant of {@code Result}
     * @throws NullPointerException if the value is null
     */
    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(Objects.requireNonNull(value, "Success value cannot be null"));
    }

    /**
     * Creates a failed {@code Result} containing the provided error.
     *
     * @param error the error value; must not be null
     * @param <T>   the type of the success value
     * @param <E>   the type of the error
     * @return an {@code Err} variant of {@code Result}
     * @throws NullPointerException if the error is null
     */
    static <T, E> Result<T, E> err(E error) {
        return new Err<>(Objects.requireNonNull(error, "Error value cannot be null"));
    }

    /**
     * Returns {@code true} if the result is an {@code Ok} value.
     *
     * @return true if successful, false otherwise
     */
    boolean isOk();

    /**
     * Returns {@code true} if the result is an {@code Err} value.
     *
     * @return true if failed, false otherwise
     */
    boolean isErr();

    /**
     * Returns the contained {@code Ok} value, or throws an exception if it is
     * an {@code Err}.
     * <p>
     * <b>Warning:</b> Calling this method on an {@code Err} will result in an
     * exception. Consider using
     * {@link #unwrapOr(Object)}, {@link #match(Function, Function)}, or pattern
     * matching instead for safer extraction.
     *
     * @return the successful value
     * @throws IllegalStateException if the result is an {@code Err}
     */
    T unwrap() throws IllegalStateException;

    /**
     * Returns the contained {@code Err} value, or throws an exception if it is
     * an {@code Ok}.
     *
     * @return the error value
     * @throws IllegalStateException if the result is an {@code Ok}
     */
    E unwrapErr() throws IllegalStateException;

    /**
     * Returns the contained {@code Ok} value, or a provided default if it is an
     * {@code Err}.
     *
     * @param defaultValue the value to return if this is an {@code Err}
     * @return the success value or the default
     */
    T unwrapOr(T defaultValue);

    /**
     * Returns the contained {@code Ok} value, or computes a fallback value from
     * the error.
     *
     * @param fallback a function to compute a replacement value from the error
     * @return the success value or the computed fallback
     */
    T unwrapOrElse(Function<? super E, ? extends T> fallback);

    /**
     * Returns the contained {@code Ok} value, or throws an exception generated
     * by the provided supplier.
     *
     * @param exceptionMapper a function mapping the error to an exception
     * @param <X>             the type of the exception to be thrown
     * @return the success value
     * @throws X if the result is an {@code Err}
     */
    <X extends Throwable> T unwrapOrElseThrow(Function<? super E, ? extends X> exceptionMapper) throws X;

    /**
     * Applies a function to the contained {@code Ok} value, leaving an
     * {@code Err} untouched.
     *
     * @param mapper the function to apply to the success value
     * @param <U>    the type of the new success value
     * @return a new {@code Result} containing the mapped value or the original
     * error
     */
    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);

    /**
     * Applies a function to the contained {@code Err} value, leaving an
     * {@code Ok} untouched.
     *
     * @param mapper the function to apply to the error value
     * @param <F>    the type of the new error value
     * @return a new {@code Result} containing the original value or the mapped
     * error
     */
    <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper);

    /**
     * Chains a subsequent operation that returns a {@code Result} onto an
     * {@code Ok} value. If this is an {@code Err}, the error is propagated
     * unmodified.
     *
     * @param mapper the function returning a new {@code Result}
     * @param <U>    the success type of the new {@code Result}
     * @return the result of applying the mapper, or the original error
     */
    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<? extends U, ? extends E>> mapper);

    /**
     * Transforms an {@code Err} value into a new {@code Result}, allowing for
     * error recovery. If this is an {@code Ok}, the success value is propagated
     * unmodified.
     *
     * @param mapper the function returning a new {@code Result}
     * @param <F>    the error type of the new {@code Result}
     * @return the result of applying the mapper, or the original success value
     */
    <F> Result<T, F> flatMapErr(Function<? super E, ? extends Result<? extends T, ? extends F>> mapper);

    /**
     * Consolidates the {@code Result} into a single return type by applying one
     * of two functions based on whether the result is {@code Ok} or
     * {@code Err}.
     *
     * @param onOk  the function to apply if the result is {@code Ok}
     * @param onErr the function to apply if the result is {@code Err}
     * @param <R>   the return type of both functions
     * @return the result of applying the corresponding function
     */
    <R> R match(Function<? super T, ? extends R> onOk, Function<? super E, ? extends R> onErr);

    /**
     * Executes a side-effect if the result is {@code Ok}. Does nothing if
     * {@code Err}.
     *
     * @param action the action to perform on the success value
     * @return this {@code Result} instance for chaining
     */
    Result<T, E> ifOk(Consumer<? super T> action);

    /**
     * Executes a side-effect if the result is {@code Err}. Does nothing if
     * {@code Ok}.
     *
     * @param action the action to perform on the error value
     * @return this {@code Result} instance for chaining
     */
    Result<T, E> ifErr(Consumer<? super E> action);

    /**
     * Converts the {@code Result} into a standard Java {@code Optional}.
     *
     * @return an {@code Optional} containing the success value, or empty if
     * {@code Err}.
     */
    Optional<T> toOptional();

    /**
     * The successful variant of {@link Result}.
     */
    record Ok<T, E>(T value) implements Result<T, E> {

        public Ok {
            Objects.requireNonNull(value, "Success value cannot be null");
        }

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public boolean isErr() {
            return false;
        }

        @Override
        public T unwrap() {
            return value;
        }

        @Override
        public E unwrapErr() {
            throw new IllegalStateException("Called unwrapErr() on an Ok value: " + value);
        }

        @Override
        public T unwrapOr(T defaultValue) {
            return value;
        }

        @Override
        public T unwrapOrElse(Function<? super E, ? extends T> fallback) {
            return value;
        }

        @Override
        public <X extends Throwable> T unwrapOrElseThrow(Function<? super E, ? extends X> exceptionMapper) {
            return value;
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return new Ok<>(mapper.apply(value));
        }

        @Override
        public <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
            // Safe cast because E is unused in Ok
            @SuppressWarnings("unchecked")
            Result<T, F> casted = (Result<T, F>) this;
            return casted;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<? extends U, ? extends E>> mapper) {
            return (Result<U, E>) mapper.apply(value);
        }

        @Override
        public <F> Result<T, F> flatMapErr(Function<? super E, ? extends Result<? extends T, ? extends F>> mapper) {
            @SuppressWarnings("unchecked")
            Result<T, F> casted = (Result<T, F>) this;
            return casted;
        }

        @Override
        public <R> R match(Function<? super T, ? extends R> onOk, Function<? super E, ? extends R> onErr) {
            return onOk.apply(value);
        }

        @Override
        public Result<T, E> ifOk(Consumer<? super T> action) {
            action.accept(value);
            return this;
        }

        @Override
        public Result<T, E> ifErr(Consumer<? super E> action) {
            return this;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }
    }

    /**
     * The failure variant of {@link Result}.
     */
    record Err<T, E>(E error) implements Result<T, E> {

        public Err {
            Objects.requireNonNull(error, "Error value cannot be null");
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public boolean isErr() {
            return true;
        }

        @Override
        public T unwrap() {
            throw new IllegalStateException("Called unwrap() on an Err value: " + error);
        }

        @Override
        public E unwrapErr() {
            return error;
        }

        @Override
        public T unwrapOr(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T unwrapOrElse(Function<? super E, ? extends T> fallback) {
            return fallback.apply(error);
        }

        @Override
        public <X extends Throwable> T unwrapOrElseThrow(Function<? super E, ? extends X> exceptionMapper) throws X {
            throw exceptionMapper.apply(error);
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            @SuppressWarnings("unchecked")
            Result<U, E> casted = (Result<U, E>) this;
            return casted;
        }

        @Override
        public <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
            return new Err<>(mapper.apply(error));
        }

        @Override
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<? extends U, ? extends E>> mapper) {
            @SuppressWarnings("unchecked")
            Result<U, E> casted = (Result<U, E>) this;
            return casted;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <F> Result<T, F> flatMapErr(Function<? super E, ? extends Result<? extends T, ? extends F>> mapper) {
            return (Result<T, F>) mapper.apply(error);
        }

        @Override
        public <R> R match(Function<? super T, ? extends R> onOk, Function<? super E, ? extends R> onErr) {
            return onErr.apply(error);
        }

        @Override
        public Result<T, E> ifOk(Consumer<? super T> action) {
            return this;
        }

        @Override
        public Result<T, E> ifErr(Consumer<? super E> action) {
            action.accept(error);
            return this;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }
    }
}
