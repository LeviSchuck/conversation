package com.levischuck.conversation.core;

import java.util.Map;

public interface Bot <C, M, D, S> {
    D rootDialog();
    Map<D, Dialog<C, M, D, S>> getDialogs();
}
