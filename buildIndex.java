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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class buildIndex {

	public static final String FILE_TO_INDEX_DIRECTORY = "lines-trec45.txt";
	public static final String INDEX_DIRECTORY = "./trec45-index";

	public static final String FIELD_DOCID = "docid";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_BODY  = "body";

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

	public static void main(String[] args) throws Exception {

		createIndex();
	}

	public static void createIndex() throws CorruptIndexException, LockObtainFailedException, IOException, Exception {
		CharArraySet emptyStopwords = new CharArraySet(0, true);
		Analyzer analyzer = new StandardAnalyzer(emptyStopwords);
		Directory dir = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter indexWriter = new IndexWriter(dir, iwc);
		try (BufferedReader br = new BufferedReader(new FileReader(FILE_TO_INDEX_DIRECTORY))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] fields = line.split("\t");
				if (fields.length < 3) continue;
				Document document = new Document();
				document.add(new StringField(FIELD_DOCID, fields[0], 
						Field.Store.YES));
				document.add(new TextField(FIELD_TITLE, 
						new BufferedReader(new StringReader(stemming(fields[1])))));
				document.add(new TextField(FIELD_BODY, 
						new BufferedReader(new StringReader(stemming(fields[2])))));
				indexWriter.addDocument(document);
			}
		}
		indexWriter.close();
	}

}

