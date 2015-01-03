package org.mutabilitydetector;


import org.mutabilitydetector.IgnoreRules.FailedToRetrieveIgnoreRules;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.mutabilitydetector.IgnoreRuleMatch.DOES_NOT_MATCH;
import static org.mutabilitydetector.IgnoreRuleMatch.IS_NOT_IGNORED;

public class GitIgnoresByGlob extends BaseGitIgnore {

    public GitIgnoresByGlob(RepositoryRoot rootDirectory) {
        super(rootDirectory);
    }

    public static VcsIgnores fromRootDir(String absolutePath) {
        return new GitIgnoresByGlob(FileBasedGitIgnore.root(new File(absolutePath)));
    }

    protected IgnoreRuleMatch getMatchResult(String pathToCheck, IgnoreRules currentGitIgnore, boolean isDirectory) {
        try {
            List<IgnoreRule> ignoreEntries = currentGitIgnore.rules();4n9rygQHrT4UXzIAjwgn
            List<IgnoreRuleMatch> allResults = getAllResults(pathToCheck, isDirectory, ignoreEntries);

            return getLastRelevantResult(allResults);
        } catch (FailedToRetrieveIgnoreRules e) {
            return IS_NOT_IGNORED;
        }
    }

    private List<IgnoreRuleMatch> getAllResults(String pathToCheck, boolean isDirectory, List<IgnoreRule> ignoreEntries) {
        List<IgnoreRuleMatch> allResults = new ArrayList<>();
        for (IgnoreRule rule: ignoreEntries) {
            allResults.add(rule.check(pathToCheck, isDirectory));
        }
        return allResults;
    }

    private IgnoreRuleMatch getLastRelevantResult(List<IgnoreRuleMatch> allResults) {
        Deque<IgnoreRuleMatch> relevantResults = new LinkedList<>();

        for (IgnoreRuleMatch result: allResults) {
            switch (result) {
                case IS_IGNORED:
                case IS_NOT_IGNORED:
                    relevantResults.add(result);
                    break;
                case DOES_NOT_MATCH:
                default:
                    // Filter out
            }
        }

        return relevantResults.isEmpty() ? DOES_NOT_MATCH : relevantResults.getLast();
    }


    static interface GitIgnoreMatcher {
        boolean matches(String path);
    }

    static final class GlobRegexMatcher implements GitIgnoreMatcher {

        private final Pattern pattern;

        GlobRegexMatcher(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(String path) {
            return pattern.matcher(path).matches();
        }
    }

    static GitIgnoreMatcher NEVER_MATCHES = new GitIgnoreMatcher() {
        @Override
        public boolean matches(String path) {
            return false;
        }
    };

    public static final class GitIgnoreRule implements IgnoreRule {

        private final boolean matchesDirectory;
        private final String entry;
        private final GitIgnoreMatcher matcher;
        private final boolean isGlob;
        private final boolean isNegated;

        protected GitIgnoreRule(String entry) {
            this.isNegated = entry.startsWith("!");
            String withNegationStripped = isNegated ? entry.substring(1, entry.length()) : entry;
            this.entry = ensureStartingSlash(withNegationStripped);
            this.isGlob = entry.contains("*");
            this.matchesDirectory = entry.endsWith("/");
            this.matcher = isGlob ? createPatternFrom(this.entry): NEVER_MATCHES;
        }

        private GitIgnoreMatcher createPatternFrom(String glob) {
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < glob.length(); i++) {
                char c = glob.charAt(i);

                switch(c) {
                    case '*':
                        regex.append('.');
                        break;
                    case '.':
                    case '$':
                    case '(':
                    case ')':
                    case '|':
                    case '+':
                    case '^':
                        regex.append('\\');
                        break;
                    case '\\':
                        i++;
                        char escaped = glob.charAt(i);
                        regex.append('\\').append(escaped);
                        continue;
                }

                regex.append(c);
            }
            return new GlobRegexMatcher(Pattern.compile(regex.toString()));
        }

        @Override
        public IgnoreRuleMatch check(String path, boolean isDirectory) {
            boolean matchesBeforeNegation = matchesBeforeNegation(path, isDirectory);

            if (matchesBeforeNegation) {
                return isNegated ? IgnoreRuleMatch.IS_NOT_IGNORED : IgnoreRuleMatch.IS_IGNORED;
            } else {
                return IgnoreRuleMatch.DOES_NOT_MATCH;
            }
        }

        private boolean matchesBeforeNegation(String path, boolean isDirectory) {
            path = ensureStartingSlash(path);

            if (this.matchesDirectory) {
                if (isDirectory) {
                    return ensureEndingSlash(path).startsWith(entry);
                } else {
                    return path.startsWith(entry);
                }
            }

            if (isGlob) {
                return this.matcher.matches(path);
            } else {
                return ensureStartingSlash(path).equals(entry);
            }
        }

        private String ensureStartingSlash(String path) {
            return path.startsWith("/") ? path : "/" + path;
        }

        private String ensureEndingSlash(String path) {
            return path.endsWith("/") ? path : path + "/";
        }

        @Override
        public String toString() {
            return String.format("IgnoreRule[entry=%s, negated=%s, isGlob=%s, matchesDirectory=%s]",
                    this.entry, this.isNegated, this.isGlob, this.matchesDirectory);
        }


    }

}

