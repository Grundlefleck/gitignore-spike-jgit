package org.mutabilitydetector;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VcsIgnoredMatcher extends TypeSafeDiagnosingMatcher<String> {

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
