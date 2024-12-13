package cipm.consistency.commitintegration.lang.lua;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xtext.lua.LuaParser;
import org.xtext.lua.tests.PreprocessingUtils;
//import org.xtext.lua.PreprocessingUtils;

import cipm.consistency.tools.evaluation.data.CodeModelCorrectnessEval;
import cipm.consistency.tools.evaluation.data.EvaluationDataContainer;

public class CodeModelEvaluator {
	private static final Logger LOGGER = Logger.getLogger(CodeModelEvaluator.class);

	
	private CodeModelCorrectnessEval evalData;
	
	public CodeModelEvaluator() {
		evalData = EvaluationDataContainer.get().getCodeModelCorrectness();
		evalData.reset();
	}

	public static void evaluateCodeModelCorrectness(Path sourceCodeDirPath) {
		var evaluator = new CodeModelEvaluator();
		try {
			evaluator.evaluateSourceCodeDir(sourceCodeDirPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void evaluateSourceCodeDir(Path sourceCodeDirPath) throws IOException {
		var resourceSet = new LuaParser().parse(sourceCodeDirPath);
		for (var r : resourceSet.getResources()) {
			var outputStream = new ByteArrayOutputStream();
			r.save(outputStream, new HashMap<>());
			var parsedAndSerialized = outputStream.toString();
			
			var originalPath = r.getURI().toFileString();
			// TODO: might need to specify charset, defaults to UTF-8
			String original = Files.readString(Paths.get(originalPath));

			updateEvalData(originalPath, original, parsedAndSerialized);
		}
	}
	
	private void updateEvalData(final String originalPath, final String original, final String parsedAndSerialized) 
			throws IOException {
		var currentIdentical = evalData.getIdenticalFiles();
		var currentSimilar = evalData.getSimilarFiles();
		var currentDissimilar = evalData.getDissimilarFiles();
		
		if (original.equals(parsedAndSerialized)) {
			evalData.setIdenticalFiles(currentIdentical + 1);
		} else if (compareNormalizedStrings(original, parsedAndSerialized)) {
			evalData.setSimilarFiles(currentSimilar + 1);
		} else {
			evalData.setDissimilarFiles(currentDissimilar + 1);
			
			// log info about dissimilar files
			var normalizedOriginal = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(original);
			var normalizedParsedAndSerialized = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(parsedAndSerialized);
			logDissimilarity(originalPath, normalizedOriginal, normalizedParsedAndSerialized);
		}
	}
	
	private boolean compareNormalizedStrings(final String str1, final String str2) {
		var s1 = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(str1);
		var s2 = PreprocessingUtils.removeCommentsAndWhiteSpacesAndNewLines(str2);
		var result = s1.equals(s2);
		
		return result;
	}
	
	private void logDissimilarity(final String path, final String normalizedOriginal, final String normalizedParsedAndSerialized) {
		var diffStr = StringUtils.difference(normalizedOriginal, normalizedParsedAndSerialized);
		LOGGER.error("Normalized original and parsed files differ for " + path + ".\n"
				+ "		Normalized original: '" + normalizedOriginal + "'\n"
				+ "		Normalized parsed: '" + normalizedParsedAndSerialized + "'\n"
				+ "		Difference found at: '" + diffStr.substring(0, diffStr.length() < 101 ? diffStr.length() : 100) + "'"
				);
	}


}
