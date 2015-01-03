package org.mutabilitydetector;

public interface IgnoreRule {
    IgnoreRuleMatch check(String path, boolean isDirectory);
}
