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

        gitFolder.mkdir("ignored-directory");
        gitFolder.appendToGitignore("/ignored-directory/");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("ignored-directory", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void ignoresFilesWithinIgnoredDirectory() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkdir("ignored-directory");
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
        gitFolder.mkdir("ignored-directory");
        gitFolder.appendToGitignore("ignored-*\n");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("ignored-directory", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("ignored-with-wildcard.txt", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void ignoresFilesWithinUnignoredDirectory() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkdir("not-ignored-directory");
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

    @Test public void canIgnoreWithWildcardForDirectory() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkdir("folder");
        gitFolder.mkdir("folder/where");
        gitFolder.mkdir("folder/where/some");
        gitFolder.mkdir("folder/where/some/level");
        gitFolder.mkdir("folder/where/some/level/is-ignored");
        gitFolder.mkFileIn("folder/where/some/level/is-ignored/ignored-file.txt");
        gitFolder.mkFileIn("folder/where/some/level/is-ignored/not-ignored-file.txt");
        gitFolder.appendToGitignore("folder/where/some/level/*/ignored-file.txt");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("folder/where/some/level/is-ignored/ignored-file.txt", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("folder/where/some/level/is-ignored/not-ignored-file.txt", is(not(ignoredBy(gitIgnores, rootDir))));
    }

    @Test public void canIgnoreWithDoubleWildcardForArbitraryDepth() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkdir("folder");
        gitFolder.mkdir("folder/where");
        gitFolder.mkdir("folder/where/some");
        gitFolder.mkdir("folder/where/some/level");
        gitFolder.mkdir("folder/where/some/level/is-ignored");
        gitFolder.mkFileIn("folder/where/some/level/is-ignored/ignored1.class");
        gitFolder.mkFileIn("folder/where/some/level/is-ignored/ignored2.class");
        gitFolder.mkFileIn("folder/where/some/level/is-ignored/not-ignored-file.java");
        gitFolder.appendToGitignore("**.class");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("folder/where/some/level/is-ignored/ignored1.class", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("folder/where/some/level/is-ignored/ignored2.class", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("folder/where/some/level/is-ignored/not-ignored-file.java", is(not(ignoredBy(gitIgnores, rootDir))));
    }

    @Test public void canIgnoreWithDoubleWildcardForArbitraryDepthWithinADirectory() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();

        gitFolder.mkdir("folder");
        gitFolder.mkdir("folder/where");
        gitFolder.mkdir("folder/where/some");
        gitFolder.mkdir("folder/where/some/level");
        gitFolder.mkdir("folder/where/some/level/is-ignored");
        gitFolder.mkFileIn("folder/where/some/level/is-ignored/ignored1.class");
        gitFolder.mkFileIn("folder/where/some/level/is-ignored/ignored2.class");
        gitFolder.mkFileIn("folder/where/some/level/is-ignored/not-ignored-file.java");
        gitFolder.appendToGitignore("folder/*/is-ignored/*.class");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("folder/where/some/level/is-ignored/ignored1.class", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("folder/where/some/level/is-ignored/ignored2.class", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("folder/where/some/level/is-ignored/not-ignored-file.java", is(not(ignoredBy(gitIgnores, rootDir))));
    }

    @Test public void canNegatePreviouslyIgnoredMatches() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();
        gitFolder.mkdir("folder");
        gitFolder.mkFileIn("folder/ignored-file.txt");
        gitFolder.mkFileIn("folder/ignore-is-negated.txt");
        gitFolder.appendToGitignore("folder/*");
        gitFolder.appendToGitignore("!folder/ignore-is-negated.txt");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("folder/ignored-file.txt", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("folder/ignore-is-negated.txt", is(not(ignoredBy(gitIgnores, rootDir))));
    }

    @Test public void commentedEntriesAreNotUsed() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();
        gitFolder.mkdir("folder");
        gitFolder.mkFileIn("folder/ignore-is-commented.txt");
        gitFolder.appendToGitignore("#folder/ignore-is-commented.txt");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("folder/ignore-is-commented.txt", is(not(ignoredBy(gitIgnores, rootDir))));
    }

    @Test public void negationAndCommentCanBeEscaped() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();
        gitFolder.mkdir("!folder");
        gitFolder.mkdir("#folder");
        gitFolder.mkFileIn("!folder/ignored.txt");
        gitFolder.mkFileIn("#folder/ignored.txt");
        gitFolder.appendToGitignore("\\#folder/*");
        gitFolder.appendToGitignore("\\!folder/*");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());

        assertThat("#folder/ignored.txt", is(ignoredBy(gitIgnores, rootDir)));
        assertThat("!folder/ignored.txt", is(ignoredBy(gitIgnores, rootDir)));
    }


    @Test public void globMetaCharactersCanBeEscaped() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();
        String weirdDirName = "[^$.?]\\*folder";
        gitFolder.mkdir(weirdDirName);
        gitFolder.mkFileIn(weirdDirName + "/ignored.txt");
        gitFolder.appendToGitignore("\\[\\^\\$\\.\\?\\]\\\\\\*folder/ignored.txt");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());
        assertThat(weirdDirName +"/ignored.txt", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void regexMetaCharactersAreEscaped() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();
        String weirdDirName = "()@+|%.$^folder";
        gitFolder.mkdir(weirdDirName);
        gitFolder.mkFileIn(weirdDirName + "/ignored.txt");
        gitFolder.appendToGitignore("()@+|%.$^folder/*.txt");

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());
        assertThat(weirdDirName +"/ignored.txt", is(ignoredBy(gitIgnores, rootDir)));
    }

    @Test public void closerGitignoreFilesTakePrecedence() throws Exception {
        File rootDir = gitFolder.getRepoDirectory();
        gitFolder.mkdir("folder");
        gitFolder.mkdir("folder/subfolder");
        gitFolder.mkFileIn("folder/subfolder/ignored.txt");
        gitFolder.appendToGitignore("folder/subfolder/*.txt");
        gitFolder.appendToGitignore("!subfolder/*.txt", new File(gitFolder.getRepoDirectory(), "folder"));

        VcsIgnores gitIgnores = provideImplementation(rootDir.getAbsolutePath());
        assertThat("folder/subfolder/ignored.txt", is(not(ignoredBy(gitIgnores, rootDir))));
    }

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

        @Override
        public void before() throws Throwable {
            super.before();

            File rootDir = getRoot();

            try {
                initGitRepoIn(rootDir);
            } catch (GitAPIException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void after() {
//            super.after();
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
            appendToGitignore(gitignoreContent, getRepoDirectory());
        }

        public void appendToGitignore(String gitignoreContent, File containingDirectory) throws IOException {
            Iterable<CharSequence> contents = Arrays.<CharSequence>asList(gitignoreContent.split("\n"));
            Files.write(
                    Paths.get(new File(containingDirectory, Constants.GITIGNORE_FILENAME).toURI()),
                    contents,
                    Charset.forName("UTF-8"),
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
        }


        public void mkFileIn(String file) throws IOException {
            assertTrue(new File(getRepoDirectory(), file).createNewFile());
        }

        public void mkdir(String directory) {
            assertTrue(new File(getRepoDirectory(), directory).mkdir());
        }
    }
}
