package cipm.consistency.commitintegration.lang.lua.detection;

public class DirectoryComponentConfig {
	
	private String componentName;
	private String[] dirs;
	private String[] files;
	
	public DirectoryComponentConfig(String componentName, String[] dirs, String[] files) {
		this.componentName = componentName;
		this.dirs = dirs;
		this.files = files;
	}

	public String getComponentName() {
		return componentName;
	}

	public String[] getDirs() {
		return dirs;
	}

	public String[] getFiles() {
		return files;
	}

}
