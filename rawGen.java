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

import org.tartarus.snowball.ext.EnglishStemmer;
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
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class rawGen {
	public static final int TOP = 2000;

	public static final String INDEX_DIRECTORY = "./trec45-index";

	public static final String FIELD_DOCID = "docid";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_BODY  = "body";

	// assume most stopwords (e.g. of, in, on, at) have length < 3
	public static String removeStopwords(String string) {
		StringTokenizer st = new StringTokenizer(string);
		StringBuilder sb = new StringBuilder();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.length() > 2) {
				sb.append(token);
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	public static String stemming(String string) {
		StringTokenizer st = new StringTokenizer(string);
                StringBuilder sb = new StringBuilder();
		EnglishStemmer stemmer = new EnglishStemmer();
                while (st.hasMoreTokens()) {
                        String token = st.nextToken();
			stemmer.setCurrent(token);
			stemmer.stem();
                        sb.append(stemmer.getCurrent());
                        sb.append(" ");
                }
                return sb.toString();
	}

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
			searchIndex(qid, normQuery);
		}
	}

	public static float getMinDistance(IndexSearcher searcher, QueryParser queryParser, int doc, 
					String field, String w1, String w2) throws Exception {
		for (int distance = 1; distance <= 10; distance++) {
			Query proximityQuery = queryParser.parse(field + ":\"" + w1 + " " + w2 + "\"~" + distance);
			if (searcher.explain(proximityQuery, doc).isMatch())
				return (float)(1.0 / distance / distance);
		}
		return 0;
	}

	public static float getScore(IndexSearcher searcher, Query query, int doc) throws Exception {
		Explanation explanation = searcher.explain(query, doc);
		if (explanation.isMatch()) 
			return explanation.getValue().floatValue();
		else	return 0;
	}

	public static void searchIndex(String qid, String searchString) throws Exception {
		StringBuilder query = new StringBuilder();
		StringTokenizer stQuery = new StringTokenizer(searchString);
		Vector<String> queryTerms = new Vector<String>();
		while (stQuery.hasMoreTokens()) {
			queryTerms.add(stemming(stQuery.nextToken()));
		}
		int queryLength = queryTerms.size();
		boolean start = true;
		for (String term : queryTerms) {
			if (start) start = false;
                        else {
				query.append("  "); // same as OR
			}
			query.append(term);
			//query.append("~2"); // fuzzy search w/ at most 2 edit distance
                }

		//System.out.println("Searching for '" + searchString + "'");
		String bothQuery = FIELD_TITLE + ":(" + query.toString()
					 + ") OR " + FIELD_BODY + ":(" + query.toString() + ")";
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(indexReader);
		searcher.setSimilarity(new BM25Similarity());

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new QueryParser(FIELD_DOCID, analyzer);
		Query queryAll = queryParser.parse(bothQuery);
		Query queryTitleOnly = queryParser.parse(FIELD_TITLE + ":(" + query.toString() + ")");
		Query queryBodyOnly = queryParser.parse(FIELD_BODY + ":(" + query.toString() + ")");
		TopDocs topDocs = searcher.search(queryAll, TOP);
		ScoreDoc[] hits = topDocs.scoreDocs;
		//System.out.println("Number of hits: " + topDocs.totalHits);

		for (int i=0; i<Math.min(TOP, topDocs.totalHits); i++) {
			Vector<Float> titleTfidfRaw = new Vector<Float>();
                	Vector<Float> titleBM25Raw = new Vector<Float>();
                	Vector<Float> bodyTfidfRaw = new Vector<Float>();
                	Vector<Float> bodyBM25Raw = new Vector<Float>();
        	        Vector<Float> titleProxRaw = new Vector<Float>();
	                Vector<Float> bodyProxRaw = new Vector<Float>();
			Document document = searcher.doc(hits[i].doc);
			String docid = document.getField("docid").stringValue();
			searcher.setSimilarity(new BM25Similarity());
			float bm25Title = getScore(searcher, queryTitleOnly, hits[i].doc);
			searcher.setSimilarity(new ClassicSimilarity());
			float tfidfTitle = getScore(searcher, queryTitleOnly, hits[i].doc);
			searcher.setSimilarity(new BM25Similarity());
                        float bm25Body = getScore(searcher, queryBodyOnly, hits[i].doc);
                        searcher.setSimilarity(new ClassicSimilarity());
                        float tfidfBody = getScore(searcher, queryBodyOnly, hits[i].doc);
			searcher.setSimilarity(new BM25Similarity());
                        float bm25 = getScore(searcher, queryAll, hits[i].doc);
                        searcher.setSimilarity(new ClassicSimilarity());
                        float tfidf = getScore(searcher, queryAll, hits[i].doc);

			float minTfidfTitle = Float.MAX_VALUE;
			float maxTfidfTitle = Float.MIN_VALUE;
			float minBM25Title = Float.MAX_VALUE;
			float maxBM25Title = Float.MIN_VALUE;
			float avgTfidfTitle = 0;
			float avgBM25Title = 0;
                	start = true;
                	for (String term : queryTerms) {
                        	Query subQuery = queryParser.parse("title:" + term);
				searcher.setSimilarity(new ClassicSimilarity());
				float subTfidf = getScore(searcher, subQuery, hits[i].doc);
				searcher.setSimilarity(new BM25Similarity());
				float subBM25  = getScore(searcher, subQuery, hits[i].doc);
				if (subTfidf < minTfidfTitle) minTfidfTitle = subTfidf;
				if (subTfidf > maxTfidfTitle) maxTfidfTitle = subTfidf;
				if (subBM25 < minBM25Title)   minBM25Title  = subBM25;
				if (subBM25 > maxBM25Title)   maxBM25Title  = subBM25;
				avgTfidfTitle += subTfidf;
				avgBM25Title += subBM25;
				titleTfidfRaw.add(subTfidf);
				titleBM25Raw.add(subBM25);
                	}
			avgTfidfTitle /= queryLength;
			avgBM25Title /= queryLength;

			float minTfidfBody = Float.MAX_VALUE;
                        float maxTfidfBody = Float.MIN_VALUE;
                        float minBM25Body = Float.MAX_VALUE;
                        float maxBM25Body = Float.MIN_VALUE;
                        float avgTfidfBody = 0;
                        float avgBM25Body = 0;
                        start = true;
                        for (String term : queryTerms) {
                                Query subQuery = queryParser.parse("body:" + term);
                                searcher.setSimilarity(new ClassicSimilarity());
                                float subTfidf = getScore(searcher, subQuery, hits[i].doc);
                                searcher.setSimilarity(new BM25Similarity());
                                float subBM25  = getScore(searcher, subQuery, hits[i].doc);
                                if (subTfidf < minTfidfBody) minTfidfBody = subTfidf;
                                if (subTfidf > maxTfidfBody) maxTfidfBody = subTfidf;
                                if (subBM25 < minBM25Body)   minBM25Body  = subBM25;
                                if (subBM25 > maxBM25Body)   maxBM25Body  = subBM25;
                                avgTfidfBody += subTfidf;
                                avgBM25Body += subBM25;
				bodyTfidfRaw.add(subTfidf);
				bodyBM25Raw.add(subBM25);
                        }
                        avgTfidfBody /= queryLength;
                        avgBM25Body /= queryLength;

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
					titleProxRaw.add(invMinDistance);
					invMinDistance =
                                                getMinDistance(searcher, queryParser, hits[i].doc, 
								"body", queryTerms.get(k), queryTerms.get(j));
                                        if (invMinDistance > maxInvMinDistanceBody)
                                                maxInvMinDistanceBody = invMinDistance;
                                        if (invMinDistance < minInvMinDistanceBody)
                                                minInvMinDistanceBody = invMinDistance;
                                        avgInvMinDistanceBody += invMinDistance;
					bodyProxRaw.add(invMinDistance);
				}
			}
			if (queryLength > 1) {
				avgInvMinDistanceTitle /= queryLength * (queryLength-1) / 2;
				avgInvMinDistanceBody  /= queryLength * (queryLength-1) / 2;
			}
			else {
				maxInvMinDistanceTitle = minInvMinDistanceTitle = avgInvMinDistanceTitle = 0;
				maxInvMinDistanceBody = minInvMinDistanceBody = avgInvMinDistanceBody = 0;
			}
			String line = qid + " " + docid 
					+ " 1:" + String.format("%.4f", maxTfidfTitle)
					+ " 2:" + String.format("%.4f", minTfidfTitle)
					+ " 3:" + String.format("%.4f", avgTfidfTitle)
					+ " 4:" + String.format("%.4f", tfidfTitle)
					+ " 5:" + String.format("%.4f", maxTfidfBody)
					+ " 6:" + String.format("%.4f", minTfidfBody)
					+ " 7:" + String.format("%.4f", avgTfidfBody)
					+ " 8:" + String.format("%.4f", tfidfBody)  
					+ " 9:" + String.format("%.4f", tfidf)      
					+ " 10:" + String.format("%.4f", maxBM25Title)
                                        + " 11:" + String.format("%.4f", minBM25Title)
                                        + " 12:" + String.format("%.4f", avgBM25Title)
					+ " 13:" + String.format("%.4f", bm25Title)
					+ " 14:" + String.format("%.4f", maxBM25Body)
                                        + " 15:" + String.format("%.4f", minBM25Body)
                                        + " 16:" + String.format("%.4f", avgBM25Body)
					+ " 17:" + String.format("%.4f", bm25Body)
					+ " 18:" + String.format("%.4f", bm25)
					+ " 19:" + String.format("%.4f", maxInvMinDistanceTitle)
					+ " 20:" + String.format("%.4f", minInvMinDistanceTitle)
					+ " 21:" + String.format("%.4f", avgInvMinDistanceTitle)
					+ " 22:" + String.format("%.4f", maxInvMinDistanceBody)
					+ " 23:" + String.format("%.4f", minInvMinDistanceBody)
					+ " 24:" + String.format("%.4f", avgInvMinDistanceBody);
			int fno = 25;
			for(int k=0; k<titleTfidfRaw.size(); k++) {
				line += " "+Integer.toString(fno)+":"+ String.format("%.4f", titleTfidfRaw.get(k));
				fno++;
			}
			for(int k=0; k<bodyTfidfRaw.size(); k++) {
                                line += " "+Integer.toString(fno)+":"+ String.format("%.4f", bodyTfidfRaw.get(k));
                                fno++;
                        }
			for(int k=0; k<titleBM25Raw.size(); k++) {
                                line += " "+Integer.toString(fno)+":"+ String.format("%.4f", titleBM25Raw.get(k));
                                fno++;
                        }
			for(int k=0; k<bodyBM25Raw.size(); k++) {
                                line += " "+Integer.toString(fno)+":"+ String.format("%.4f", bodyBM25Raw.get(k));
                                fno++;
                        }
			for(int k=0; k<titleProxRaw.size(); k++) {
                                line += " "+Integer.toString(fno)+":"+ String.format("%.4f", titleProxRaw.get(k));
                                fno++;
                        }
			for(int k=0; k<bodyProxRaw.size(); k++) {
                                line += " "+Integer.toString(fno)+":"+ String.format("%.4f", bodyProxRaw.get(k));
                                fno++;
                        }
			System.out.println(line);
		}
	}

}

