package com.levischuck.conversation.core;

public interface Conversation<C, M, D, S> {
    StepResult<C, M, D, S> converse(C context, M message, D dialogDescriptor, S stepDescriptor);
}
