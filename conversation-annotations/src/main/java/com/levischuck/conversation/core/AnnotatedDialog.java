package com.levischuck.conversation.core;

import java.util.Map;

public interface AnnotatedDialog<C, M> extends Dialog<C, M, String, String> {
    Map<String, AnnotatedCall<C, M>> getCalls();
}
