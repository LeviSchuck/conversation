package com.levischuck.conversation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a @DialogStep method does not return a context value, it will assume
 * it is mutated or unchanged in the implementation.
 *
 * If a @DialogStep method does not return a dialog reference explicitly, it will default
 * to the value here. In the case that the value here is not the default, it will assume
 * the same Dialog that this step belongs to, namely the class with a @DialogGen annotation.
 * Specified values for dialog will be checked at compile time.
 *
 * If a @DialogStep does not return a step explicitly, it will be defaulted to the value here.
 * In the case that neither the return value nor the step specified in this annotation is valid,
 * it will remain in the same step as the executing step.
 * Values in step will refer to function names within the selected dialog and will be checked
 * at compile time.
 *
 * Uses should have a @StepContext parameter and a @StepMessage parameter.
 * All uses of @StepContext within a dialog must be the same type.
 * All uses of @StepMessage within a dialog must be the same type.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface DialogStep {
    /**
     * Default next dialog class (if annotated), if this step does not return a step value
     * @return
     */
    Class dialog() default Object.class;

    /**
     * Default next step, if this step does not return a step value
     * @return
     */
    String step() default "";
}
