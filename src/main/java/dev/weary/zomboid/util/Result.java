package dev.weary.zomboid.util;

import java.util.function.Consumer;

public class Result<R, E> {
	private final R value;
	private final E error;

	private Result(R value, E error) {
		this.value = value;
		this.error = error;
	}

	public static<R, E> Result<R, E> ofValue(R value) {
		return new Result<>(value, null);
	}

	public static<R, E> Result<R, E> ofError(E error) {
		return new Result<>(null, error);
	}

	public R handleError(Consumer<E> errorHandler) {
		if (error != null) {
			errorHandler.accept(error);
			return null;
		}

		return value;
	}
}
