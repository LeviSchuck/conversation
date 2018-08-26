package com.levischuck.conversation.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {
    public static <C> GenResult<C> execute(Runnable method, Class defaultNextDialog, String defaultNextStep) {
        method.run();
        return GenResult.result(defaultNextDialog, defaultNextStep);
    }

    public static <C, M> AnnotatedCall<C, M> toCall(Runnable method, Class defaultNextDialog, String defaultNextStep) {
        return (c, m) -> {
            method.run();
            return GenResult.result(c, defaultNextDialog, defaultNextStep);
        };
    }

    public static <C, M> StepResult<C, M, String, String> wrap(
            AnnotatedBot<C, M> bot,
            C context,
            M message,
            AnnotatedCall<C, M> call,
            Class dialog,
            Class defaultNextDialog,
            String defaultNextStep
    ) {
        Logger log = LoggerFactory.getLogger(dialog);

        // Step up defaults
        Class nextDialogClass = defaultNextDialog;
        String nextDialog = defaultNextDialog.getCanonicalName();
        String nextStep = defaultNextStep;

        GenResult<C> result = call.call(context, message);

        if (result.getDialog() != null) {
            nextDialogClass = result.getDialog();
            nextDialog = nextDialogClass.getCanonicalName();
            nextStep = null;
        }

        if (result.getStep() != null) {
            nextStep = result.getStep();
        }

        if (result.getContext() != null) {
            context = result.getContext();
        }

        AnnotatedDialog<C, M> foundNextDialog = bot.getAnnotatedDialogs().get(nextDialogClass);

        if (foundNextDialog == null) {
            log.warn("Dialog: {} could not be found within this bot, defaulting to root", nextDialog);
            // Reset to default
            nextDialog = bot.rootDialog();
            nextStep = bot.getDialogs().get(nextDialog).rootStep();
        } else if (nextStep != null) {
            if (result.isCall()) {
                AnnotatedCall<C, M> foundNextCall = foundNextDialog.getCalls().get(nextStep);
                if (foundNextCall == null) {
                    log.warn("Call: {}::{} could not be found, defaulting to root", nextDialog, nextStep);
                    nextStep = foundNextDialog.rootStep();
                } else {
                    log.trace("Next call: {}::{}", nextDialog, nextStep);
                    return wrap(bot, context, message, foundNextCall, nextDialogClass, nextDialogClass, nextStep);
                }
            } else {
                Step<C, M, String, String> foundNextStep = foundNextDialog.getSteps().get(nextStep);
                if (foundNextStep == null) {
                    log.warn("Step: {}::{} could not be found, defaulting to root", nextDialog, nextStep);
                    nextStep = foundNextDialog.rootStep();
                }
            }
        } else {
            nextStep = foundNextDialog.rootStep();
        }

        log.trace("Next step: {}::{}", nextDialog, nextStep);

        return new StepResult<>(context, nextDialog, nextStep);
    }
}
