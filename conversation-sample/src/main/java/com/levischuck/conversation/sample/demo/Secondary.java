package com.levischuck.conversation.sample.demo;

import com.levischuck.conversation.annotations.DialogGen;
import com.levischuck.conversation.annotations.DialogStep;
import com.levischuck.conversation.annotations.StepContext;
import com.levischuck.conversation.annotations.StepMessage;
import com.levischuck.conversation.core.GenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.levischuck.conversation.core.GenResult.result;

@DialogGen(group = "demo", root = "first")
public class Secondary {
    private final static Logger log = LoggerFactory.getLogger(Secondary.class);
    @DialogStep
    public GenResult<Context> first(@StepContext Context context, @StepMessage String message) {
        switch (message) {
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
                log.info("You said: " + message);
                break;
        }
        switch (message) {
            case "1": return result();
            case "2": return result(context);
            case "3": return result("second");
            case "4": return result(Main.class);
            case "5": return result(context, Main.class);
            case "6": return result(context, "third");
            case "7": return result(context, Main.class, "extra");
        }
        log.info("Try a number from 1 to 7");
        return result();
    }

    @DialogStep(step="first")
    public void second() {
        log.info("Neat. Now try a number from 1 to 7");
    }

    @DialogStep(step="first")
    public void third() {
        log.info("Cool. Now try a number from 1 to 7");
    }
}
