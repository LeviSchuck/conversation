package com.levischuck.conversation.sample;

import com.levischuck.conversation.core.MemorizingConversation;
import com.levischuck.conversation.impl.Builder;
import com.levischuck.conversation.sample.demo.Context;
import com.levischuck.conversation.sample.demo.GenBot;
import com.levischuck.conversation.sample.demo.Main;
import com.levischuck.conversation.sample.demo.Secondary;

public class Application {
    public static void main(String args[]) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
        MemorizingConversation<Context, String, String, String> convo = Builder.memorized(new GenBot(new Main(), new Secondary()));
        Context context = new Context();
        convo.converse(context, "stay");
        convo.converse(context, "hello");
        convo.converse(context, "hello");
        convo.converse(context, "hello");
        convo.converse(context, "1");
        convo.converse(context, "2");
        convo.converse(context, "3");
        convo.converse(context, "4");
        convo.converse(context, "5");
        convo.converse(context, "hello");
        convo.converse(context, "hello");
        convo.converse(context, "hello");
        convo.converse(context, "6");
        convo.converse(context, "hello");
        convo.converse(context, "7");
        convo.converse(context, "hello");

        System.exit(0);
    }
}
