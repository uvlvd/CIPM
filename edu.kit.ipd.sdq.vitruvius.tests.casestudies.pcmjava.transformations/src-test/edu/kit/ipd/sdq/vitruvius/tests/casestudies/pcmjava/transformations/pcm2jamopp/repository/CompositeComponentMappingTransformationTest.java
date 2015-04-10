package edu.kit.ipd.sdq.vitruvius.tests.casestudies.pcmjava.transformations.pcm2jamopp.repository;

import java.io.IOException;

import org.emftext.language.java.containers.CompilationUnit;
import org.emftext.language.java.containers.Package;
import org.junit.Test;

import de.uka.ipd.sdq.pcm.core.composition.AssemblyContext;
import de.uka.ipd.sdq.pcm.repository.BasicComponent;
import de.uka.ipd.sdq.pcm.repository.CompositeComponent;
import de.uka.ipd.sdq.pcm.repository.OperationInterface;
import de.uka.ipd.sdq.pcm.repository.OperationProvidedRole;
import de.uka.ipd.sdq.pcm.repository.OperationRequiredRole;
import de.uka.ipd.sdq.pcm.repository.Repository;
import de.uka.ipd.sdq.pcm.repository.RepositoryFactory;
import edu.kit.ipd.sdq.vitruvius.tests.casestudies.pcmjava.transformations.pcm2jamopp.PCM2JaMoPPTransformationTest;
import edu.kit.ipd.sdq.vitruvius.tests.casestudies.pcmjava.transformations.utils.PCM2JaMoPPTestUtils;

public class CompositeComponentMappingTransformationTest extends PCM2JaMoPPTransformationTest {

    @Test
    public void testCreateCompositeComponent() throws Throwable {
        final CompositeComponent compositeComponent = this.createAndSyncRepoAndCompositeComponent();

        this.assertCompositeComponentCorrespondences(compositeComponent, PCM2JaMoPPTestUtils.COMPOSITE_COMPONENT_NAME);
    }

    @Test
    public void testAddProvidedRoleToCompositeComponent() throws Throwable {
        final CompositeComponent compositeComponent = this.createAndSyncRepoAndCompositeComponent();
        final OperationInterface opInterface = this.addInterfaceToReposiotryAndSync(
                compositeComponent.getRepository__RepositoryComponent(), PCM2JaMoPPTestUtils.INTERFACE_NAME);

        final OperationProvidedRole providedRole = super.createAndSyncOperationProvidedRole(opInterface,
                compositeComponent);

        this.assertCompositeComponentCorrespondences(compositeComponent, PCM2JaMoPPTestUtils.COMPOSITE_COMPONENT_NAME);
        this.assertOperationProvidedRole(providedRole);
    }

    @Test
    public void testAddOperationRequiredRoleToCompositeComponent() throws Throwable {
        final CompositeComponent compositeComponent = this.createAndSyncRepoAndCompositeComponent();
        final OperationInterface opInterface = this.addInterfaceToReposiotryAndSync(
                compositeComponent.getRepository__RepositoryComponent(), PCM2JaMoPPTestUtils.INTERFACE_NAME);

        final OperationRequiredRole operationRequiredRole = super.createAndSyncOperationRequiredRole(opInterface,
                compositeComponent);

        this.assertCompositeComponentCorrespondences(compositeComponent, PCM2JaMoPPTestUtils.COMPOSITE_COMPONENT_NAME);
        this.assertOperationRequiredRole(operationRequiredRole);
    }

    @Test
    public void testAddAssemblyContextToCompositeComponent() throws Throwable {
        final CompositeComponent compositeComponent = this.createAndSyncRepoAndCompositeComponent();
        final BasicComponent basicComponent = super.addBasicComponentAndSync(compositeComponent
                .getRepository__RepositoryComponent());

        final AssemblyContext assemblyContext = this.createAndSyncAssemblyContext(compositeComponent, basicComponent);

        this.assertCompositeComponentCorrespondences(compositeComponent, PCM2JaMoPPTestUtils.COMPOSITE_COMPONENT_NAME);
        super.assertAssemblyContext(assemblyContext);
    }

    @Test
    public void testAddRequiredDelegationRoleToCompositeComponent() throws Throwable {
        final CompositeComponent compositeComponent = this.createAndSyncRepoAndCompositeComponent();
        final OperationInterface opInterface = this.addInterfaceToReposiotryAndSync(
                compositeComponent.getRepository__RepositoryComponent(), PCM2JaMoPPTestUtils.INTERFACE_NAME);

    }

    private CompositeComponent createAndSyncRepoAndCompositeComponent() throws IOException {
        final Repository repo = this.createAndSyncRepository(this.resourceSet, PCM2JaMoPPTestUtils.REPOSITORY_NAME);
        final CompositeComponent compositeComponent = this.addCompositeComponentAndSync(repo);
        return compositeComponent;
    }

    private CompositeComponent addCompositeComponentAndSync(final Repository repo) {
        final CompositeComponent cc = RepositoryFactory.eINSTANCE.createCompositeComponent();
        cc.setEntityName(PCM2JaMoPPTestUtils.COMPOSITE_COMPONENT_NAME);
        cc.setRepository__RepositoryComponent(repo);
        super.triggerSynchronization(repo);
        return cc;
    }

    /**
     * a composite component should correspond to a class and a package
     *
     * @param compositeComponent
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    private void assertCompositeComponentCorrespondences(final CompositeComponent compositeComponent,
            final String expectedName) throws Throwable {
        super.assertCorrespondnecesAndCompareNames(compositeComponent, 3, new java.lang.Class[] { Package.class,
                org.emftext.language.java.classifiers.Class.class, CompilationUnit.class }, new String[] {
                expectedName, expectedName + "Impl", expectedName + "Impl" });
    }

}
