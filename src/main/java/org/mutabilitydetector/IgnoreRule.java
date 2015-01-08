package org.mutabilitydetector;

public interface IgnoreRule {
    String definition();
    IgnoreRuleMatch check(String path, boolean isDirectory);
}
