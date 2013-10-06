package org.splevo.vpm.analyzer.semantic;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.graphstream.graph.Node;
import org.splevo.vpm.analyzer.AbstractVPMAnalyzer;
import org.splevo.vpm.analyzer.VPMAnalyzerResult;
import org.splevo.vpm.analyzer.VPMEdgeDescriptor;
import org.splevo.vpm.analyzer.config.BooleanConfiguration;
import org.splevo.vpm.analyzer.config.NumericConfiguration;
import org.splevo.vpm.analyzer.config.StringConfiguration;
import org.splevo.vpm.analyzer.config.VPMAnalyzerConfigurations;
import org.splevo.vpm.analyzer.graph.VPMGraph;
import org.splevo.vpm.analyzer.semantic.lucene.Indexer;
import org.splevo.vpm.analyzer.semantic.lucene.RelationShipSearchConfiguration;
import org.splevo.vpm.analyzer.semantic.lucene.Searcher;
import org.splevo.vpm.software.SoftwareElement;
import org.splevo.vpm.variability.VariationPoint;

/**
 * <h1>What it does</h1>
 * The semantic relationship VPMAnalazer analyzer is able to find semantic
 * relationships between several {@link VariationPoint}s. Several configurations
 * allow a customized search, just as needed.
 * 
 * <h1>How does that work?</h1>
 * As a first step, the analyzer extracts all relevant content from a VPMGraph
 * and stores that within a Lucene index. Through storing additional
 * informations about the indexed text, Lucene provides the ability to extract
 * vectors from given index content. The Analyzer uses several Finders to search
 * for semantic dependencies. Those results can be displayed within the VPMGraph
 * or the Refinement Browser.
 * 
 * 
 * @author Daniel Kojic
 * 
 */
public class SemanticVPMAnalyzer extends AbstractVPMAnalyzer {

	/** The relationship label of the analyzer. */
	private static final String RELATIONSHIP_LABEL_SEMANTIC = "Semantic";

	/** The displayed name of the analyzer. */
	private static final String DISPLAYED_NAME = "Semantic VPM Analyzer";

	/** The logger for this class. */
	private Logger logger = Logger.getLogger(SemanticVPMAnalyzer.class);

	/** The indexer instance. */
	private Indexer indexer;

	/** The configuration-object for the include comments configuration. */
	private BooleanConfiguration includeCommentsConfig;

	/** The configuration-object for the split on case change configuration. */
	private BooleanConfiguration splitCamelCaseConfig;

	/** The configuration-object for the stop words configuration. */
	private StringConfiguration stopWordsConfig;

	/** The configuration-object for the use overall sim finder configuration. */
	private BooleanConfiguration useOverallSimFinderConfig;

	/** The configuration-object for the use important term finder configuration. */
	private BooleanConfiguration useimportantTermFinderConfig;

	/** The configuration-object for the use top n finder configuration. */
	private BooleanConfiguration useTopNFinderConfig;

	/** The configuration-object for the minimum similarity configuration. */
	private NumericConfiguration minSimConfig;

	/** The configuration-object for the least document frequency configuration. */
	private NumericConfiguration topNLeastDocFreqConfig;

	/** The configuration-object for the N configuration. */
	private NumericConfiguration topNNConfig;

	/**
	 * The default constructor for this class.
	 */
	public SemanticVPMAnalyzer() {
		indexer = Indexer.getInstance();

		includeCommentsConfig = new BooleanConfiguration(
				ConfigDefaults.LABEL_INCLUDE_COMMENTS, null,
				ConfigDefaults.DEFAULT_INCLUDE_COMMENTS);
		splitCamelCaseConfig = new BooleanConfiguration(
				ConfigDefaults.LABEL_SPLIT_CAMEL_CASE, null,
				ConfigDefaults.DEFAULT_SPLIT_CAMEL_CASE);
		stopWordsConfig = new StringConfiguration(
				ConfigDefaults.LABEL_STOP_WORDS,
				ConfigDefaults.EXPL_STOP_WORDS,
				ConfigDefaults.DEFAULT_STOP_WORDS);

		useOverallSimFinderConfig = new BooleanConfiguration(
				ConfigDefaults.LABEL_USE_OVERALL_SIMILARITY_FINDER, null,
				ConfigDefaults.DEFAULT_USE_OVERALL_SIMILARITY_FINDER);
		useimportantTermFinderConfig = new BooleanConfiguration(
				ConfigDefaults.LABEL_USE_IMPORTANT_TERM_FINDER, null,
				ConfigDefaults.DEFAULT_USE_IMPORTANT_TERM_FINDER);
		useTopNFinderConfig = new BooleanConfiguration(
				ConfigDefaults.LABEL_USE_TOP_N_TERM_FINDER, null,
				ConfigDefaults.DEFAULT_USE_TOP_N_TERM_FINDER);

		minSimConfig = new NumericConfiguration(
				ConfigDefaults.LABEL_MIN_SIMILARITY,
				ConfigDefaults.EXPL_MIN_SIMILARITY,
				ConfigDefaults.DEFAULT_MIN_SIMILARITY, 0.01d, 0.d, 1.d, 2);

		topNLeastDocFreqConfig = new NumericConfiguration(
				ConfigDefaults.LABEL_LEAST_DOC_FREQ,
				ConfigDefaults.EXPL_LEAST_DOC_FREQ,
				ConfigDefaults.DEFAULT_LEAST_DOC_FREQ, 0.01d, 0.d, 1.d, 2);
		topNNConfig = new NumericConfiguration(ConfigDefaults.LABEL_N,
				ConfigDefaults.EXPL_N, ConfigDefaults.DEFAULT_N, 1.d, 1.d,
				100.d, 0);
	}

