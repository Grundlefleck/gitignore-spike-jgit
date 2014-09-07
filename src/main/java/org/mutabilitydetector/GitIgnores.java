package org.mutabilitydetector;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class GitIgnores implements VcsIgnores {

    private final Set<String> unignoredResources;

    public GitIgnores(Set<String> unignoredResources) {
        this.unignoredResources = Collections.unmodifiableSet(unignoredResources);
    }

    static GitIgnores fromRootDir(String path) throws IOException {
        File gitDir = new File(path + "/.git");
        Repository repository = new FileRepository(gitDir);
        FileTreeIterator fileTreeIterator = new FileTreeIterator(repository);
        TreeWalk tw = new TreeWalk(repository);
        tw.setRecursive(true);
        tw.addTree(fileTreeIterator);
        tw.setFilter(new NotIgnoredFilter(0));

        Set<String> unignoredResources = new HashSet<String>();
        while (tw.next()) {
            System.out.println(tw.getPathString());
            unignoredResources.add(tw.getPathString());

        }
        return new GitIgnores(unignoredResources);
    }

    public boolean isIgnored(String relativePath) {
        return !unignoredResources.contains(relativePath);
    }
}
