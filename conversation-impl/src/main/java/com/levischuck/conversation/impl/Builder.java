package com.levischuck.conversation.impl;

import com.levischuck.conversation.core.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Builder {
    public static <C, M, D, S> BotBuilder<C, M, D, S> bot() {
        return new BotBuilder<>();
    }

    public static <C, M, D, S> Conversation<C, M, D, S> simple(
            Function<BotBuilder<C, M, D, S>, BotBuilder<C, M, D, S>> builder
    ) {
        return new SimpleConversation<>(builder.apply(Builder.bot()).endBot());
    }

    public static <C extends MemorizingContext<C, D, S>, M, D, S> MemorizingConversation<C, M, D, S> memorized(
            Function<BotBuilder<C, M, D, S>, BotBuilder<C, M, D, S>> builder
    ) {
        return new PersistingConversation<>(simple(builder));
    }

    public static class BotBuilder<C, M, D, S> {
        D first;
        Map<D, Dialog<C, M, D, S>> dialogs;

        private BotBuilder() {
            dialogs = new HashMap<>();
        }

        public DialogBuilder<C, M, D, S> dialog(D reference) {
            return new DialogBuilder<>(reference, this);
        }

        public Bot<C, M, D, S> endBot() {
            return build();
        }

        private BotBuilder<C, M, D, S> with(D dialogReference, Dialog<C, M, D, S> dialog) {
            if (dialogReference == null) {
                throw new NullPointerException("reference is null");
            }
            if (first == null) {
                first = dialogReference;
            }
            dialogs.put(dialogReference, dialog);
            return this;
        }

        private Bot<C, M, D, S> build() {
            if (first == null) {
                throw new IllegalStateException("Invalid bot builder, no dialogs");
            }
            return new Bot<C, M, D, S>() {
                @Override
                public D rootDialog() {
                    return first;
                }

                @Override
                public Map<D, Dialog<C, M, D, S>> getDialogs() {
                    return dialogs;
                }
            };
        }
    }

    public static class DialogBuilder<C, M, D, S> {
        private final Map<S, Step<C, M, D, S>> steps;
        private final BotBuilder<C, M, D, S> botBuilder;
        private final D reference;
        S first;

        private DialogBuilder(D reference , BotBuilder<C, M, D, S> botBuilder) {
            this.botBuilder = botBuilder;
            this.reference = reference;
            steps = new HashMap<>();
        }

        public DialogBuilder<C, M, D, S> step(S reference, Step<C, M, D, S> step) {
            if (reference == null) {
                throw new NullPointerException("reference is null");
            }
            if (first == null) {
                first = reference;
            }
            steps.put(reference, step);
            return this;
        }

        public BotBuilder<C, M, D, S> endDialog() {
            return botBuilder.with(reference, build());
        }

        private Dialog<C, M, D, S> build() {
            if (first == null) {
                throw new IllegalStateException("Invalid dialog builder, no steps");
            }
            return new Dialog<C, M, D, S>() {
                @Override
                public S rootStep() {
                    return first;
                }

                @Override
                public Map<S, Step<C, M, D, S>> getSteps() {
                    return steps;
                }
            };
        }
    }
}
