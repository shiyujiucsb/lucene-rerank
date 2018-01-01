export CLASSPATH=.:./lucene-solr/lucene/build/core/classes/java/:./lucene-solr/lucene/build/queryparser/classes/java/:./lucene-solr/lucene/build/analysis/common/classes/java/

javac buildIndex.java searchIndex.java

java searchIndex

