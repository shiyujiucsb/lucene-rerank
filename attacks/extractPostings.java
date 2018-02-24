/*****************************************************************/
/* Copyright 2013 Code Strategies                                */
/* This code may be freely used and distributed in any project.  */
/* However, please do not remove this credit if you publish this */
/* code in paper or electronic form, such as on a web site.      */
/*****************************************************************/

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.nio.file.Paths;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.HashSet;

import org.tartarus.snowball.ext.EnglishStemmer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class extractPostings {
	public static final int TOP = 100000000;
	public static final String INDEX_DIRECTORY = "./trec45-index";

	public static final String FIELD_DOCID = "docid";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_BODY  = "body";

	public static HashSet<String> terms = new HashSet<String>();

	public static String normalize(String query) {
		StringBuilder sb = new StringBuilder();
		boolean isSeparated = true;
		for (char c: query.toCharArray()) {
			if (Character.isLetter(c) || Character.isDigit(c)) { 
				sb.append(Character.toLowerCase(c));
				isSeparated = false;
			}
			else if (isSeparated == false) {
				sb.append(' ');
				isSeparated = true;
			}
		}
		return sb.toString().trim();
	}

	public static void main(String[] args) throws Exception {
		BufferedReader  br = new BufferedReader(
					new FileReader("queries.04robust.301-450_601-700.txt"));
		String line;
		while((line = br.readLine()) != null) {
			String qid = line.substring(0, line.indexOf(" "));
			String orgQuery = line.substring(line.indexOf(" ")+1, line.length());
			String normQuery = normalize(orgQuery);
			for (String term : normQuery.split(" ", 0)) {
				if(term.length() > 0) terms.add(term);
			}
		}
		for (String term : terms) {
			searchIndex(term);
		}
	}

	public static void searchIndex(String term) throws Exception {
		String bothQuery = FIELD_TITLE + ":" + term
					 + " OR " + FIELD_BODY + ":" + term;
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(indexReader);
		searcher.setSimilarity(new BM25Similarity());

		CharArraySet emptyStopwords = new CharArraySet(0, true);
		Analyzer analyzer = new StandardAnalyzer(emptyStopwords);
		QueryParser queryParser = new QueryParser(FIELD_DOCID, analyzer);
		Query queryAll = queryParser.parse(bothQuery);
		TopDocs topDocs = searcher.search(queryAll, TOP);
		ScoreDoc[] hits = topDocs.scoreDocs;

		StringBuilder sb = new StringBuilder(term);
		for (int i=0; i<Math.min(TOP, topDocs.totalHits); i++) {
			Document document = searcher.doc(hits[i].doc);
			String docid = document.getField("docid").stringValue();
			sb.append(" " + docid);
		}
		System.out.println(sb.toString());
	}

}

