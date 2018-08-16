package com.levischuck.conversation.sample.demo;

import com.levischuck.conversation.annotations.*;
import com.levischuck.conversation.core.GenResult;
import static com.levischuck.conversation.core.GenResult.result;

@RootDialog("demo")
@DialogGen(group = "demo", root = "first")
public class Main {
    @DialogStep
    public GenResult<Context> first(@StepMessage String message) {
        System.out.println("First");
        if ("stay".equals(message)) {
            System.out.println("Staying.");
            return result("first");
        } else {
            return result("second");
        }
    }

    @DialogStep(step = "third")
    public void second() {
        System.out.println("Second");
    }

    @DialogStep(dialog = Secondary.class)
    public void third() {
        System.out.println("Thirth, switching to Secondary");
    }

    @DialogStep(step="first")
    public void extra() {
        System.out.println("Congratulations, you found the bonus room, going back to first");
    }
}
