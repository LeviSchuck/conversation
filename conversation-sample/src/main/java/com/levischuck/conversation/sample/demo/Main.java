package com.levischuck.conversation.sample.demo;

import com.levischuck.conversation.annotations.*;
import com.levischuck.conversation.core.GenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.levischuck.conversation.core.GenResult.result;

@RootDialog(packageName = "com.levischuck.conversation.sample.demo")
@DialogGen(group = "demo", root = "first")
public class Main {
    private final static Logger log = LoggerFactory.getLogger(Main.class);
    @DialogStep
    public GenResult<Context> first(@StepMessage String message) {
        log.info("First");
        if ("stay".equals(message)) {
            log.info("Staying");
            return result("first");
        } else {
            return result("second");
        }
    }

    @DialogStep(step = "third")
    public void second() {
        log.info("Second");
    }

    @DialogStep(dialog = Secondary.class)
    public void third() {
        log.info("Thirth, switching to Secondary");
    }

    @DialogStep(step="first")
    public void extra() {
        log.info("Congratulations, you found the bonus room, going back to first");
    }

    @DialogStep(dialog = Secondary.class, step="first", callOnly = true)
    public void extraCall() {
        log.info("Congratulations, you found the bonus room, going back");
    }
}
