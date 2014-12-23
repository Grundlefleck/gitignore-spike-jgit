package org.mutabilitydetector;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mutabilitydetector.GitIgnoreTestFrame.VcsIgnoredMatcher.ignoredBy;

public abstract class GitIgnoreTestFrame {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    abstract VcsIgnores provideImplementation(String path);

    @Test public void honoursGitIgnoreConfig() throws IOException {
        ensureIgnoredFilesExist();
        VcsIgnores gitIgnores = provideImplementation(new File("").getAbsolutePath());
        assertExpectedIgnores(gitIgnores);
    }

    @Test public void returnsFalseWhenNoGitIgnoreFileExistsAnywhere() throws Exception {
        folder.create();
        File projectFolder = folder.newFolder("not-a-git-project");
        folder.newFile("not-a-git-project/some-file.txt");

        VcsIgnores gitIgnores = provideImplementation(projectFolder.getAbsolutePath());

        assertThat("not-a-git-project/some-file.txt", is(not(ignoredBy(gitIgnores))));
    }


    // honours negation
    // honours comments
    // honours escaping comments
    // ignores trailing spaces
    // can escape negation
    // removes ending slash from a pattern
    // ignores everything in .git directory
    // honours two asterisk for arbitrary depth (**)



    
    private void assertExpectedIgnores(VcsIgnores gitIgnores) {
    	assertThat("src/main/resources/ignored.txt", is(ignoredBy(gitIgnores)));
    	assertThat("src/main/resources/ignored.txt", is(ignoredBy(gitIgnores)));
    	assertThat("src/main/resources/not-ignored.txt", is(not(ignoredBy(gitIgnores))));

    	assertThat("src/main/resources/ignored-directory", is(ignoredBy(gitIgnores)));
    	assertThat("src/main/resources/ignored-directory/ignored-file-in-ignored-dir.txt", is(ignoredBy(gitIgnores)));
    	assertThat("src/main/resources/ignored-directory/not-ignored-file-in-ignored-dir.txt", is(ignoredBy(gitIgnores)));
    	assertThat("src/main/resources/ignored-with-wildcard.txt", is(ignoredBy(gitIgnores)));
    	assertThat("src/main/resources/not-ignored-directory/ignored-in-not-ignored-dir.txt", is(ignoredBy(gitIgnores)));
    	
    	assertThat("src/main/resources/not-ignored-directory/not-ignored-in-not-ignored-dir.txt", is(not(ignoredBy(gitIgnores))));
    }

    private void ensureIgnoredFilesExist() throws IOException {
        new File("src/main/resources/ignored.txt").createNewFile();
        new File("src/main/resources/ignored-with-wildcard.txt").createNewFile();
        new File("src/main/resources/ignored-directory").mkdirs();
        new File("src/main/resources/ignored-directory/ignored-file-in-ignored-dir.txt").createNewFile();
        new File("src/main/resources/ignored-directory/not-ignored-file-in-ignored-dir.txt").createNewFile();
        new File("src/main/resources/not-ignored-directory/ignored-in-not-ignored-dir.txt").createNewFile();
    }

    public static class WalkingFileSystem extends GitIgnoreTestFrame {
        @Override VcsIgnores provideImplementation(String path) {
            return GitIgnoresByWalkingFileSystem.fromRootDir(path);
        }
    }

    public static class JGit extends GitIgnoreTestFrame {
        @Override VcsIgnores provideImplementation(String path) {
            return GitIgnoresByApplyingIgnoreRuleDirectlyOnFilter.fromRootDir(path);
        }
    }

    public static class ByGlob extends GitIgnoreTestFrame {
        @Override VcsIgnores provideImplementation(String path) {
            return GitIgnoresByGlob.fromRootDir(path);
        }
    }
    
    public static class VcsIgnoredMatcher extends TypeSafeDiagnosingMatcher<String> {
    	
    	private VcsIgnores vcsIgnores;

		public VcsIgnoredMatcher(VcsIgnores vcsIgnores) {
			this.vcsIgnores = vcsIgnores;
		}
		
		public static VcsIgnoredMatcher ignoredBy(VcsIgnores vcsIgnores) {
			return new VcsIgnoredMatcher(vcsIgnores);
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("a file that was ignored");
		}

		@Override
		protected boolean matchesSafely(String item, Description mismatchDescription) {
			if (!new File(item).exists()) {
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
}
