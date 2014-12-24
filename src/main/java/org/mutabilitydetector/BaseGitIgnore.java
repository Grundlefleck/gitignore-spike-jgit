package org.mutabilitydetector;

import org.eclipse.jgit.ignore.IgnoreNode.MatchResult;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;

import static org.eclipse.jgit.lib.Constants.GITIGNORE_FILENAME;

public abstract class BaseGitIgnore implements VcsIgnores {
    protected final File rootDirectory;

    protected BaseGitIgnore(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public boolean isIgnored(String pathToCheck) {
        File fileToCheck = new File(rootDirectory, pathToCheck);
        boolean pathIsForDirectory = fileToCheck.isDirectory();
        File directoryContainingFileToCheck = pathIsForDirectory ? fileToCheck : fileToCheck.getParentFile();
        Iterator<File> pathSegments = createPathSegmentsFromRootTo(directoryContainingFileToCheck);
        return descendInSearchOfGitIgnoreFile(fileToCheck, pathSegments, pathIsForDirectory);
    }

    private Iterator<File> createPathSegmentsFromRootTo(File file) {
        LinkedList<File> files = new LinkedList<File>();
        File currentFile = file;
        files.addFirst(currentFile);

        while (currentFile.getParent() != null && !currentFile.equals(rootDirectory)) {
            File parentFile = currentFile.getParentFile();
            files.addLast(parentFile);
            currentFile = parentFile;
        }

        return files.iterator();
    }

    private boolean descendInSearchOfGitIgnoreFile(File fileToCheck, Iterator<File> pathSegments, boolean isDirectory) {
        return pathSegments.hasNext()
                ? checkAgainstCurrentGitIgnoreAndDescendIfNecessary(fileToCheck, pathSegments, isDirectory)
                : false;
    }

    private boolean checkAgainstCurrentGitIgnoreAndDescendIfNecessary(
            File fileToCheck,
            Iterator<File> pathSegments,
            boolean isDirectory) {

        File current = pathSegments.next();
        if (current.getName().equals(".git")) {
            return true;
        }

        File currentGitIgnore = new File(current, GITIGNORE_FILENAME);

        if (currentGitIgnore.exists()) {
            URI relativeURI = current.toURI().relativize(fileToCheck.toURI());
            switch (getMatchResult(relativeURI.getPath(), currentGitIgnore, isDirectory)) {
                case CHECK_PARENT:
                    return descendInSearchOfGitIgnoreFile(fileToCheck, pathSegments, isDirectory);
                case IGNORED:
                    return true;
                case NOT_IGNORED:
                default:
                    return false;
            }
        } else {
            return descendInSearchOfGitIgnoreFile(fileToCheck, pathSegments, isDirectory);
        }
    }

    protected abstract MatchResult getMatchResult(String pathToCheck, File currentGitIgnore, boolean isDirectory);
}
