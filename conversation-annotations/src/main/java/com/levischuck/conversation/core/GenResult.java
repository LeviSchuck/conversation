package com.levischuck.conversation.core;

import java.util.Objects;

/**
 * When returned, the type parameter will also be checked against the type used
 * in the @StepContext parameter at compile time.
 * @param <C>
 */
public class GenResult<C> {
    private C context;
    private Class dialog;
    private String step;
    private boolean call;

    public static <C> GenResult<C> result() {
        return new GenResult<>();
    }

    public static <C> GenResult<C> result(C context) {
        GenResult<C> result = new GenResult<>();
        return result.withContext(context);
    }

    public static <C> GenResult<C> result(Class dialog) {
        GenResult<C> result = new GenResult<>();
        return result.withDialog(dialog);
    }

    public static <C> GenResult<C> result(String step) {
        GenResult<C> result = new GenResult<>();
        return result.withStep(step);
    }

    public static <C> GenResult<C> result(Class dialog, String step) {
        GenResult<C> result = new GenResult<>();
        return result.withDialog(dialog).withStep(step);
    }

    public static <C> GenResult<C> result(C context, Class dialog) {
        GenResult<C> result = new GenResult<>();
        return result.withContext(context).withDialog(dialog);
    }

    public static <C> GenResult<C> result(C context, String step) {
        GenResult<C> result = new GenResult<>();
        return result.withContext(context).withStep(step);
    }

    public static <C> GenResult<C> result(C context, Class dialog, String step) {
        GenResult<C> result = new GenResult<>();
        return result.withContext(context).withDialog(dialog).withStep(step);
    }

    public static <C> GenResult<C> call(String step) {
        GenResult<C> result = new GenResult<>();
        return result.withStep(step).withCall(true);
    }

    public static <C> GenResult<C> call(Class dialog, String step) {
        GenResult<C> result = new GenResult<>();
        return result.withDialog(dialog).withStep(step).withCall(true);
    }

    public static <C> GenResult<C> call(C context, Class dialog, String step) {
        GenResult<C> result = new GenResult<>();
        return result.withContext(context).withDialog(dialog).withStep(step).withCall(true);
    }

    public C getContext() {
        return context;
    }

    public void setContext(C context) {
        this.context = context;
    }

    public Class getDialog() {
        return dialog;
    }

    public void setDialog(Class dialog) {
        this.dialog = dialog;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public boolean isCall() {
        return call;
    }

    public void setCall(boolean call) {
        this.call = call;
    }

    private GenResult<C> withContext(C context) {
        setContext(context);
        return this;
    }

    private GenResult<C> withDialog(Class dialog) {
        setDialog(dialog);
        return this;
    }

    private GenResult<C> withStep(String step) {
        setStep(step);
        return this;
    }

    private GenResult<C> withCall(boolean call) {
        setCall(call);
        return this;
    }

    @Override
    public String toString() {
        return "GenResult{" +
                "context=" + context +
                ", dialog=" + dialog +
                ", step='" + step + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenResult<?> genResult = (GenResult<?>) o;
        return Objects.equals(context, genResult.context) &&
                Objects.equals(dialog, genResult.dialog) &&
                Objects.equals(step, genResult.step);
    }

    @Override
    public int hashCode() {

        return Objects.hash(context, dialog, step);
    }
}
