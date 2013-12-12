command line build command for java lexical check:
ant -buildfile [workspace direction]/build.xml -DECLIPSE_HOME [eclipse install direction]

check result as normal ant build result : FAILED or SUCCESSFUL

command line build command for generating js file and java lexical check:
workspace direction: ./com.intel.jsdroid/

java -jar [eclipse RCP direction]/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar -application org.eclipse.jdt.apt.core.aptBuild -data [workspace direction]

for example:
java -jar /home/luq/eclipse-rcp-indigo-SR2/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar -application org.eclipse.jdt.apt.core.aptBuild -data /home/luq/workspace_indigo_mpt_runtime/

Note, the argument of -data is a %workspace% of eclipse. The org.eclipse.equinox.launcher_xxxx.jar is located in it. 

For our autobuild workflow,
    1. update source by git
    2. copy com.intel.jsdroid to %workspace%
    3. perform cmd line: java -jar [eclipse RCP direction]/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar -application org.eclipse.jdt.apt.core.aptBuild -data [workspace direction]

check result as : (Found xxxx errors)

samples: 
    success,
    ...
	(Found 1961 warnings) Build done
    
    fail,
    ...
    (Found 1 error + 1956 warnings) Build done
    
    
   
