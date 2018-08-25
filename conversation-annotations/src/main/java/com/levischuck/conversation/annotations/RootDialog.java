package com.levischuck.conversation.annotations;

/**
 * Specify the class name here that should be generated for the bot
 */
public @interface RootDialog {
    String packageName();
    String classPrefix() default "Gen";
}
