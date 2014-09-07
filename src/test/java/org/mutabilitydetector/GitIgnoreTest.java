package org.mutabilitydetector;

import org.eclipse.jgit.ignore.IgnoreRule;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class GitIgnoreTest {

    @Test public void unmatchedFilesAreNotConsideredIgnored() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("/ignored.txt");
        URI thisSourceFile = resource.toURI();
        System.out.println(new File("/home/gallan/dev/source/github/gitignore-spike/").getAbsolutePath());
        Repository repository = new FileRepository(new File(""));
        Repository unpushedRepository = new FileRepository(new File(""));
        FileTreeIterator fileTreeIterator = new FileTreeIterator(repository);
    }
}
