package org.states;

import org.CodeThiefPro;

public class ErrorState implements ScriptState {
    @Override
    public int execute(CodeThiefPro context) {
        if (System.currentTimeMillis() - context.getLastStateChange() > 30000) {
            context.log("[ErrorState] Attempting to recover from error state");
            context.setState(new InitializeState());
            return 1000;
        }
        return 5000;
    }
}