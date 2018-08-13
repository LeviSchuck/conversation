package com.levischuck.conversation.core;

@FunctionalInterface
public interface Step <C, M, D, S> {
    ConverseResult<C, M, D, S> converse(C context, M message);
}
