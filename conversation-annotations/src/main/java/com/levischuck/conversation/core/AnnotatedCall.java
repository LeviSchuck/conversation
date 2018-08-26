package com.levischuck.conversation.core;

@FunctionalInterface
public interface AnnotatedCall<C, M> {
    GenResult<C> call(C context, M message);
}
