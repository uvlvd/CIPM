package org.splevo.jamopp.refactoring.java.caslicensehandler.cheatsheet.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class LicenseConstantsClassOpenTypeDialogAction extends OpenTypeDialogAction {
	@Override
	public void run(String[] params, ICheatSheetManager manager) {
		FilteredItemsSelectionDialog licenseConstantDialog = initTypeDialog();

		if (licenseConstantDialog.open() == Window.OK) {
			IFile file = (IFile) licenseConstantDialog.getResult()[0];
			CASLicenseHandlerConfiguration.getInstance().setLicenseConstant(convertToFile(file));
			return;
		}
		
		openErrorMessage();
	}
}
