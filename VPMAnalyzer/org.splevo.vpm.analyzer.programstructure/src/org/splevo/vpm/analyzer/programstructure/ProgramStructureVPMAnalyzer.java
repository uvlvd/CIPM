package org.splevo.vpm.analyzer.programstructure;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.gmt.modisco.java.ASTNode;
import org.graphstream.graph.Node;
import org.splevo.vpm.analyzer.AbstractVPMAnalyzer;
import org.splevo.vpm.analyzer.VPMAnalyzerResult;
import org.splevo.vpm.analyzer.VPMEdgeDescriptor;
import org.splevo.vpm.analyzer.config.BooleanConfiguration;
import org.splevo.vpm.analyzer.config.VPMAnalyzerConfigurations;
import org.splevo.vpm.analyzer.graph.VPMGraph;
import org.splevo.vpm.analyzer.programstructure.index.VariationPointIndex;
import org.splevo.vpm.variability.VariationPoint;

/**
 * Program Dependency Analyzer analyzing the dependency graph and the included variation points,
 * respectively the ASTnodes referenced by the Variation Points.
 * 
 * The analyzer uses different strategies to find dependent elements.
 * 
 * First an inverse variation point index is prepared to lookup the node for a variation point. This
 * allows to work with the variation points and if a matching pair is found, the nodes can be
 * retrieved to create an edge.
 * 
 * <strong>VariableDeclaration Forward Slice</strong>
 * <ul>
 * <li>
 * detect all variable declaration statements (VDS) in the variation points<br>
 * All ASTNodes referenced by all variants of a variation point filtered for
 * VariableDeclarationStatement type</li>
 * <li>
 * iterate over all VDS and for each detect all relevant ASTNodes influenced by the declared
 * variable(s).<br>
 * The detection traverse the ASTNode meta model. Currently only VariableStatements are returned.
 * Logger warnings are created if a currently not respected direction within the AST traversing is
 * identified. TODO: Check the completeness of the influenced AST node detection.</li>
 * <li>Lookup the variation points for the influenced ASTNodes.</li>
 * <li>Lookup the Nodes for the influenced variation points.</li>
 * <li>Create edges between all nodes influenced by the same variable declaration</li>
 * </ul>
 * 
 * 
 * 
 * @author Benjamin Klatt
 * 
 */
public class ProgramStructureVPMAnalyzer extends AbstractVPMAnalyzer {

    private static final boolean DEFAULT_CONFIG_FULL_REFERRED_TREE = true;

	/** Label of the configuration to consider the full sub AST for the referred VP. */
    private static final String CONFIG_LABEL_FULL_REFERRED_TREE = "Check Full Referred AST";

    /** The relationship label of the analyzer. */
    public static final String RELATIONSHIP_LABEL_PROGRAM_STRUCTURE = "ProgramStructure";

    /** The logger for this class. */
    private Logger logger = Logger.getLogger(ProgramStructureVPMAnalyzer.class);

     /** The index of variation points and AST nodes. **/
    private VariationPointIndex variationPointIndex = null;

    /** Switch to get the referrer for an ast node. */
    private ReferringASTNodeSwitch referringASTNodeSwitch = new ReferringASTNodeSwitch();

    /**
     * Constructor initializing the variation point analyzer.
     */
    public ProgramStructureVPMAnalyzer() {
        variationPointIndex = new VariationPointIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.splevo.vpm.analyzer.VPMAnalyzer#analyze(org.splevo.vpm.analyzer.graph.VPMGraph)
     */
    @Override
    public VPMAnalyzerResult analyze(final VPMGraph vpmGraph) {
        logger.info("index variation point nodes");
        try {
            variationPointIndex.index(vpmGraph);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        logger.info("start analysis");
        return analyzeVPs();
    }

    /**
     * Analyzes variation points for pairs with a VariableDeclarationStatement that declares a
     * variable that is used by another node later on.
     * 
     * This has to work on the variant level to really analyze the variable declaration statement
     * and not the enclosing block.
     * 
     * TODO: Enhance dependency handling. Currently, a very simplified handling of all software
     * entities referenced by a variant and all variants in a variation point is used.
     * 
     * @return The result of the analysis.
     */
    private VPMAnalyzerResult analyzeVPs() {
        VPMAnalyzerResult result = new VPMAnalyzerResult(this);

        for (VariationPoint vp : variationPointIndex.getVariationPoints()) {

            List<ASTNode> astNodes = variationPointIndex.getVariantSoftwareEntities(vp);
            for (ASTNode astNode : astNodes) {
                List<VariationPoint> relatedVPs = findRelatedVariationPoints(vp, astNode);
                for (VariationPoint referee : relatedVPs) {
                    Node sourceNode = variationPointIndex.getGraphNode(vp);
                    Node targetNode = variationPointIndex.getGraphNode(referee);

                    VPMEdgeDescriptor descriptor = buildEdgeDescriptor(sourceNode, targetNode, astNode.getClass()
                            .getSimpleName());
                    if (descriptor != null) {
                        result.getEdgeDescriptors().add(descriptor);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Search for variation points referencing AST nodes influenced by a variable declaration
     * statement.
     * 
     * @param referredVP
     *            The variation point to find relationships for.
     * @param referredAstNode
     *            The AST node to find references for.
     * @return The list of referring variation points.
     */
    private List<VariationPoint> findRelatedVariationPoints(VariationPoint referredVP, ASTNode referredAstNode) {

        List<VariationPoint> variationPoints = new ArrayList<VariationPoint>();

        List<ASTNode> referringASTNodes = referringASTNodeSwitch.doSwitch(referredAstNode);

        for (ASTNode referringASTNode : referringASTNodes) {
            VariationPoint relatedVariationPoint = variationPointIndex.getEnclosingVariationPoint(referringASTNode);
            if (relatedVariationPoint != null) {
                variationPoints.add(relatedVariationPoint);

                String vp1ID = variationPointIndex.getGraphNode(referredVP).getId();
                String vp2ID = variationPointIndex.getGraphNode(relatedVariationPoint).getId();
                String sourceInfo = referredAstNode.getClass().getSimpleName();
                if (referredAstNode.getOriginalCompilationUnit() != null) {
                    sourceInfo += " (" + referredAstNode.getOriginalCompilationUnit().getName() + ")";
                }
                String targetInfo = referringASTNode.getClass().getSimpleName();
                if (referringASTNode.getOriginalCompilationUnit() != null) {
                    targetInfo += " (" + referringASTNode.getOriginalCompilationUnit().getName() + ")";
                }
                logAnalysisInfo(vp1ID, vp2ID, sourceInfo, targetInfo);
            }
        }

        return variationPoints;
    }
    
    @Override
    public VPMAnalyzerConfigurations getConfigurations() {
    	VPMAnalyzerConfigurations configurations = new VPMAnalyzerConfigurations();
    	BooleanConfiguration fullReferredTreeConfig = new BooleanConfiguration(CONFIG_LABEL_FULL_REFERRED_TREE, null, DEFAULT_CONFIG_FULL_REFERRED_TREE);
    	configurations.addConfigurations("General Settings", fullReferredTreeConfig);
    
        return configurations;
    }

    @Override
    public String getName() {
        return "Program Structure VPM Analyzer";
    }

    @Override
    public String getRelationshipLabel() {
        return RELATIONSHIP_LABEL_PROGRAM_STRUCTURE;
    }

}
