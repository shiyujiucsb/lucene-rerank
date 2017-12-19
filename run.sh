export CLASSPATH=.:./lucene-solr/lucene/build/core/classes/java/:./lucene-solr/lucene/build/queryparser/classes/java/

javac buildIndex.java searchIndex.java

java searchIndex

