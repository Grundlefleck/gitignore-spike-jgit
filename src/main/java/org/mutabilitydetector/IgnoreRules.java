package org.mutabilitydetector;

import java.util.List;

public interface IgnoreRules {
    boolean exists();
    List<IgnoreRule> rules() throws FailedToRetrieveIgnoreRules;

    static class FailedToRetrieveIgnoreRules extends Exception {
        public FailedToRetrieveIgnoreRules(Exception cause) {
            super(cause);
        }
    }
}
