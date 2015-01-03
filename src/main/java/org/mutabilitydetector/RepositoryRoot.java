package org.mutabilitydetector;

public interface RepositoryRoot extends RepositoryFile {
    RepositoryFile fromPath(String path);
}
