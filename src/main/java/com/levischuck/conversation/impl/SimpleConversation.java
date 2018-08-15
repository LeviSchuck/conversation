package com.levischuck.conversation.impl;

import com.levischuck.conversation.core.*;

public class SimpleConversation<C, M, D, S> implements Conversation<C, M, D, S> {
    private final Bot<C, M, D, S> bot;

    public SimpleConversation(Bot<C, M, D, S> bot) {
        if (bot == null) {
            throw new NullPointerException("bot is null");
        }
        this.bot = bot;
    }

    @Override
    public ConverseResult<C, M, D, S> converse(C context, M message, D dialogDescriptor, S stepDescriptor) {
        boolean ignoreStepDescriptor = false;
        if (dialogDescriptor == null) {
            ignoreStepDescriptor = true;
            dialogDescriptor = bot.rootDialog();
            if (dialogDescriptor == null) {
                throw new NullPointerException("No root dialog descriptor on bot " + bot);
            }
        }
        Dialog<C, M, D, S> dialog = bot.getDialogs().get(dialogDescriptor);
        if (dialog == null) {
            throw new NullPointerException("Dialog " + dialogDescriptor + " could not be loaded from bot " + bot);
        }
        if (ignoreStepDescriptor || stepDescriptor == null) {
            stepDescriptor = dialog.rootStep();
            if (stepDescriptor == null) {
                throw new NullPointerException("No root step on dialog " + dialog);
            }
        }
        Step<C, M, D, S> step = dialog.getSteps().get(stepDescriptor);
        if (step == null) {
            throw new NullPointerException("Step " + stepDescriptor + " could not be loaded from dialog " + dialog);
        }
        ConverseResult<C, M, D, S> converseResult = step.converse(context, message);
        if (converseResult == null) {
            throw new NullPointerException("converseResult from step converse is null");
        }
        return converseResult;
    }
}
