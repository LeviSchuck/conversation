package com.levischuck.conversation.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {
    public static <C> GenResult<C> execute(Runnable method, Class defaultNextDialog, String defaultNextStep) {
        method.run();
        return GenResult.result(defaultNextDialog, defaultNextStep);
    }

    public static <C, M> StepResult<C, M, String, String> wrap(
            Bot<C, M, String, String> bot,
            C context,
            GenResult<C> result,
            Class dialog,
            Class defaultNextDialog,
            String defaultNextStep
    ) {
        Logger log = LoggerFactory.getLogger(dialog);

        // Step up defaults
        String nextDialog = defaultNextDialog.getCanonicalName();
        String nextStep = defaultNextStep;

        if (result.getDialog() != null) {
            nextDialog = result.getDialog().getCanonicalName();
            nextStep = null;
        }

        if (result.getStep() != null) {
            nextStep = result.getStep();
        }

        if (result.getContext() != null) {
            context = result.getContext();
        }

        Dialog<C, M, String, String> foundNextDialog = bot.getDialogs().get(nextDialog);

        if (foundNextDialog == null) {
            log.warn("Dialog: {} could not be found within this bot, defaulting to root", nextDialog);
            // Reset to default
            nextDialog = bot.rootDialog();
            nextStep = bot.getDialogs().get(nextDialog).rootStep();
        } else if (nextStep != null) {
            Step<C, M, String, String> foundNextStep = foundNextDialog.getSteps().get(nextStep);

            if (foundNextStep == null) {
                log.warn("Step: {}::{} could not be found, defaulting to root", nextDialog, nextStep);
                nextStep = foundNextDialog.rootStep();
            }
        } else {
            nextStep = foundNextDialog.rootStep();
        }

        log.trace("Next step: {}::{}", nextDialog, nextStep);

        return new StepResult<>(context, nextDialog, nextStep);
    }
}
