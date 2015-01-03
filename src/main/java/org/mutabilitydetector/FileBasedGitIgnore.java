package org.mutabilitydetector;

import org.mutabilitydetector.GitIgnoresByGlob.GitIgnoreRule;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.mutabilitydetector.IgnoreRuleMatch.IS_NOT_IGNORED;

public final class FileBasedGitIgnore {
    private FileBasedGitIgnore() {}

    public static RepositoryRoot root(File file) {
        return new GitRepositoryRoot(file);
    }

    public static class GitRepositoryFile implements RepositoryFile {
        protected final File file;

        public GitRepositoryFile(File file) {
            this.file = file;
        }

        @Override
        public RepositoryFile getParent() {
            return new GitRepositoryFile(file.getParentFile());
        }

        @Override
        public IgnoreRules getIgnoreRules() {
            return new GitIgnoreRules(new File(file, ".gitignore"));
        }

        @Override
        public RepositoryFile relativize(RepositoryFile descendant) {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isRoot() {
            return false;
        }

        @Override
        public boolean isInternal() {
            return false;
        }
    }

    public static final class GitRepositoryRoot extends GitRepositoryFile implements RepositoryRoot {

        public GitRepositoryRoot(File rootDirectory) {
            super(rootDirectory);
        }

        @Override
        public RepositoryFile fromPath(String path) {
            return new GitRepositoryFile(new File(file, path));
        }
    }

    public static final class GitIgnoreRules implements IgnoreRules {

        private final File dotGitignoreFile;

        public GitIgnoreRules(File dotGitignoreFile) {
            this.dotGitignoreFile = dotGitignoreFile;
        }

        @Override
        public boolean exists() {
            return dotGitignoreFile.exists();
        }

        @Override
        public List<IgnoreRule> rules() throws FailedToRetrieveIgnoreRules {
            InputStream in = null;
            try {
                return ignoreEntries(in);
            } catch (IOException e) {
                throw new FailedToRetrieveIgnoreRules(e);
            } finally {
                closeQuietly(in);
            }
        }

        private static List<IgnoreRule> ignoreEntries(InputStream input) throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
            List<IgnoreRule> rules = new ArrayList<IgnoreRule>();
            String text;
            while ((text = br.readLine()) != null) {
                rules.add(new GitIgnoreRule(text.trim()));
            }
            return rules;
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
    }


}
