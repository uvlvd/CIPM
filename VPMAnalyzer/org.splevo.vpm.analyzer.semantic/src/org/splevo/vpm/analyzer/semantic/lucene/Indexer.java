package org.splevo.vpm.analyzer.semantic.lucene;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.splevo.vpm.analyzer.semantic.Constants;
import org.splevo.vpm.variability.VariationPoint;

/**
 * This class handles the indexing. It creates one single main index. The class provides
 * methods to add content to the index using customizable {@link Analyzer}s. Via a {@link DirectoryReader}
 * the {@link Indexer} class provides access to the index.
 * 
 * @author Daniel Kojic
 * 
 */
public class Indexer {
	
	/** The logger for this class. */
    private Logger logger = Logger.getLogger(Indexer.class);
	
	/** The {@link Analyzer} code gets indexed with. */
	private Analyzer contentAnalyzer;
	
	/** The {@link Analyzer} comments get indexed with. */
	private Analyzer commentAnalyzer;
	
	/** The {@link Directory} for the index. */
	private Directory directory;
	
	/** The {@link IndexWriterConfig} for code. */
	private IndexWriterConfig contentConfig;

	/** The {@link IndexWriterConfig} for comments. */
	private IndexWriterConfig commentConfig;
	
	/** Singleton instance. */
	private static Indexer instance;
	
    /** Indexed, tokenized, stored. */
    private static final FieldType TYPE_STORED = new FieldType();

    static {
    	// Setup of fields beeing indexed. Store TermVectors for later analysis.
        TYPE_STORED.setIndexed(true);
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        //TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
    }
	
	/**
	 *  Private constructor to prevent this	class from being
	 *  instantiated multiple times.
	 *  The default analyzer will be the {@link WhitespaceAnalyzer}.
	 * @throws IOException Throws an {@link IOException} if there are problems opening the index.
	 */
	private Indexer() {
		// Set default values.
		contentAnalyzer = new WhitespaceAnalyzer(Version.LUCENE_42);
		commentAnalyzer = new StandardAnalyzer(Version.LUCENE_42);
		directory = new RAMDirectory();
		contentConfig = new IndexWriterConfig(Version.LUCENE_42, contentAnalyzer);
		commentConfig = new IndexWriterConfig(Version.LUCENE_42, commentAnalyzer);
	}
	
	/**
	 * Gets the singleton instance.
	 * @return The singleton instance.
	 */
	public static Indexer getInstance() {
		// Return singleton, create new if not existing.
		return instance == null ? instance = new Indexer() : instance;
	}
	
	/**
	 * @return A {@link DirectoryReader} to search the main index.
	 * @throws IOException Throws an {@link IOException} if there are problems opening the index.
	 */
	public DirectoryReader getIndexReader() throws IOException {
		return DirectoryReader.open(directory);
	}
	
	/**
	 * This method adds the given code to the index.
	 * 
	 * @param variationPointId The contents id.
	 * @param content The content.
	 */
	public void addCodeToIndex(String variationPointId, String content){
		addToIndex(variationPointId, content, contentConfig);
	}

	/**
	 * This method adds the given comments to the index.
	 * 
	 * @param variationPointId The comments id.
	 * @param content The comments.
	 */
	public void addCommentToIndex(String variationPointId, String comment){
		addToIndex(variationPointId, comment, commentConfig);
	}
	
	/**
	 * Adds content to the main index.
	 * 
	 * @param variationPointId The ID of the {@link VariationPoint} to be linked with its content.
	 * @param content The text content of the {@link VariationPoint}.
	 * @param indexConfiguration The {@link IndexWriterConfig} the {@link IndexWriter} gets initialized with. 
	 */
	private void addToIndex(String variationPointId, String content, IndexWriterConfig indexConfiguration){
		if(variationPointId == null || content == null || indexConfiguration == null){
			throw new IllegalArgumentException();
		}
		
		if(content.length() == 0 || variationPointId.length() == 0){
			logger.error("Invalid content or id. Empty String not allowed.");
		}
		
		IndexWriter indexWriter = null;
		
		try {
			indexWriter = new IndexWriter(directory, indexConfiguration);
			Document doc = new Document();
			doc.add(new Field(Constants.INDEX_VARIATIONPOINT, variationPointId, TYPE_STORED));
			doc.add(new Field(Constants.INDEX_CONTENT, content, TYPE_STORED));
			indexWriter.addDocument(doc);
			indexWriter.close();
		} catch (IOException e) {
			logger.error("Error while adding data to Index.");
		}
	}
	
	/**
	 * Deletes all contents from the main index.
	 * @throws IOException 
	 */
	public void clearIndex() throws IOException{
		IndexWriter indexWriter = new IndexWriter(directory, contentConfig);
		indexWriter.deleteAll();
		indexWriter.close();
	}
}
