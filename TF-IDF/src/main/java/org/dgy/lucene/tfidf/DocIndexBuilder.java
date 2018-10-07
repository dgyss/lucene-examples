package org.dgy.lucene.tfidf;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Index builder for files in a directory
 *
 * @auther: dgy
 * @date: 2018/10/7
 * @version: 1.0
 */
public final class DocIndexBuilder {

	public enum Mode {
		CREATE,

		NONE_IF_EXIST,

		UPDATE_IF_EXIST
	}

	private final String docsPath;

	private final String indexPath;

	public DocIndexBuilder(String docsPath, String indexPath) {
		this.docsPath = docsPath;
		this.indexPath = indexPath;
	}

	public boolean build(Mode mode) {
		boolean exists = exists();
		if (exists && mode == Mode.NONE_IF_EXIST) {
			return true;
		}

		IndexWriterConfig indexWriterConfig = new IndexWriterConfig();
		if (exists && mode == Mode.UPDATE_IF_EXIST) {
			indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
		} else {
			indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		}

		final Path docDir = FileUtils.getPathRelativeToClasspath(docsPath);
		if (!Files.isReadable(docDir)) {
			throw new RuntimeException("Document directory '" + docDir.toAbsolutePath() + "' does not exist or is not readable, please check the path");
		}

		try {
			Directory dir = FSDirectory.open(FileUtils.getPathRelativeToClasspath(indexPath));
			IndexWriter writer = new IndexWriter(dir, indexWriterConfig);
			indexDocs(writer, docDir);
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	private boolean exists() {
		try {
			Directory dir = FSDirectory.open(FileUtils.getPathRelativeToClasspath(indexPath));
			IndexReader indexReader = DirectoryReader.open(dir);
			indexReader.close();
			return true;
		} catch (IOException ignore) {
		}
		return false;
	}

	static void indexDocs(final IndexWriter writer, Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
					} catch (IOException ignore) {
						// don't index files that can't be read.
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}

	/**
	 * Indexes a single document
	 */
	static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
		try (InputStream stream = Files.newInputStream(file)) {
			// make a new, empty document
			Document doc = new Document();

			// Add the path of the file as a field named "path".  Use a
			// field that is indexed (i.e. searchable), but don't tokenize
			// the field into separate words and don't index term frequency
			// or positional information:
			Field pathField = new StringField("path", file.getFileName().toString(), Field.Store.YES);
			doc.add(pathField);

			// Add the last modified date of the file a field named "modified".
			// Use a LongPoint that is indexed (i.e. efficiently filterable with
			// PointRangeQuery).  This indexes to milli-second resolution, which
			// is often too fine.  You could instead create a number based on
			// year/month/day/hour/minutes/seconds, down the resolution you require.
			// For example the long value 2011021714 would mean
			// February 17, 2011, 2-3 PM.
			doc.add(new LongPoint("modified", lastModified));

			// Add the contents of the file to a field named "contents".  Specify a Reader,
			// so that the text of the file is tokenized and indexed, but not stored.
			// Note that FileReader expects the file to be in UTF-8 encoding.
			// If that's not the case searching for special characters will fail.
			FieldType type = new FieldType();
			type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
			type.setStored(true);
			type.setStoreTermVectors(true);
			type.setTokenized(true);
			type.freeze();
			Field field = new Field("contents", new String(Files.readAllBytes(file), StandardCharsets.UTF_8), type);
			doc.add(field);

			if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
				// New index, so we just add the document (no old document can be there):
				System.out.println("adding " + file);
				writer.addDocument(doc);
			} else {
				// Existing index (an old copy of this document may have been indexed) so
				// we use updateDocument instead to replace the old one matching the exact
				// path, if present:
				System.out.println("updating " + file);
				writer.updateDocument(new Term("path", file.toString()), doc);
			}
		}
	}
}
