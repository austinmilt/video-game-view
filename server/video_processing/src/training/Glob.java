/**
 * This class was copied from http://javapapers.com/java/glob-with-java-nio/
 */

package training;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 * This class finds all files matching a pattern in a directory using glob-
 * style matching.
 * <p>
 * This class was copied from http://javapapers.com/java/glob-with-java-nio/
 */
public class Glob {

    public static Path[] match(String location, String pattern) throws IOException {

        String glob = "glob:" + pattern;
        ArrayList<Path> matches = new ArrayList<>();

        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(
                glob);

        Files.walkFileTree(Paths.get(location), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path,
                                             BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(path)) {
                    matches.add(path);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        return matches.toArray(new Path[matches.size()]);
    }

}