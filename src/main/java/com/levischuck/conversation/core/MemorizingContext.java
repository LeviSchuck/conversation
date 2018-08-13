package com.levischuck.conversation.core;

public interface MemorizingContext<C extends MemorizingContext, D, S> {
    D getCurrentDialog();
    C withCurrentDialog(D dialog);

    S getCurrentStep();
    C withCurrentStep(S step);
}
