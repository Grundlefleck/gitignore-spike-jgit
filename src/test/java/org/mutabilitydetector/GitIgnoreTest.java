package org.mutabilitydetector;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitIgnoreTest {

    @Test public void unmatchedFilesAreNotConsideredIgnored() throws IOException {
        ensureIgnoredFilesExist();

        VcsIgnores gitIgnores = GitIgnores.fromRootDir(new File("").getAbsolutePath());

        assertTrue(gitIgnores.isIgnored("src/main/resources/ignored.txt"));
        assertFalse(gitIgnores.isIgnored("src/main/resources/not-ignored.txt"));
        assertTrue(gitIgnores.isIgnored("src/main/resources/ignored-directory"));
        assertTrue(gitIgnores.isIgnored("src/main/resources/ignored-directory/ignored-file-in-ignored-dir.txt"));
        assertTrue(gitIgnores.isIgnored("src/main/resources/ignored-directory/unignored-file-in-ignored-dir.txt"));
        assertTrue(gitIgnores.isIgnored("src/main/resources/ignored-with-wildcard.txt"));
    }

    private void ensureIgnoredFilesExist() throws IOException {
        new File("src/main/resources/ignored.txt").createNewFile();
        new File("src/main/resources/ignored-with-wildcard.txt").createNewFile();
        new File("src/main/resources/ignored-directory").mkdirs();
        new File("src/main/resources/ignored-directory/ignored-file-in-ignored-dir.txt").createNewFile();
        new File("src/main/resources/ignored-directory/not-ignored-file-in-ignored-dir.txt").createNewFile();
    }
}