	@Override
	public VPMAnalyzerResult analyze(VPMGraph vpmGraph) {
		if (vpmGraph == null) {
			throw new IllegalArgumentException();
		}

		VPMAnalyzerResult result = null;

		// Fill the index.
		try {
			logger.info("Filling index...");
			fillIndex(vpmGraph);
			logger.info("Indexing done.");
		} catch (Exception e) {
			logger.error(
					"Cannot write Index. Close all open IndexWriters first.", e);
		}

		// Find relationships.
		try {
			logger.info("Analyzing...");
			VPLinkContainer similars = findRelationships(vpmGraph);
			result = addSimilarsToAnalyzerResultSet(vpmGraph, similars);
			logger.info("Analysis done.");
		} catch (IOException e) {
			logger.error(
					"Cannot read Index. Close all open IndexWriters first.", e);
		}

		// CLEAN-UP.
		try {
			Indexer.getInstance().clearIndex();
			logger.info("Clean-Up done.");
		} catch (IOException e) {
			logger.error("Failure while trying to empty main index.", e);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.splevo.vpm.analyzer.VPMAnalyzer#getConfigurations()
	 */
	@Override
	public VPMAnalyzerConfigurations getConfigurations() {
		VPMAnalyzerConfigurations configurations = new VPMAnalyzerConfigurations();
		configurations.addConfigurations("General Configuations",
				includeCommentsConfig, splitCamelCaseConfig, stopWordsConfig);
		configurations.addConfigurations("Overall Similarity Search",
				useOverallSimFinderConfig, minSimConfig);
		configurations.addConfigurations("Important Term Search",
				useimportantTermFinderConfig);
		configurations.addConfigurations("Top N Term Search",
				useTopNFinderConfig, topNLeastDocFreqConfig, topNNConfig);

		return configurations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.splevo.vpm.analyzer.VPMAnalyzer#getName()
	 */
	@Override
	public String getName() {
		return DISPLAYED_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.splevo.vpm.analyzer.VPMAnalyzer#getRelationshipLabel()
	 */
	@Override
	public String getRelationshipLabel() {
		return RELATIONSHIP_LABEL_SEMANTIC;
	}

	/**
	 * Writes all necessary data from the {@link VPMGraph} into the Index.
	 * 
	 * @param vpmGraph
	 *            The {@link VPMGraph} containing the information to be indexed.
	 */
	private void fillIndex(VPMGraph vpmGraph) {
		if (vpmGraph == null) {
			throw new IllegalArgumentException();
		}

		// Get the user-configurations.
		boolean indexComments = includeCommentsConfig.getCurrentValue();
		boolean splitCamelCase = splitCamelCaseConfig.getCurrentValue();
		String stopWords = stopWordsConfig.getCurrentValue();

		this.indexer.splitCamelCase(splitCamelCase);

		if (stopWords != null) {
			if (stopWords.length() > 0) {
				this.indexer.setStopWords(stopWords.split(" "));
			} else {
				this.indexer.setStopWords(new String[0]);
			}
		}

		// Iterate through the graph.
		for (Node node : vpmGraph.getNodeSet()) {
			VariationPoint vp = node.getAttribute(VPMGraph.VARIATIONPOINT,
					VariationPoint.class);
			indexNode(node.getId(), node, vp, indexComments);
		}
	}

	/**
	 * This method uses the IndexASTNodeSwitch to extract the text from the
	 * given Node. It iterates through all child elements.
	 * 
	 * @param id
	 *            The ID used to store the text in the Lucene index.
	 * @param node
	 *            The corresponding {@link Node}.
	 * @param vp
	 *            The corresponding {@link VariationPoint}.
	 * @param indexComments
	 *            Determines if comments should be indexed or ignored.
	 */
	private void indexNode(String id, Node node, VariationPoint vp,
			boolean indexComments) {
		if (id == null || node == null || vp == null) {
			throw new IllegalArgumentException();
		}

		SoftwareElement astNode = vp.getEnclosingSoftwareEntity();
		if (astNode == null) {
			throw new IllegalStateException();
		}

		// Iterate through all child elements.
		TreeIterator<EObject> allContents = EcoreUtil.getAllContents(astNode
				.eContents());
		IndexASTNodeSwitch indexASTNodeSwitch = new IndexASTNodeSwitch(
				indexComments);
		while (allContents.hasNext()) {
			EObject next = allContents.next();
			indexASTNodeSwitch.doSwitch(next);
		}

		// Get content and comment from switch.
		String content = indexASTNodeSwitch.getContent();
		String comment = indexASTNodeSwitch.getComments();

		// Add to index.
		try {
			this.indexer.addToIndex(id, content, comment);
		} catch (IOException e) {
			logger.error("Failure while adding node to index.", e);
		}

	}

	/**
	 * Finds semantic relationships between the variation points.
	 * 
	 * @param graph
	 *            The {@link VPMGraph} to extract the IDs of the result nodes
	 *            from.
	 * @return A {@link VPLinkContainer} containing the search results.
	 * @throws IOException
	 *             Throws an {@link IOException} when there is already an open
	 *             {@link IndexWriter}.
	 */
	private VPLinkContainer findRelationships(VPMGraph graph)
			throws IOException {
		if (graph == null) {
			throw new IllegalArgumentException();
		}

		// Get the configurations
		boolean includeComments = includeCommentsConfig.getCurrentValue();
		boolean useRareFinder = useimportantTermFinderConfig.getCurrentValue();
		boolean useOverallSimFinder = useOverallSimFinderConfig
				.getCurrentValue();
		boolean useTopNTermFinder = useTopNFinderConfig.getCurrentValue();
		Double minSimilarity = minSimConfig.getCurrentValue();
		Double leastDocFreq = topNLeastDocFreqConfig.getCurrentValue();
		Integer n = topNNConfig.getIntegerValue();

		// Setup the configuration object.
		RelationShipSearchConfiguration searchConfig = new RelationShipSearchConfiguration();
		searchConfig.useComments(includeComments);
		searchConfig.configureOverallFinder(useOverallSimFinder, minSimilarity);
		searchConfig.configureImportantTermFinder(useRareFinder);
		searchConfig.configureTopNFinder(useTopNTermFinder, leastDocFreq, n);

		// Use the searcher to search for semantic relationships.
		return Searcher.findSemanticRelationships(searchConfig);
	}

	/**
	 * Transforms the links from the {@link VPLinkContainer} to
	 * {@link VPMAnalyzerResult}.
	 * 
	 * @param graph
	 *            The related graph.
	 * @param similars
	 *            The search results.
	 * @return A {@link VPMAnalyzerResult} containing the edge descriptors.
	 */
	private VPMAnalyzerResult addSimilarsToAnalyzerResultSet(VPMGraph graph,
			VPLinkContainer similars) {
		VPMAnalyzerResult result = new VPMAnalyzerResult(this);
		for (String key : similars.getAllLinks().keySet()) {
			Set<String> values = similars.getAllLinks().get(key);

			for (String value : values) {
				Node sourceNode = graph.getNode(key);
				Node targetNode = graph.getNode(value);
				String[] explanations = similars.getExplanations(key, value);

				if (explanations == null) {
					explanations = new String[] { RELATIONSHIP_LABEL_SEMANTIC };
				}

				for (String explanation : explanations) {
					logAnalysisInfo(sourceNode.getId(), targetNode.getId(), "",
							"", explanation);
				}

				VPMEdgeDescriptor descriptor = buildEdgeDescriptor(sourceNode,
						targetNode, Arrays.deepToString(explanations));
				if (descriptor != null) {
					result.getEdgeDescriptors().add(descriptor);
				}
			}
		}
		return result;
	}
}
