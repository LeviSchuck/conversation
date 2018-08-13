package com.levischuck.conversation.core;

import java.util.Objects;

public class ConverseResult<C, M, D, S> {
    private final C resultContext;
    private final D nextDialog;
    private final S nextStep;

    public ConverseResult(C resultContext, D nextDialog, S nextStep) {
        if (resultContext == null) {
            throw new NullPointerException("resultContext is null");
        }
        this.resultContext = resultContext;
        this.nextDialog = nextDialog;
        this.nextStep = nextStep;
    }

    public C getResultContext() {
        return resultContext;
    }

    public D getNextDialog() {
        return nextDialog;
    }

    public S getNextStep() {
        return nextStep;
    }

    public ConverseResult<C, M, D, S> withResultContext(C resultContext) {
        return new ConverseResult<>(resultContext, nextDialog, nextStep);
    }

    @Override
    public String toString() {
        return "ConverseResult{" +
                "resultContext=" + resultContext +
                ", nextDialog=" + nextDialog +
                ", nextStep=" + nextStep +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConverseResult<?, ?, ?, ?> that = (ConverseResult<?, ?, ?, ?>) o;
        return Objects.equals(resultContext, that.resultContext) &&
                Objects.equals(nextDialog, that.nextDialog) &&
                Objects.equals(nextStep, that.nextStep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultContext, nextDialog, nextStep);
    }
}
