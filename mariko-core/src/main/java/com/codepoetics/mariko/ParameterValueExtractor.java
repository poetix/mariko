package com.codepoetics.mariko;

import com.codepoetics.mariko.api.InterpretationException;
import com.codepoetics.mariko.api.Interpreter;

import java.util.List;
import java.util.regex.MatchResult;

class ParameterValueExtractor {

    private final List<? extends Interpreter<?>> interpreters;

    public ParameterValueExtractor(List<? extends Interpreter<?>> interpreters) {
        this.interpreters = interpreters;
    }

    public Object[] extractParameterValues(MatchResult matchResult) {
        if (matchResult.groupCount() == 0) {
            if (interpreters.size() != 1) {
                throw new InterpretationException(
                        "Expected %d parameters, but match result %s has no subgroups"
                                .formatted(interpreters.size(), matchResult));
            }
            return new Object[] {interpreters.get(0).interpret(matchResult.group())};
        }

        if (matchResult.groupCount() != interpreters.size()) {
            throw new InterpretationException(
                    "Expected %d parameters, but match result %s has %d subgroups"
                            .formatted(interpreters.size(), matchResult, matchResult.groupCount())
            );
        }

        var parameterValues = new Object[interpreters.size()];
        for (int i = 0; i < interpreters.size(); i++) {
            parameterValues[i] = interpreters.get(i).interpret(matchResult.group(i + 1));
        }
        return parameterValues;
    }
}
