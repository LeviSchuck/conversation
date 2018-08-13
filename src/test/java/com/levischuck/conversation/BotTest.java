package com.levischuck.conversation;

import com.levischuck.conversation.core.ConverseResult;
import com.levischuck.conversation.core.MemorizingConversation;
import com.levischuck.conversation.impl.MemorizedContext;
import com.levischuck.conversation.impl.PersistingConversation;
import com.levischuck.conversation.impl.SimpleConversation;
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

    private static class TestConversation extends PersistingConversation<Context, Message, DialogRef, StepRef> {
        public TestConversation() {
            super(new SimpleConversation<>(b -> b.dialog(DialogRef.Root)
                    .step(StepRef.Root, (c, m) -> {
                        if ("Hello".equalsIgnoreCase(m.message)) {
                            c.reply("Hello");
                            return new ConverseResult<>(c, DialogRef.Root, StepRef.Hello);
                        }
                        c.reply("Unhandled");
                        return new ConverseResult<>(c, DialogRef.Root, StepRef.Root);
                    })
                    .step(StepRef.Hello, (c, m) -> {
                        c.reply("I hear that " + m.message);
                        if ("Magic".equalsIgnoreCase(m.message)) {
                            return new ConverseResult<>(c, DialogRef.Magic, null);
                        }
                        return new ConverseResult<>(c, DialogRef.Root, StepRef.Root);
                    })
                    .endDialog()
                    .dialog(DialogRef.Magic)
                    .step(StepRef.Root, (c, m) -> {
                        c.reply("Magical indeed.");
                        return new ConverseResult<>(c, DialogRef.Root, null);
                    })
                    .endDialog()
            ));
        }

        @Override
        public ConverseResult<Context, Message, DialogRef, StepRef> converse(Context context, Message message) {
            System.out.println("Input: " + message.message);
            return super.converse(context, message);
        }
    }

    @Test
    public void converseHello() {
        MemorizingConversation<Context, Message, DialogRef, StepRef> conversation = new TestConversation();
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
        MemorizingConversation<Context, Message, DialogRef, StepRef> conversation = new TestConversation();
        Context context = new Context();
        conversation.converse(context, new Message("Blah"));
        Assertions.assertEquals("Unhandled", context.lastResponse);
        context.clear();
    }

    @Test
    public void converseMagical() {
        MemorizingConversation<Context, Message, DialogRef, StepRef> conversation = new TestConversation();
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
}