package com.levischuck.conversation.core;

public interface MemorizingConversation<C, M, D, S> {
    StepResult<C, M, D, S> converse(C context, M message);
}
