package org.mutabilitydetector;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class GitFolder extends TemporaryFolder {

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
