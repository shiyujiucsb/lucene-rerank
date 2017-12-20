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

import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class searchIndex {

	public static final String FILE_TO_INDEX_DIRECTORY = "./lines-trec45-unprocessed.txt";
	public static final String INDEX_DIRECTORY = "./index";

	public static final String FIELD_DOCID = "docid";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_BODY  = "body";

	public static String normalize(String query) {
		StringBuilder sb = new StringBuilder();
		for (char c: query.toCharArray()) {
			if (Character.isLetter(c)) sb.append(Character.toLowerCase(c));
			else sb.append(' ');
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
			searchIndex(qid, normQuery);
		}
	}

	public static float getMinDistance(IndexSearcher searcher, QueryParser queryParser, int doc, 
					String field, String w1, String w2) throws Exception {
		for (int distance = 1; distance <= 100; distance++) {
			Query proximityQuery = queryParser.parse(field + ":\"" + w1 + " " + w2 + "\"~" + distance);
			if (searcher.explain(proximityQuery, doc).getValue() > .01)
				return (float)(1.0 / distance);
		}
		return 0;
	}

	public static void searchIndex(String qid, String searchString) throws Exception {
		StringBuilder titleQuery = new StringBuilder();
		StringBuilder bodyQuery = new StringBuilder();
		StringTokenizer stQuery = new StringTokenizer(searchString);
		Vector<String> queryTerms = new Vector<String>();
		while (stQuery.hasMoreTokens()) {
			queryTerms.add(stQuery.nextToken());
		}
		int queryLength = queryTerms.size();
		boolean start = true;
		for (String term : queryTerms) {
			if (start) start = false;
                        else {
				titleQuery.append(" OR ");
				bodyQuery.append(" OR ");
			}
			titleQuery.append("title:" + term + " ");
                        bodyQuery.append("body:" + term + " ");
                }

		//System.out.println("Searching for '" + searchString + "'");
		String submittedQuery = "(" + titleQuery.toString()
					 + ") OR (" + bodyQuery.toString() + ")";
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(indexReader);

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new QueryParser(FIELD_BODY, analyzer);
		Query query = queryParser.parse(submittedQuery);
		Query queryTitleOnly = queryParser.parse(titleQuery.toString());
		Query queryBodyOnly = queryParser.parse(bodyQuery.toString());
		TopDocs topDocs = searcher.search(query, 2000);
		ScoreDoc[] hits = topDocs.scoreDocs;
		//System.out.println("Number of hits: " + topDocs.totalHits);

		for (int i=0; i<Math.min(2000, topDocs.totalHits); i++) {
			Document document = searcher.doc(hits[i].doc);
			String docid = document.getField("docid").stringValue();
			searcher.setSimilarity(new BM25Similarity());
			float bm25Title = searcher.explain(queryTitleOnly, hits[i].doc).getValue();
			searcher.setSimilarity(new ClassicSimilarity());
			float tfidfTitle = searcher.explain(queryTitleOnly, hits[i].doc).getValue();
			searcher.setSimilarity(new BM25Similarity());
                        float bm25Body = searcher.explain(queryBodyOnly, hits[i].doc).getValue();
                        searcher.setSimilarity(new ClassicSimilarity());
                        float tfidfBody = searcher.explain(queryBodyOnly, hits[i].doc).getValue();
			searcher.setSimilarity(new BM25Similarity());
                        float bm25 = searcher.explain(query, hits[i].doc).getValue();
                        searcher.setSimilarity(new ClassicSimilarity());
                        float tfidf = searcher.explain(query, hits[i].doc).getValue();

			float minTfidfTitle = Float.MAX_VALUE;
			float maxTfidfTitle = Float.MIN_VALUE;
			float avgTfidfTitle = 0;
                	start = true;
                	for (String term : queryTerms) {
                        	Query subQuery = queryParser.parse("title:" + term);
				float subTfidf = searcher.explain(subQuery, hits[i].doc).getValue();
				if (subTfidf < minTfidfTitle) minTfidfTitle = subTfidf;
				if (subTfidf > maxTfidfTitle) maxTfidfTitle = subTfidf;
				avgTfidfTitle += subTfidf;
                	}
			avgTfidfTitle /= queryLength;

			float minTfidfBody  = Float.MAX_VALUE;
			float maxTfidfBody  = Float.MIN_VALUE;
			float avgTfidfBody = 0;
                        start = true;
                        for (String term : queryTerms) {
                                Query subQuery = queryParser.parse("body:" + term);
                                float subTfidf = searcher.explain(subQuery, hits[i].doc).getValue();
                                if (subTfidf < minTfidfBody) minTfidfBody = subTfidf;
                                if (subTfidf > maxTfidfBody) maxTfidfBody = subTfidf;
                                avgTfidfBody += subTfidf;
                        }			
			avgTfidfBody /= queryLength;

			float minInvMinDistanceTitle = Float.MAX_VALUE;
			float maxInvMinDistanceTitle = Float.MIN_VALUE;
			float avgInvMinDistanceTitle = 0;
			float minInvMinDistanceBody  = Float.MAX_VALUE;
                        float maxInvMinDistanceBody  = Float.MIN_VALUE;
                        float avgInvMinDistanceBody  = 0;
			for (int k=0; k < queryLength-1; k++) {
				for (int j=k+1; j < queryLength; j++) {
					float invMinDistance = 
						getMinDistance(searcher, queryParser, hits[i].doc, 
								"title", queryTerms.get(k), queryTerms.get(j));
					if (invMinDistance > maxInvMinDistanceTitle)
						maxInvMinDistanceTitle = invMinDistance;
					if (invMinDistance < minInvMinDistanceTitle)
                                                minInvMinDistanceTitle = invMinDistance;
					avgInvMinDistanceTitle += invMinDistance;
					invMinDistance =
                                                getMinDistance(searcher, queryParser, hits[i].doc, 
								"body", queryTerms.get(k), queryTerms.get(j));
                                        if (invMinDistance > maxInvMinDistanceBody)
                                                maxInvMinDistanceBody = invMinDistance;
                                        if (invMinDistance < minInvMinDistanceBody)
                                                minInvMinDistanceBody = invMinDistance;
                                        avgInvMinDistanceBody += invMinDistance;
				}
			}
			if (queryLength > 2) {
				avgInvMinDistanceTitle /= queryLength * (queryLength-1) / 2;
				avgInvMinDistanceBody  /= queryLength * (queryLength-1) / 2;
			}
			else {
				maxInvMinDistanceTitle = minInvMinDistanceTitle = avgInvMinDistanceTitle = 0;
				maxInvMinDistanceBody = minInvMinDistanceBody = avgInvMinDistanceBody = 0;
			}
			System.out.println(qid + " " + docid 
					+ " 1:" + String.format("%.2f", maxTfidfTitle)
					+ " 2:" + String.format("%.2f", minTfidfTitle)
					+ " 3:" + String.format("%.2f", avgTfidfTitle)
					+ " 4:" + String.format("%.2f", tfidfTitle)
					+ " 5:" + String.format("%.2f", maxTfidfBody)
					+ " 6:" + String.format("%.2f", minTfidfBody)
					+ " 7:" + String.format("%.2f", avgTfidfBody)
					+ " 8:" + String.format("%.2f", tfidfBody)  
					+ " 9:" + String.format("%.2f", tfidf)      
					+ " 10:" + String.format("%.2f", bm25Title)
					+ " 11:" + String.format("%.2f", bm25Body)
					+ " 12:" + String.format("%.2f", bm25)
					+ " 13:" + String.format("%.2f", maxInvMinDistanceTitle)
					+ " 14:" + String.format("%.2f", minInvMinDistanceTitle)
					+ " 15:" + String.format("%.2f", avgInvMinDistanceTitle)
					+ " 16:" + String.format("%.2f", maxInvMinDistanceBody)
					+ " 17:" + String.format("%.2f", minInvMinDistanceBody)
					+ " 18:" + String.format("%.2f", avgInvMinDistanceBody)
				);
		}
	}

}

