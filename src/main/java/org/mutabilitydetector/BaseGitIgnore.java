package org.mutabilitydetector;

import java.util.Iterator;
import java.util.LinkedList;

public abstract class BaseGitIgnore implements VcsIgnores {
    protected final RepositoryRoot rootDirectory;

    protected BaseGitIgnore(RepositoryRoot rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public boolean isIgnored(String pathToCheck) {
        RepositoryFile fileToCheck = rootDirectory.fromPath(pathToCheck);
        boolean pathIsForDirectory = fileToCheck.isDirectory();
        RepositoryFile directoryContainingFileToCheck = pathIsForDirectory ? fileToCheck : fileToCheck.getParent();
        Iterator<RepositoryFile> pathSegments = createPathSegmentsFromRootTo(directoryContainingFileToCheck);
        return descendInSearchOfGitIgnoreFile(fileToCheck, pathSegments, pathIsForDirectory);
    }

    private Iterator<RepositoryFile> createPathSegmentsFromRootTo(RepositoryFile file) {
        LinkedList<RepositoryFile> files = new LinkedList<>();
        RepositoryFile currentFile = file;
        files.addFirst(currentFile);

        while (currentFile.getParent() != null && !currentFile.isRoot()) {
            RepositoryFile parentFile = currentFile.getParent();
            files.addLast(parentFile);
            currentFile = parentFile;
        }

        return files.iterator();
    }

    private boolean descendInSearchOfGitIgnoreFile(RepositoryFile fileToCheck, Iterator<RepositoryFile> pathSegments, boolean isDirectory) {
        return pathSegments.hasNext()
                ? checkAgainstCurrentGitIgnoreAndDescendIfNecessary(fileToCheck, pathSegments, isDirectory)
                : false;
    }

    private boolean checkAgainstCurrentGitIgnoreAndDescendIfNecessary(
            RepositoryFile fileToCheck,
            Iterator<RepositoryFile> pathSegments,
            boolean isDirectory) {

        RepositoryFile current = pathSegments.next();
        if (current.isInternal()) {
            return true;
        }

        IgnoreRules currentGitIgnore = current.getIgnoreRules();

        if (currentGitIgnore.exists()) {
            RepositoryFile relativeFile = current.relativize(fileToCheck);
            switch (getMatchResult(relativeFile.getPath(), currentGitIgnore, isDirectory)) {
                case DOES_NOT_MATCH:
                    return descendInSearchOfGitIgnoreFile(fileToCheck, pathSegments, isDirectory);
                case IS_IGNORED:
                    return true;
                case IS_NOT_IGNORED:
                default:
                    return false;
            }
        } else {
            return descendInSearchOfGitIgnoreFile(fileToCheck, pathSegments, isDirectory);
        }
    }

    protected abstract IgnoreRuleMatch getMatchResult(String pathToCheck, IgnoreRules currentGitIgnore, boolean isDirectory);


}
