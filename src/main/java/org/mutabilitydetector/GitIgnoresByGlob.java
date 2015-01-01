package org.mutabilitydetector;

import org.eclipse.jgit.ignore.IgnoreNode.MatchResult;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.CHECK_PARENT;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.NOT_IGNORED;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.IGNORED;

public class GitIgnoresByGlob extends BaseGitIgnore {

    public GitIgnoresByGlob(File rootDirectory) {
        super(rootDirectory);
    }

    public static VcsIgnores fromRootDir(String absolutePath) {
        return new GitIgnoresByGlob(new File(absolutePath));
    }

    protected MatchResult getMatchResult(String pathToCheck, File currentGitIgnore, boolean isDirectory) {
        InputStream in = null;
        try {
            in = new FileInputStream(currentGitIgnore);

            List<IgnoreRule> ignoreEntries = ignoreEntries(in);
            List<MatchResult> allResults = getAllResults(pathToCheck, isDirectory, ignoreEntries);
            return getLastRelevantResult(allResults);
        } catch (IOException e) {
            return NOT_IGNORED;
        } finally {
           closeQuietly(in);
        }
    }

    private List<MatchResult> getAllResults(String pathToCheck, boolean isDirectory, List<IgnoreRule> ignoreEntries) {
        List<MatchResult> allResults = new ArrayList<>();
        for (IgnoreRule rule: ignoreEntries) {
            allResults.add(rule.matches(pathToCheck, isDirectory));
        }
        return allResults;
    }

    private MatchResult getLastRelevantResult(List<MatchResult> allResults) {
        Deque<MatchResult> relevantResults = new LinkedList<>();
        
        for (MatchResult result: allResults) {
            switch (result) {
                case IGNORED:
                case NOT_IGNORED:
                    relevantResults.addFirst(result);
                    break;
                case CHECK_PARENT:
                default:
                    // Filter out
            }
        }

        return relevantResults.isEmpty() ? CHECK_PARENT : relevantResults.getFirst();
    }

    private void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // Ignore, as with Apache Commons IOUtils.closeQuietly.
        }
    }

    private static List<IgnoreRule> ignoreEntries(InputStream input) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
        List<IgnoreRule> rules = new ArrayList<IgnoreRule>();
        String text;
        while ((text = br.readLine()) != null) {
            rules.add(new IgnoreRule(text.trim()));
        }
        return rules;
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

    private static final class IgnoreRule {

        private final boolean matchesDirectory;
        private final String entry;
        private final GitIgnoreMatcher matcher;
        private final boolean isGlob;
        private final boolean isNegated;

        protected IgnoreRule(String entry) {
            this.isNegated = entry.startsWith("!");
            String withNegationStripped = isNegated ? entry.substring(1, entry.length()) : entry;
            this.entry = ensureStartingSlash(withNegationStripped);
            this.isGlob = entry.contains("*");
            this.matchesDirectory = entry.endsWith("/");
            this.matcher = isGlob ? createPatternFrom(this.entry): NEVER_MATCHES;
        }

        private GitIgnoreMatcher createPatternFrom(String glob) {
            StringBuilder regex = new StringBuilder();
            for (char c: glob.toCharArray()) {
                switch(c) {
                    case '*': regex.append('.');
                }

                regex.append(c);
            }
            return new GlobRegexMatcher(Pattern.compile(regex.toString()));
        }

        public MatchResult matches(String path, boolean isDirectory) {
            boolean matchesBeforeNegation = matchesBeforeNegation(path, isDirectory);

            if (matchesBeforeNegation) {
                return isNegated ? NOT_IGNORED : IGNORED;
            } else {
                return CHECK_PARENT;
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

