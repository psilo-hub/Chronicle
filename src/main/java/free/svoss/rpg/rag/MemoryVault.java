package free.svoss.rpg.rag;

import free.svoss.rpg.llm.OllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MemoryVault {
    private final Directory indexDirectory;
    private final IndexWriter writer;
    private final String VECTOR_FIELD = "vector";
    private final String CONTENT_FIELD = "content";
    // nomic-embed-text uses 768 dimensions
    private final int DIMENSIONS = 768;

    public MemoryVault(String path) throws IOException {
        // Use FSDirectory.open(Paths.get(path)) for persistent storage
        // Or new ByteBuffersDirectory() for in-memory (reset on restart)
        this.indexDirectory = FSDirectory.open(Paths.get(path));

        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        this.writer = new IndexWriter(indexDirectory, config);
    }

    /**
     * Stores a new memory chunk (User + AI exchange).
     */
    public void addMemory(String text, float[] vector) throws IOException {
        log.info("Adding memory :\n{}", text);
        Document doc = new Document();
        // The text content is stored so we can read it back
        doc.add(new StoredField(CONTENT_FIELD, text));
        // The vector is indexed for KNN search
        doc.add(new KnnFloatVectorField(VECTOR_FIELD, vector, VectorSimilarityFunction.COSINE));

        writer.addDocument(doc);
        writer.commit();
    }

    /**
     * Retrieves the top K most relevant memories based on a query vector.
     */
    public List<String> findRelevantMemories(float[] queryVector, int topK) throws IOException {
        List<String> memories = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // Perform the Vector Search (Approximate Nearest Neighbor)
            KnnFloatVectorQuery query = new KnnFloatVectorQuery(VECTOR_FIELD, queryVector, topK);
            TopDocs results = searcher.search(query, topK);

            for (ScoreDoc hit : results.scoreDocs) {
                Document doc = searcher.doc(hit.doc);
                memories.add(doc.get(CONTENT_FIELD));
            }
        } catch (IndexNotFoundException e) {
            // Return empty if no memories exist yet
        }

        log.info("Found "+memories.size()+" relevant memories");
        return memories;
    }

    public void close() throws IOException {
        writer.close();
        indexDirectory.close();
    }
}