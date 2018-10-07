package org.dgy.lucene.tfidf;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * retrieval tf-idf from index
 *
 * @auther: dgy
 * @date: 2018/10/7
 * @version: 1.0
 */
public final class TFIDFReader {

	private final String indexPath;

	private Map<String, Integer> docFreq = new HashMap<>();

	private Map<String, Map<Integer, Integer>> termFreq = new HashMap<>();

	public TFIDFReader(String indexPath) {
		this.indexPath = indexPath;
	}

	public void read() {
		try {
			Directory dir = FSDirectory.open(FileUtils.getPathRelativeToClasspath(indexPath));
			IndexReader indexReader = DirectoryReader.open(dir);
			int maxDoc = indexReader.maxDoc();
			for (int docId = 0; docId < maxDoc; ++docId) {
				Terms terms = indexReader.getTermVector(docId, "contents");
				if (terms == null) {
					continue;
				}
				TermsEnum termsEnum = terms.iterator();
				BytesRef bytesRef;
				while ((bytesRef = termsEnum.next()) != null) {
					String text = bytesRef.utf8ToString();
					int num = docFreq.getOrDefault(text, 0);
					docFreq.put(text, num + 1);

					PostingsEnum postings = termsEnum.postings(null, PostingsEnum.FREQS);
					postings.nextDoc();
					termFreq.computeIfAbsent(text, k -> new HashMap<>()).put(docId, postings.freq());
				}
			}

			System.out.println("==================DOCUMENT FREQUENCY===================");
			for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
				System.out.println(entry.getKey() + ": " + entry.getValue());
			}

			System.out.println("==================TERM FREQUENCY===================");
			for (Map.Entry<String, Map<Integer, Integer>> entry : termFreq.entrySet()) {
				System.out.println("Term " + entry.getKey() + ": ");
				for (Map.Entry<Integer, Integer> ele : entry.getValue().entrySet()) {
					Document document = indexReader.document(ele.getKey());
					System.out.println("  " + document.get("path") + ": " + ele.getValue());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
