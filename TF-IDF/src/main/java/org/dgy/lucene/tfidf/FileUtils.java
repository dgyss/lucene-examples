package org.dgy.lucene.tfidf;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Tools for file or directory operation
 *
 * @auther: dgy
 * @date: 2018/10/7
 * @version: 1.0
 */
public final class FileUtils {

    private FileUtils() {
    }

    public static Path getPathRelativeToClasspath(String path) {
        URL url = FileUtils.class.getClassLoader().getResource(path);
        if (url == null) {
            throw new RuntimeException("Failed to find path: " + path);
        }

        File file = new File(url.getPath());
        return file.toPath();
    }

}
