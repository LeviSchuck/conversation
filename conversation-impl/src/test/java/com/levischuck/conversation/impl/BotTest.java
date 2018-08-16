package com.levischuck.conversation.impl;

import com.levischuck.conversation.core.StepResult;
import com.levischuck.conversation.core.MemorizingConversation;
import com.levischuck.conversation.impl.Builder;
import com.levischuck.conversation.impl.MemorizedContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class BotTest {
    private enum DialogRef {
        Root,
        Magic
    };
    private enum StepRef {
        Root,
        Hello
    }

    private static class Context extends MemorizedContext<Context, DialogRef, StepRef> {
        String lastResponse;

        void reply(String response) {
            System.out.println("Output: " + response);
            lastResponse = response;
        }

        void clear() {
            lastResponse = null;
        }
    }

    private static class Message {
        Message(String message) {
            this.message = message;
        }
        String message;
    }

    private MemorizingConversation<Context, Message, DialogRef, StepRef> makeTestConversation() {
        return Builder.memorized(b -> b.dialog(DialogRef.Root)
                .step(StepRef.Root, (c, m) -> {
                    if ("Hello".equalsIgnoreCase(m.message)) {
                        c.reply("Hello");
                        return new StepResult<>(c, DialogRef.Root, StepRef.Hello);
                    }
                    c.reply("Unhandled");
                    return new StepResult<>(c, DialogRef.Root, StepRef.Root);
                })
                .step(StepRef.Hello, (c, m) -> {
                    c.reply("I hear that " + m.message);
                    if ("Magic".equalsIgnoreCase(m.message)) {
                        return new StepResult<>(c, DialogRef.Magic, null);
                    }
                    return new StepResult<>(c, DialogRef.Root, StepRef.Root);
                })
                .endDialog()
                .dialog(DialogRef.Magic)
                .step(StepRef.Root, (c, m) -> {
                    c.reply("Magical indeed.");
                    return new StepResult<>(c, DialogRef.Root, null);
                })
                .endDialog());
    }

    @Test
    public void converseHello() {
        MemorizingConversation<Context, Message, DialogRef, StepRef> conversation = makeTestConversation();
        Context context = new Context();
        conversation.converse(context, new Message("Hello"));
        Assertions.assertEquals("Hello", context.lastResponse);
        context.clear();
        conversation.converse(context, new Message("red is a color"));
        Assertions.assertEquals("I hear that red is a color", context.lastResponse);
        context.clear();
    }

    @Test
    public void converseUnhandled() {
        MemorizingConversation<Context, Message, DialogRef, StepRef> conversation = makeTestConversation();
        Context context = new Context();
        conversation.converse(context, new Message("Blah"));
        Assertions.assertEquals("Unhandled", context.lastResponse);
        context.clear();
    }

    @Test
    public void converseMagical() {
        MemorizingConversation<Context, Message, DialogRef, StepRef> conversation = makeTestConversation();
        Context context = new Context();
        conversation.converse(context, new Message("Hello"));
        Assertions.assertEquals("Hello", context.lastResponse);
        Assertions.assertEquals(DialogRef.Root, context.getCurrentDialog());
        Assertions.assertEquals(StepRef.Hello, context.getCurrentStep());
        context.clear();
        conversation.converse(context, new Message("magic"));
        Assertions.assertEquals("I hear that magic", context.lastResponse);
        Assertions.assertEquals(DialogRef.Magic, context.getCurrentDialog());
        context.clear();
        conversation.converse(context, new Message("neato"));
        Assertions.assertEquals("Magical indeed.", context.lastResponse);
        Assertions.assertEquals(DialogRef.Root, context.getCurrentDialog());
        context.clear();
    }

    @Test
    public void badBuilderBot() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            Builder.bot().endBot();
        });
    }
    @Test
    public void badBuilderDialog() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            Builder.bot().dialog(new Object()).endDialog().endBot();
        });
    }
    @Test
    public void badBuilderNullDialog() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            Builder.bot().dialog(null).step("Something", (c, m) -> new StepResult<>(c, 1, 2)).endDialog().endBot();
        });
    }
    @Test
    public void badBuilderNullStep() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            Builder.bot().dialog("Something").step(null, (c, m) -> new StepResult<>(c, 1, 2)).endDialog().endBot();
        });
    }
}
