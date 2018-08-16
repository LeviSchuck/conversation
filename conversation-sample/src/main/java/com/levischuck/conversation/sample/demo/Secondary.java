package com.levischuck.conversation.sample.demo;

import com.levischuck.conversation.annotations.DialogGen;
import com.levischuck.conversation.annotations.DialogStep;
import com.levischuck.conversation.annotations.StepContext;
import com.levischuck.conversation.annotations.StepMessage;
import com.levischuck.conversation.core.GenResult;

import static com.levischuck.conversation.core.GenResult.result;

@DialogGen(group = "demo", root = "first")
public class Secondary {
    @DialogStep
    public GenResult<Context> first(@StepContext Context context, @StepMessage String message) {
        switch (message.toLowerCase()) {
            case "1": return result();
            case "2": return result(context);
            case "3": return result("second");
            case "4": return result(Main.class);
            case "5": return result(context, Main.class);
            case "6": return result(context, "third");
            case "7": return result(context, Main.class, "extra");
        }
        System.out.println("Try a number from 1 to 7");
        return result();
    }

    @DialogStep(step="first")
    public void second() {
        System.out.println("Neat. Now try a number from 1 to 7");
    }

    @DialogStep(step="first")
    public void third() {
        System.out.println("Cool. Now try a number from 1 to 7");
    }
}
