package org.mutabilitydetector;

public interface RepositoryFile {
    RepositoryFile getParent();
    IgnoreRules getIgnoreRules();
    RepositoryFile relativize(RepositoryFile descendant);
    String getPath();
    boolean isDirectory();
    boolean isRoot();
    boolean isInternal();
}
