package org.mutabilitydetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.ignore.IgnoreNode.MatchResult;

public class GitIgnoresByApplyingIgnoreRuleDirectlyOnFilter implements VcsIgnores {

	private IgnoreNode ignoreNode;

	public GitIgnoresByApplyingIgnoreRuleDirectlyOnFilter(IgnoreNode ignoreNode) {
		this.ignoreNode = ignoreNode;
	}

	@Override
	public boolean isIgnored(String relativePath) {
		return ignoreNode.isIgnored(relativePath, new File(relativePath).isDirectory()) == MatchResult.IGNORED;
	}

	public static VcsIgnores fromRootDir(String absolutePath) throws IOException {
		File gitignore = new File(absolutePath + "/.gitignore");
		IgnoreNode ignoreNode = new IgnoreNode();
		try(InputStream in = new FileInputStream(gitignore)) {
			ignoreNode.parse(in);
		}
		
		return new GitIgnoresByApplyingIgnoreRuleDirectlyOnFilter(ignoreNode);
	}

}
