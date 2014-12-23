package org.mutabilitydetector;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mutabilitydetector.GitIgnoreTestFrame.VcsIgnoredMatcher.ignoredBy;

public abstract class GitIgnoreTestFrame {

    @Rule public TemporaryFolder folder = new TemporaryFolder();
    @Rule public GitFolder gitFolder = new GitFolder();

    abstract VcsIgnores provideImplementation(String path);

    public static class WalkingFileSystem extends GitIgnoreTestFrame {
        @Override VcsIgnores provideImplementation(String path) {
            return GitIgnoresByWalkingFileSystem.fromRootDir(path);
        }
    }

    public static class JGit extends GitIgnoreTestFrame {
        @Override VcsIgnores provideImplementation(String path) {
            return GitIgnoresWithJGit.fromRootDir(path);
        }
    }

    public static class ByGlob extends GitIgnoreTestFrame {
        @Override VcsIgnores provideImplementation(String path) {
            return GitIgnoresByGlob.fromRootDir(path);
        }
    }

    @Test public void ignoresFileWithExactMatch() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkFileIn("ignored.txt");
        gitFolder.appendToGitignore("ignored.txt");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("ignored.txt", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void doesNotIgnoreFileWhenThereAreNoMatches() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkFileIn("not-ignored.txt");
        gitFolder.appendToGitignore("ignored.txt");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("not-ignored.txt", is(not(ignoredBy(gitIgnores, rootDir))));
    }

    @Test public void ignoresDirectoryWithExactMatch() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkFileIn("ignored-directory");
        gitFolder.appendToGitignore("ignored-directory");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("ignored-directory", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void ignoresDirectoryWithTrailingSlash() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkdirIn("ignored-directory");
        gitFolder.appendToGitignore("/ignored-directory/");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("ignored-directory", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void ignoresFilesWithinIgnoredDirectory() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkdirIn("ignored-directory");
        gitFolder.mkFileIn("ignored-directory/ignored-file-in-ignored-dir.txt");
        gitFolder.mkFileIn("ignored-directory/not-ignored-file-in-ignored-dir.txt");
        gitFolder.appendToGitignore(
                "ignored-directory\n" +
                "ignored-directory/ignored-file-in-ignored-dir.txt");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("ignored-directory/ignored-file-in-ignored-dir.txt", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("ignored-directory/not-ignored-file-in-ignored-dir.txt", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void ignoresFilesWithWildcard() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkFileIn("ignored-with-wildcard.txt");
        gitFolder.mkdirIn("ignored-directory");
        gitFolder.appendToGitignore("ignored-*\n");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("ignored-directory", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("ignored-with-wildcard.txt", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void ignoresFilesWithinUnignoredDirectory() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkdirIn("not-ignored-directory");
        gitFolder.mkFileIn("not-ignored-directory/not-ignored-file.txt");
        gitFolder.mkFileIn("not-ignored-directory/ignored-file.txt");
        gitFolder.appendToGitignore("not-ignored-directory/ignored-file.txt\n");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("not-ignored-directory/ignored-file.txt", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("not-ignored-directory/not-ignored-file.txt", is(not(ignoredBy(gitIgnores, rootDir))));
    }

    @Test public void doesNotIgnoreWhenNoGitIgnoreFileExistsAnywhere() throws Exception {
        File projectFolder = folder.newFolder("not-a-git-project");
        folder.newFile("not-a-git-project/some-file.txt");

        VcsIgnores gitIgnores = provideImplementation(projectFolder.getAbsolutePath());

        assertThat("not-a-git-project/some-file.txt", is(not(ignoredBy(gitIgnores, projectFolder))));
    }

    @Test public void ignoresEverythingInDotGitDirectory() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();
        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());
        assertThat(".git", is(ignoredBy(gitIgnores, rootDir)));
        assertThat(".git/config", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void removesWhitespaceFromGitignoreEntries() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkFileIn("ignored.txt");
        gitFolder.appendToGitignore("     ignored.txt    \t\n");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("ignored.txt", is(ignoredBy(gitIgnores, rootDir)));
    }

    // honours negation
    // honours comments
    // honours escaping comments
    // ignores trailing spaces
    // can escape negation
    // can use regex meta characters
    // removes ending slash from a pattern
    // honours two asterisk for arbitrary depth (**)

    public static class VcsIgnoredMatcher extends TypeSafeDiagnosingMatcher<String> {
    	
    	private final VcsIgnores vcsIgnores;
        private final File rootDir;


        public VcsIgnoredMatcher(VcsIgnores vcsIgnores, File rootDir) {
			this.vcsIgnores = vcsIgnores;
            this.rootDir = rootDir;
        }
		
		public static VcsIgnoredMatcher ignoredBy(VcsIgnores vcsIgnores, File rootDir) {
			return new VcsIgnoredMatcher(vcsIgnores, rootDir);
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("a file that was ignored");
		}

		@Override
		protected boolean matchesSafely(String item, Description mismatchDescription) {
            Path path = Paths.get(rootDir.getAbsolutePath(), item);
			if (!path.toFile().exists()) {
				mismatchDescription.appendValue(item).appendText("did not exist as a file or directory");
				return false;
			}
			boolean isIgnored = vcsIgnores.isIgnored(item);
			
			if (!isIgnored) {
				mismatchDescription.appendValue(item).appendText(" was not ignored");
			}
			
			return isIgnored;
		}
    	
    }

    public static class GitFolder extends TemporaryFolder {
        public void create() throws IOException {
            super.create();

            File rootDir = getRoot();

            try {
                initGitRepoIn(rootDir);
            } catch (GitAPIException e) {
                throw new IOException(e);
            }
        }

        public File getRepoDirectory() {
            return getRoot();
        }

        private void initGitRepoIn(File rootDir) throws GitAPIException, IOException {
            Git.init().setDirectory(rootDir).setBare(false).call();
            Repository repository = FileRepositoryBuilder.create(new File(rootDir.getAbsolutePath(), ".git"));
            repository.close();
        }

        public void appendToGitignore(String gitignoreContent) throws IOException {
            Iterable<CharSequence> contents = Arrays.<CharSequence>asList(gitignoreContent.split("\n"));
            Files.write(
                    Paths.get(new File(getRepoDirectory(), Constants.GITIGNORE_FILENAME).toURI()),
                    contents,
                    Charset.forName("UTF-8"),
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
        }


        public void mkFileIn(String file) throws IOException {
            assertTrue(new File(getRepoDirectory(), file).createNewFile());
        }

        public void mkdirIn(String directory) {
            assertTrue(new File(getRepoDirectory(), directory).mkdir());
        }
    }
}
