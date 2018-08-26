package com.levischuck.conversation.core;

import java.util.Map;

public interface AnnotatedBot <C, M> extends Bot <C, M, String, String> {
    Class rootDialogClass();
    Map<Class, AnnotatedDialog<C, M>> getAnnotatedDialogs();
}
