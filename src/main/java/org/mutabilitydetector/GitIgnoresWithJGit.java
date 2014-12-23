package org.mutabilitydetector;

import org.eclipse.jgit.ignore.IgnoreNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;

import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.NOT_IGNORED;
import static org.eclipse.jgit.lib.Constants.GITIGNORE_FILENAME;

public final class GitIgnoresWithJGit implements VcsIgnores {

	private final File rootDirectory;

	private GitIgnoresWithJGit(File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

    public static VcsIgnores fromRootDir(String absolutePath) {
        return new GitIgnoresWithJGit(new File(absolutePath));
    }

	@Override
	public boolean isIgnored(String fileToCheck) {
        File absolutePath = new File(rootDirectory, fileToCheck);
        boolean isDirectory = absolutePath.isDirectory();
        File directoryContainingFileToCheck = isDirectory ? absolutePath : absolutePath.getParentFile();
        Iterator<File> pathSegments = createPathSegmentsFromRootTo(directoryContainingFileToCheck);
		return descendInSearchOfGitIgnoreFile(new File(fileToCheck), pathSegments, isDirectory);
    }

    private Iterator<File> createPathSegmentsFromRootTo(File file) {
        LinkedList<File> files = new LinkedList<File>();
        File currentFile = file;
        files.addFirst(currentFile);

        while (currentFile.getParent() != null && !currentFile.equals(rootDirectory)) {
            File parentFile = currentFile.getParentFile();
            files.addFirst(parentFile);
            currentFile = parentFile;
        }

        return files.iterator();
    }

    private boolean descendInSearchOfGitIgnoreFile(File fileToCheck, Iterator<File> pathSegments, boolean isDirectory) {
        return pathSegments.hasNext()
                ? checkAgainstCurrentGitIgnoreAndDescendIfNecessary(fileToCheck, pathSegments, isDirectory)
                : false;
    }

    private boolean checkAgainstCurrentGitIgnoreAndDescendIfNecessary(File fileToCheck, Iterator<File> pathSegments, boolean isDirectory) {

        File current = pathSegments.next();

        if (current.getName().equals(".git")) {
            return true;
        }

        File currentGitIgnore = new File(current, GITIGNORE_FILENAME);

        if (currentGitIgnore.exists()) {
            switch (getMatchResult(fileToCheck, currentGitIgnore, isDirectory)) {
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

    private MatchResult getMatchResult(File fileToCheck, File currentGitIgnore, boolean isDirectory) {
        try (InputStream in = new FileInputStream(currentGitIgnore)) {
            IgnoreNode ignoreNode = new IgnoreNode();
            ignoreNode.parse(in);
            return ignoreNode.isIgnored(fileToCheck.getPath(), isDirectory);
        } catch (IOException e) {
            return NOT_IGNORED;
        }
    }

}
