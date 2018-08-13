package com.levischuck.conversation.impl;

import com.levischuck.conversation.core.MemorizingContext;

public class MemorizedContext<C extends MemorizedContext<C, D, S>, D, S> implements MemorizingContext<C, D, S> {
    private D dialog;
    private S step;

    @Override
    public D getCurrentDialog() {
        return dialog;
    }

    @SuppressWarnings("unchecked")
    @Override
    public C withCurrentDialog(D dialog) {
        this.dialog = dialog;
        return (C)this;
    }

    @Override
    public S getCurrentStep() {
        return step;
    }

    @SuppressWarnings("unchecked")
    @Override
    public C withCurrentStep(S step) {
        this.step = step;
        return (C)this;
    }
}
