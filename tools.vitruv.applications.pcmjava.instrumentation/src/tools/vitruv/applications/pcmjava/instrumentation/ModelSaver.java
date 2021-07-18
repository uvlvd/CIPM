package tools.vitruv.applications.pcmjava.instrumentation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emftext.language.java.LogicalJavaURIGenerator;
import org.emftext.language.java.commons.NamespaceAwareElement;
import org.emftext.language.java.containers.CompilationUnit;
import org.emftext.language.java.containers.JavaRoot;

/**
 * Saves the instrumented model.
 * 
 * @author Martin Armbruster
 */
public class ModelSaver {
	void saveModels(ResourceSet copyContainer, Resource copiedResource, Path target) {
		for (EObject root : new ArrayList<>(copiedResource.getContents())) {
			if (root instanceof CompilationUnit || root instanceof org.emftext.language.java.containers.Package) {
				JavaRoot cu = (JavaRoot) root;
				Resource newResource = copyContainer.createResource(createURI(cu, target));
				newResource.getContents().add(cu);
				try {
					newResource.save(null);
				} catch (IOException e) {
				}
			}
		}
	}
	
	private URI createURI(JavaRoot cu, Path newContainer) {
		Path resulting = newContainer;
		if (cu instanceof NamespaceAwareElement) {
			for (String ns : ((NamespaceAwareElement) cu).getNamespaces()) {
				resulting = resulting.resolve(ns);
			}
		}
		if (cu instanceof CompilationUnit) {
			resulting = resulting.resolve(cu.getName() + LogicalJavaURIGenerator.JAVA_FILE_EXTENSION);
		} else if (cu instanceof org.emftext.language.java.containers.Package) {
			resulting = resulting.resolve(LogicalJavaURIGenerator.JAVA_PACKAGE_FILE_NAME);
		}
		return URI.createFileURI(resulting.toAbsolutePath().toString());
	}
}
