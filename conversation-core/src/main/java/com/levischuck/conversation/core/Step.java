package com.levischuck.conversation.core;

@FunctionalInterface
public interface Step <C, M, D, S> {
    StepResult<C, M, D, S> converse(C context, M message);
}
