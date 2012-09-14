package org.splevo.ui.listeners;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Shell;
import org.splevo.ui.editors.SPLevoProjectEditor;
import org.splevo.ui.workflow.BasicSPLevoWorkflowConfiguration;
import org.splevo.ui.workflow.DiffingWorkflowDelegate;
import org.splevo.ui.workflow.InitVPMWorkflowDelegate;

/**
 * Mouse adapter to listen for events which trigger the extraction of the source
 * projects.
 */
public class InitVPMListener extends MouseAdapter {

	/** The internal reference to the splevo project editor to work with. */
	private SPLevoProjectEditor splevoProjectEditor = null;

	/**
	 * Constructor requiring the reference to a splevoProject.
	 * 
	 * @param splevoProject
	 *            The references to the project.
	 */
	public InitVPMListener(SPLevoProjectEditor splevoProjectEditor) {
		this.splevoProjectEditor = splevoProjectEditor;
	}

	@Override
	public void mouseUp(MouseEvent e) {
		
		// build the job configuration
		BasicSPLevoWorkflowConfiguration config = buildWorflowConfiguration();
		
		
		// validate configuration
		if(!config.isValid()){
			Shell shell = e.widget.getDisplay().getShells()[0];
			MessageDialog.openError(shell, "Invalid Project Configuration", config.getErrorMessage());
			return;
		}
		
		// trigger workflow
		InitVPMWorkflowDelegate workflowDelegate = new InitVPMWorkflowDelegate(config);
		IAction action = new Action("Init VPM"){};
		workflowDelegate.run(action);
	}

	/**
	 * Build the configuration for the workflow.
	 * @return The prepared configuration.
	 */
	private BasicSPLevoWorkflowConfiguration buildWorflowConfiguration() {
		BasicSPLevoWorkflowConfiguration config = new BasicSPLevoWorkflowConfiguration();
		config.setSplevoProject(splevoProjectEditor.getSplevoProject());
		config.setSplevoProjectEditor(splevoProjectEditor);
		return config;
	}

}
