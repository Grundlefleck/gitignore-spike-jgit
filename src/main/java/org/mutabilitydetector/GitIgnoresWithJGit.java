package org.mutabilitydetector;

import org.eclipse.jgit.ignore.IgnoreNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.NOT_IGNORED;

public final class GitIgnoresWithJGit extends BaseGitIgnore {

	private GitIgnoresWithJGit(File rootDirectory) {
		super(rootDirectory);
	}

    public static VcsIgnores fromRootDir(String absolutePath) {
        return new GitIgnoresWithJGit(new File(absolutePath));
    }

    protected MatchResult getMatchResult(String pathToCheck, File currentGitIgnore, boolean isDirectory) {
        try (InputStream in = new FileInputStream(currentGitIgnore)) {
            IgnoreNode ignoreNode = new IgnoreNode();
            ignoreNode.parse(in);
            return ignoreNode.isIgnored(pathToCheck, isDirectory);
        } catch (IOException e) {
            return NOT_IGNORED;
        }
    }

}
