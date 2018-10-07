package org.dgy.lucene.tfidf;

/**
 * @auther: dgy
 * @date: 2018/10/7
 * @version:
 */
public final class Main {

	private static final String DOCS_PATH = "docs";

	private static final String INDEX_PATH = "indexes";

	public static final void main(String args[]) {
		DocIndexBuilder docIndexBuilder = new DocIndexBuilder(DOCS_PATH, INDEX_PATH);
		docIndexBuilder.build(DocIndexBuilder.Mode.CREATE);

		TFIDFReader tfidfReader = new TFIDFReader(INDEX_PATH);
		tfidfReader.read();
	}
}
