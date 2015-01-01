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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.CHECK_PARENT;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.NOT_IGNORED;
import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.IGNORED;
import static org.eclipse.jgit.lib.Constants.GITIGNORE_FILENAME;

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

            for (IgnoreRule rule: ignoreEntries) {
                if (rule.matches(pathToCheck, isDirectory)) {
                    return IGNORED;
                }
            }
            return CHECK_PARENT;
        } catch (IOException e) {
            return NOT_IGNORED;
        } finally {
           closeQuietly(in);
        }
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

    private static final class IgnoreRule {

        private final boolean matchesDirectory;
        private final String entry;
        private final Pattern globPattern;

        protected IgnoreRule(String text) {
            this.matchesDirectory = text.endsWith("/");
            this.entry = ensureStartingSlash(text);
            this.globPattern = createPatternFrom(text);
        }

        private Pattern createPatternFrom(String text) {
            return null;
        }

        public boolean matches(String path, boolean isDirectory) {
            path = ensureStartingSlash(path);

            if (this.matchesDirectory) {
                if (isDirectory) {
                    return ensureEndingSlash(path).startsWith(entry);
                } else {
                    return path.startsWith(entry);
                }
            }

            if (ensureStartingSlash(path).equals(entry)) {
                return true;
            }

            return false;
        }

        private String ensureStartingSlash(String path) {
            return path.startsWith("/") ? path : "/" + path;
        }

        private String ensureEndingSlash(String path) {
            return path.endsWith("/") ? path : path + "/";
        }
    }

}

