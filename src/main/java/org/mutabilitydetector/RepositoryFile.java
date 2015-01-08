package org.mutabilitydetector;

public interface RepositoryFile {
    RepositoryFile getParent();
    IgnoreRules getIgnoreRules();
    String getRepositoryRelativePath();
    boolean isDirectory();
    boolean isRoot();
    boolean isInternal();
}
