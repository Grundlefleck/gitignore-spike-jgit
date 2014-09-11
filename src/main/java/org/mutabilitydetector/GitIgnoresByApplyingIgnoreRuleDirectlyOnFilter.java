package org.mutabilitydetector;

import org.eclipse.jgit.ignore.IgnoreNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.eclipse.jgit.lib.Constants.GITIGNORE_FILENAME;

public class GitIgnoresByApplyingIgnoreRuleDirectlyOnFilter implements VcsIgnores {

	private File rootDirectory;

	public GitIgnoresByApplyingIgnoreRuleDirectlyOnFilter(File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	@Override
	public boolean isIgnored(String fileToCheck) {
        File absolutePath = new File(rootDirectory, fileToCheck);
        File currentDirectory = absolutePath.isDirectory() ? absolutePath : absolutePath.getParentFile();
		return checkRecursivelyUpToProjectRoot(fileToCheck, currentDirectory);
	}

    private boolean checkRecursivelyUpToProjectRoot(String fileToCheck, File currentDirectory) {
        File currentGitIgnore = new File(currentDirectory, GITIGNORE_FILENAME);
        return (currentGitIgnore.exists())
                ? applyIgnoreRules(fileToCheck, currentGitIgnore)
                : checkRecursivelyUpToProjectRoot(fileToCheck, currentDirectory.getParentFile());
    }

    private boolean applyIgnoreRules(String fileToCheck, File currentGitIgnore) {
        try (InputStream in = new FileInputStream(currentGitIgnore)) {
            IgnoreNode ignoreNode = new IgnoreNode();
            ignoreNode.parse(in);
            switch (ignoreNode.isIgnored(fileToCheck, new File(fileToCheck).isDirectory())) {
                case IGNORED:
                    return true;
                case NOT_IGNORED:
                    return false;
                case CHECK_PARENT:
                    File parentDirectory = currentGitIgnore.getParentFile();

                    return parentDirectory.equals(rootDirectory)
                            ? false
                            : checkRecursivelyUpToProjectRoot(fileToCheck, parentDirectory.getParentFile());
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }


    public static VcsIgnores fromRootDir(String absolutePath) throws IOException {
        return new GitIgnoresByApplyingIgnoreRuleDirectlyOnFilter(new File(absolutePath));
	}

}
