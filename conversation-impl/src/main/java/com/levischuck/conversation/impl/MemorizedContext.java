package com.levischuck.conversation.impl;

import com.levischuck.conversation.core.MemorizingContext;

public class MemorizedContext<C extends MemorizedContext<C, D, S>, D, S> implements MemorizingContext<C, D, S> {
    private D dialog;
    private S step;

    @Override
    public D getCurrentDialog() {
        return dialog;
    }

    public void setCurrentDialog(D dialog) {
        this.dialog = dialog;
    }

    @SuppressWarnings("unchecked")
    @Override
    public C withCurrentDialog(D dialog) {
        setCurrentDialog(dialog);
        return (C)this;
    }

    @Override
    public S getCurrentStep() {
        return step;
    }

    public void setCurrentStep(S step) {
        this.step = step;
    }

    @SuppressWarnings("unchecked")
    @Override
    public C withCurrentStep(S step) {
        setCurrentStep(step);
        return (C)this;
    }
}
