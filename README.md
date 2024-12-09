# Commit-Based Continuous Integration of Performance Models

# Lua

## Project setup

You will need [Eclipse Modeling Tools 2022-09](https://www.eclipse.org/downloads/packages/release/2022-09/r/eclipse-modeling-tools) as well as Java JDK 11 and 17. For information on how to install from an updatesite or import a project/package into eclipse see below.

1. Clone this repo, switch to branch `lua-dev` then initialize and prepare the submodules:
    - `git submodule init`
    - `git submodule update` (ignore errors)
    - `cd` into Vitruv-Change folder `commit-based-cipm\bundles\Vitruv-Change`
    - `git checkout de9272e` to checkout the necessary commit (see commit linked in GibHub for this submodule)
   - cd to `CIPM/CIPM-Pipeline` 
   - checkout branch `vitruv3` (`git checkout vitruv3`)
   - cd to `CIPM\CIPM-Pipeline\cipm.consistency.bridge.eclipse\cipm.consistency.base.shared\dep-generator/`
   - run `./gralew.bat bundle copyBundles`
2. Clone the [Lua Code Model Repository](https://github.com/uvlvd/Metamodels/tree/refactoring) (checkout the `refactoring` branch) //TODO: should be the main branch
3. In Eclipse, install Lombok from its [updatesite](https://projectlombok.org/p2)
4. In Eclipse, import projects: 
    - From the Lua Code Model Repository import all projects  (`org.xtext.lua*`)
    - From `commit-based-cipm\bundles\Vitruv-Change\bundles` import 
        `tools.vitruv.change.composite`
    - From `CIPM-Pipeline\cipm.consistency.bridge.eclipse` import 
        `cipm.consistency.base.shared` and 
        `cipm.consistency.models.instrumentation`
    - From `commit-based-cipm/bundles/fi` import all
5. Import and set target platform: 
    - From `commit-based-cipm\releng-dev\` import `cipm.consistency.targetplatform`
    - open `cipm.consistency.targetplatform.development.target`
    - wait until eclipse is finished loading
    - click `Set as Active Target Platform` (top-right corner)
    - (clean projects/close exlipse and reopen (close .target files first) until no errors are shown)
6. Trigger modelcode generation for the genmodels (Open the `.genmodel` file and click `Generator -> Generate Model Code`):
    - org.splevo.diffing/model/splevodiff.genmodel
    - org.splevo.jamopp.diffing/model/jamoppdiff.genmodel
7. (Add `xtend-gen` folder to `tools.vitruv.change.composite` to avoid error)
    - this may not be necessary: during testing, adding the folder did not solve the issue (since no files were generated inside the `xtend-gen` folder). Installing Xtext and cleaning all projects solved the issue (see next step).
8. Generate Lua model code:
    - Install Xtext from the marketplace (tested with version 2.37.0)
    - `org.xtext.lua/src/org.xtext.lua/GenerateLua.mwe2 -> Right click -> Run as -> MWE2 Workflow`  (TODO: this needs Xtext to be installed?)
9. Start second Eclipse instance: `Run -> Run Configurations -> Eclipse Application -> Launch Runtime Eclipse`
    - make sure "All workspace and enabled target Plug-ins" is selected in the Plug-ins tab
10. In second instance, import all from `commit-based-cipm/bundles/fi`

##### Debugging second Eclipse instance (TODO: remove?)
 - add xtend-gen folder to cipm.consistency.vsum.test
 - change StringUtils import in cipm.consistency.commitintegration.lang.lua to org.apache.commons.lang3 (plugin) from lang (imported packages)

##### Installing from an update site
To install from an update site, in Eclipse go to `Help -> Install new Software... -> Add...`, then insert the URL (in case of an URL update site) or the Archive (in case of a downloaded package), select the items you want to install and click `Next`, accepting licence agreements/trust as necessary, wait for the installation to finish and restart eclipse when prompted.

##### Importing a package/project into Eclipse
Go to `File -> Import... -> General -> Existing Projects into Workspace`, clich `Next`, click `Browse`, go to the location on disk, `Select Folder` and choose the desired packages/projects.

# Java

**The code review summary of this project can be found [here](./CODE_REVIEW.md)**

This repository provides the prototypical implementation for the change extraction, change propagation, incremental model update, and adaptive instrumentation of the CIPM approach.

# Setup
**Note: a new setup method is currently in development. Therefore, some configurations are not supported yet or can cause issues.**

1. Install Java JDK 11 and 17

1. Install Maven
    - The `JAVA_HOME` environment variable must be set pointing to a JDK 11 (so that Maven can find the Java executable in `%JAVA_HOME%\bin\java.exe`).

1. Clone this Repository

1. For the initial setup after cloning the repository, setup scripts for Linux and Windows are provided.
    - Execute `./scripts/setup.sh` (Linux) or `.\scripts\setup.bat` (Windows) from the top-level of this repository.
	    - If you encounter an error like `Internal error: java.lang.IllegalArgumentException: bundleLocation not found: [home]/.m2/[..]` it can help to delete the file `[home]/.m2/repository/.meta/p2-artifacts.properties` and restart the script.

Further the project needs two instances of the Eclipse Modeling Tools 2022-09 setup as follows:

## First Eclipse Instance

The first instance contains plugins and dependencies which need to be loaded in the second instance later on.

1. Please add the following update site and install the according components:
    - Lombok (from its [update site](https://projectlombok.org/p2))
    - CheckStyle (optional, from its [update site](https://checkstyle.org/eclipse-cs-update-site))
	    - If checkstyle is used: Import the following projects and enable the containing checkstyle rulesets
			- `CodingConventions/*`

1. Import the following projects:
    - `commit-based-cipm/releng-dev/cipm.consistency.targetplatform`
    - `commit-based-cipm/fi/*` except the domain projects
    - `luaXtext/org.xtext.lua.*`

1. Open the target platform from `commit-based-cipm/releng-dev/cipm.consistency.targetplatform`, activate and then reload it.

1. Trigger modelcode generation for the genmodels (Open the genmodel file and click `Generator > Generate Model Code`):
    - org.splevo.diffing/model/splevodiff.genmodel
    - org.splevo.jamopp.diffing/model/jamoppdiff.genmodel

1. Generate the Lua xtext code by running `org.xtext.lua/src/org.xtext.lua/GenerateLua.mwe2`

1. If some projects show errors reload the target platform and  run `Project > Clean > Clean all projects`

1. Start the `SecondInstance` launch configuration and proceed to the next section

## Second Eclipse Instance

1. Import all the following projects into the first instance:
   - All projects located in the `commit-based-cipm/bundles/si` folder

1. Make sure that the Setting ` Preferences > Xtend > Compiler > General > Source Compatibility level ..`  is set to Java 11.

1. Run the `CodeReviewEntryPoint` launch configuration. The configuration executes a unit test that executes an integration.
