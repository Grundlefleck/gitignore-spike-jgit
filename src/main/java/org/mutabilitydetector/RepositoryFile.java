package org.mutabilitydetector;

public interface RepositoryFile {
    RepositoryFile getParent();
    IgnoreRules getIgnoreRules();
    String getRepositoryRelativePath();
    String pathRelativeTo(RepositoryFile ancestor);
    boolean isDirectory();
    boolean isRoot();
    boolean isInternal();
}
