package com.levischuck.conversation.impl;

import com.levischuck.conversation.core.Conversation;
import com.levischuck.conversation.core.StepResult;
import com.levischuck.conversation.core.MemorizingContext;
import com.levischuck.conversation.core.MemorizingConversation;

public class PersistingConversation<C extends MemorizingContext<C, D, S>, M, D, S> implements MemorizingConversation<C, M, D, S> {
    private final Conversation<C, M, D, S> conversation;

    public PersistingConversation(Conversation<C, M, D, S> conversation) {
        this.conversation = conversation;
    }

    @Override
    public StepResult<C, M, D, S> converse(C context, M message) {
        StepResult<C, M, D, S> result = conversation.converse(
                context,
                message,
                context.getCurrentDialog(),
                context.getCurrentStep()
        );
        return result.withResultContext(result.getResultContext()
                .withCurrentDialog(result.getNextDialog())
                .withCurrentStep(result.getNextStep())
        );
    }
}
