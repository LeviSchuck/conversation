package com.levischuck.conversation.core;

import java.util.Map;

public interface Dialog <C, M, D, S> {
    S rootStep();
    Map<S, Step<C, M, D, S>> getSteps();
}
