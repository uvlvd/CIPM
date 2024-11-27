package cipm.consistency.vsum.test.appspace.tests;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;

import cipm.consistency.commitintegration.git.GitRepositoryWrapper;
import cipm.consistency.vsum.test.appspace.AppSpaceCITestController;

public class ApisixCaseStudyTest extends AppSpaceCITestController {
	private static final String CASESTUDY_SUBMODULE = "commit-based-cipm/bundles/si/cipm.consistency.vsum.test/ciTestRepos/apisix";
    private static final String[] SINGLE_COMMIT = {null, "21f0bb7"}; // probably needs to start with null?

    // this is the history used for the evaluation:
//    private static final String[] EVALUATION_HISTORY = { null, "e25fb6b", "7126aab", "d92b459", "e6d87e0", "542d2e9",
//            "6b7b35f", "1f2fb08"};
    
    // with two synthetic commits to evaluate some nasty CPR edge cases:
    private static final String[] EVALUATION_HISTORY = { null, "e25fb6b", "7126aab", "d92b459", "e6d87e0", "542d2e9",
            "6b7b35f", "1f2fb08", "616f65a", "de68b25"};

    public GitRepositoryWrapper getGitRepositoryWrapper()
            throws InvalidRemoteException, TransportException, GitAPIException, IOException {
        return super.getGitRepositoryWrapper()
        				.withLocalSubmodule(CIPM_GIT_DIR, CASESTUDY_SUBMODULE)
        				.initialize();
    }

//    @Test
//    public void testPropagationDangling() {
//        propagateAndEvaluate(COMMITS_DANGLING);
//    }

    @Test
    public void runEvaluation() {
        doCompleteEvaluation(SINGLE_COMMIT);
    }
}
