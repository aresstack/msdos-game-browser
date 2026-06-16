package de.bund.zrb.msdosgames.backend;

import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameIdentifier;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class LuceneGameDetailsIndex {

    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final RAMDirectory directory = new RAMDirectory();
    private final IndexWriter writer;

    LuceneGameDetailsIndex() {
        try {
            writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create Lucene index", exception);
        }
    }

    synchronized void index(GameDetails details) throws IOException {
        Document document = new Document();
        document.add(new StringField("identifier", details.getIdentifier().getValue(), Field.Store.YES));
        document.add(new TextField("title", details.getTitle(), Field.Store.NO));
        document.add(new TextField("description", details.getDescriptionText(), Field.Store.NO));
        document.add(new TextField("rights", details.getLicenseNotice().getRights(), Field.Store.NO));
        document.add(new StringField("pageUrl", details.getLicenseNotice().getSourceUrl(), Field.Store.NO));
        writer.updateDocument(new Term("identifier", details.getIdentifier().getValue()), document);
        writer.commit();
    }

    synchronized boolean contains(GameIdentifier identifier) throws IOException {
        writer.commit();
        DirectoryReader reader = DirectoryReader.open(directory);
        try {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(new TermQuery(new Term("identifier", identifier.getValue())), 1);
            return topDocs.totalHits.value > 0;
        } finally {
            reader.close();
        }
    }

    synchronized List<GameIdentifier> findByIdentifierPrefix(String text, int limit) throws IOException {
        writer.commit();
        DirectoryReader reader = DirectoryReader.open(directory);
        try {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new TermQuery(new Term("identifier", text));
            TopDocs topDocs = searcher.search(query, Math.max(1, limit));
            List<GameIdentifier> identifiers = new ArrayList<GameIdentifier>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document document = searcher.doc(scoreDoc.doc);
                identifiers.add(GameIdentifier.of(document.get("identifier")));
            }
            return identifiers;
        } finally {
            reader.close();
        }
    }

    synchronized void close() {
        try {
            writer.close();
        } catch (IOException ignored) {
        }
        directory.close();
    }
}
