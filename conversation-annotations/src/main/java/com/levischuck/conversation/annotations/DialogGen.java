package com.levischuck.conversation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate a dialog implementation for this class.
 *
 * The group in this annotation refers to the namespace of this dialog,
 * any dialogs that are linked to must be of the same namespace.
 * All dialogs within the same namespace must implement the same context type.
 *
 * The root in this annotation refers to a step in this dialog.
 * This will be checked at compile time.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DialogGen {
    String group();
    String root();
}
