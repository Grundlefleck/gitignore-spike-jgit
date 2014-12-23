package org.mutabilitydetector;

import org.eclipse.jgit.ignore.IgnoreNode;

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

import static org.eclipse.jgit.ignore.IgnoreNode.MatchResult.NOT_IGNORED;
import static org.eclipse.jgit.lib.Constants.GITIGNORE_FILENAME;

public class GitIgnoresByGlob implements VcsIgnores {
    private final File rootDirectory;

    public GitIgnoresByGlob(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public static VcsIgnores fromRootDir(String absolutePath) {
        return new GitIgnoresByGlob(new File(absolutePath));
    }


    @Override
    public boolean isIgnored(String fileToCheck) {
        File absolutePath = new File(rootDirectory, fileToCheck);
        File directoryContainingFileToCheck = absolutePath.isDirectory() ? absolutePath : absolutePath.getParentFile();
        Iterator<File> pathSegments = createPathSegmentsFromRootTo(directoryContainingFileToCheck);
        return descendInSearchOfGitIgnoreFile(new File(fileToCheck), pathSegments);
    }

    private Iterator<File> createPathSegmentsFromRootTo(File file) {
        LinkedList<File> files = new LinkedList<File>();
        File currentFile = file;
        files.addFirst(currentFile);

        while (currentFile.getParent() != null && !currentFile.equals(rootDirectory)) {
            File parentFile = currentFile.getParentFile();
            files.addFirst(parentFile);
            currentFile = parentFile;
        }

        return files.iterator();
    }

    private boolean descendInSearchOfGitIgnoreFile(File fileToCheck, Iterator<File> pathSegments) {
        return pathSegments.hasNext()
                ? checkAgainstCurrentGitIgnoreAndDescendIfNecessary(fileToCheck, pathSegments)
                : false;
    }

    private boolean checkAgainstCurrentGitIgnoreAndDescendIfNecessary(File fileToCheck, Iterator<File> pathSegments) {
        File currentGitIgnore = new File(pathSegments.next(), GITIGNORE_FILENAME);

        if (currentGitIgnore.exists()) {
            switch (getMatchResult(fileToCheck, currentGitIgnore)) {
                case CHECK_PARENT:
                    return descendInSearchOfGitIgnoreFile(fileToCheck, pathSegments);
                case IGNORED:
                    return true;
                case NOT_IGNORED:
                default:
                    return false;
            }
        } else {
            return descendInSearchOfGitIgnoreFile(fileToCheck, pathSegments);
        }
    }

    private IgnoreNode.MatchResult getMatchResult(File fileToCheck, File currentGitIgnore) {
        InputStream in = null;
        try {
            in = new FileInputStream(currentGitIgnore);
            IgnoreNode ignoreNode = new IgnoreNode();
            ignoreNode.parse(in);
            return ignoreNode.isIgnored(fileToCheck.getPath(), fileToCheck.isDirectory());
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
            text = text.trim();
            if (text.length() > 0 && !text.startsWith("#") && !text.equals("/")) {
                rules.add(new IgnoreRule(text));
            }
        }
        return rules;
    }

    private static final class IgnoreRule {

        protected IgnoreRule(String text) {

        }
    }

}

