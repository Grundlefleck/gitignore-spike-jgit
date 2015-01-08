package org.mutabilitydetector;

import org.eclipse.jgit.ignore.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.CHECK_PARENT;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.IGNORED;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.NOT_IGNORED;
import static org.mutabilitydetector.IgnoreRuleMatch.DOES_NOT_MATCH;
import static org.mutabilitydetector.IgnoreRuleMatch.IS_IGNORED;
import static org.mutabilitydetector.IgnoreRuleMatch.IS_NOT_IGNORED;

public final class GitIgnoresWithJGit extends BaseGitIgnore {

	private GitIgnoresWithJGit(File rootDirectory) {
		super(FileBasedGitIgnore.root(rootDirectory));
	}

    private static final Map<MatchResult, IgnoreRuleMatch> JGIT_TO_IGNORE_RULE_MATCH = createMapping();

    private static Map<MatchResult, IgnoreRuleMatch> createMapping() {
        Map<MatchResult, IgnoreRuleMatch> mappings = new HashMap<>();
        mappings.put(IGNORED, IS_IGNORED);
        mappings.put(NOT_IGNORED, IS_NOT_IGNORED);
        mappings.put(CHECK_PARENT, DOES_NOT_MATCH);
        return Collections.unmodifiableMap(mappings);
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

    @Override
    protected IgnoreRuleMatch getMatchResult(String pathToCheck, IgnoreRules currentGitIgnore, boolean isDirectory) {
        try {
            List<org.eclipse.jgit.ignore.IgnoreRule> converted = new ArrayList<>();
            for (IgnoreRule rule: currentGitIgnore.rules()) {
                converted.add(from(rule));
            }
            IgnoreNode ignoreNode = new IgnoreNode(converted);
            return JGIT_TO_IGNORE_RULE_MATCH.get(ignoreNode.isIgnored(pathToCheck, isDirectory));
        } catch (IgnoreRules.FailedToRetrieveIgnoreRules failedToRetrieveIgnoreRules) {
            failedToRetrieveIgnoreRules.printStackTrace();
            return IS_NOT_IGNORED;
        }
    }



    private static org.eclipse.jgit.ignore.IgnoreRule from(IgnoreRule thisRule) {
        return new org.eclipse.jgit.ignore.IgnoreRule(thisRule.definition());
    }
}
