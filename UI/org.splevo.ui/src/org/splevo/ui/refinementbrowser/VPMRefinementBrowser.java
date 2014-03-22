/*******************************************************************************
 * Copyright (c) 2014
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Klatt
 *******************************************************************************/
package org.splevo.ui.refinementbrowser;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.wb.swt.SWTResourceManager;
import org.splevo.ui.editors.SPLevoProjectEditor;
import org.splevo.vpm.refinement.Refinement;

/***
 * An editor to view and accept, decline or modify the 
 * variation point model refinement recommendations.
 * 
 * @author Benjamin Klatt
 *
 */
public class VPMRefinementBrowser extends EditorPart {

	/** The public id to reference the editor. */
	public static final String ID = "org.splevo.ui.editor.VPMRefinementEditor"; //$NON-NLS-1$

	/** The editor input of the browser. */
	private VPMRefinementBrowserInput input;

	/** The checkbox tree viewer to select the refinements to apply. */
	private TreeViewer refinementTreeViewer;

	/** The form toolkit to manipulate the form. */
	private FormToolkit toolkit;
	
	/** The main form of the editor. */
	private Form form;

	private RefinementDetailsView detailsView;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		if (!(editorInput instanceof VPMRefinementBrowserInput)) {
			throw new RuntimeException("Wrong input");
		}

		this.input = (VPMRefinementBrowserInput) editorInput;
		setSite(site);
		setInput(editorInput);
		setPartName("VPM Refinement Browser");
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 * 
	 * @param parent The parent ui element to create this editor in.
	 */
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createForm(parent);
		form.setText("SPLevo Refinement Browser");
		toolkit.decorateFormHeading(form);
		form.getMenuManager().add(
				new ApplyRefinementsAction(this, "Apply Refinements"));

		createFormContent(form.getBody());

		SashForm sashForm = new SashForm(form.getBody(), SWT.BORDER | SWT.FILL);
		sashForm.setSashWidth(2);
		sashForm.setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		toolkit.adapt(sashForm);
		toolkit.paintBordersFor(sashForm);
	
		this.refinementTreeViewer = new TreeViewer(sashForm, SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		refinementTreeViewer
				.setContentProvider(new RefinementTreeContentProvider());
		refinementTreeViewer
				.setLabelProvider(new RefinementTreeLabelProvider());
		refinementTreeViewer.setAutoExpandLevel(2);
		refinementTreeViewer.setInput(input.getRefinements());

		// Listener to expand the tree
		refinementTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				TreeViewer viewer = (TreeViewer) event.getViewer();
				IStructuredSelection thisSelection = (IStructuredSelection) event
						.getSelection();
				Object selectedNode = thisSelection.getFirstElement();
				viewer.setExpandedState(selectedNode,
						!viewer.getExpandedState(selectedNode));
			}
		});

		detailsView = new RefinementDetailsView(sashForm);

		sashForm.setWeights(new int[] { 3, 7 });
		RefinementSelectionListener listener = new RefinementSelectionListener(detailsView, input, toolkit);
		refinementTreeViewer.addSelectionChangedListener(listener);
		initContextMenu();
	}

	/**
	 * Create the form body providing the real content.
	 * 
	 * @param parent The parent ui element to create the form in.
	 */
	private void createFormContent(Composite parent) {
		form.getBody().setLayout(new FillLayout(SWT.HORIZONTAL));
	}

	/**
	 * Get the selected refinements.
	 * 
	 * @return The list of refinements activated in the tree view.
	 */
	public List<Refinement> getSelectedRefinements() {
		return input.getRefinements();
	    
		// TODO: Check refinement selection in the ui (only delete refinement if not to apply?)
//	    List<Refinement> refinements = new ArrayList<Refinement>();
//		for (VPMRefinementAnalyzer analyzer : input.getRefinements().keySet()) {
//			List<Refinement> refinementsForAnalyzer = input.getRefinements()
//					.get(analyzer);
//			refinements.addAll(refinementsForAnalyzer);
//		}
//		return refinements;
		
	}

	/**
	 * Get the splevo project editor that was originally used to trigger the
	 * analysis process.
	 * @return Get the reference to the splevo dashboard editor.
	 */
	public SPLevoProjectEditor getSPLevoProjectEditor() {
		return this.input.getSplevoEditor();
	}

	/** Passing the focus request to the form. */
	public void setFocus() {
		form.setFocus();
	}

	/**
	 * Disposes the toolkit.
	 */
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// never be called
	}

	@Override
	public void doSaveAs() {
		// never be called
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/**
     * initialize the context menu.
     */
    private void initContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                Action action = new DeleteRefinementAction(refinementTreeViewer, detailsView);
                manager.add(action);
            }
        });
        Menu menu = menuMgr.createContextMenu(refinementTreeViewer.getTree());
        refinementTreeViewer.getTree().setMenu(menu);
    }
}
