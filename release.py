from distutils.dir_util import copy_tree
from os import path, chdir, getcwd
import subprocess
import xml.etree.ElementTree as ET


maven_module_path = "snowflake-maven-plugin-module"
core_path = "snowflake-plugins-core"
maven_release_path = "release/snowflake-maven-plugin"
java_src_path = "src/main/java"
pom_file_name = "pom.xml"


# Copy source code from core and plugin modules
copy_tree(path.join(maven_module_path, java_src_path), path.join(maven_release_path, java_src_path))
copy_tree(path.join(core_path, java_src_path), path.join(maven_release_path, java_src_path))

# Parse pom xml
ET.register_namespace('', "http://maven.apache.org/POM/4.0.0")
pom = ET.parse(path.join(maven_module_path, pom_file_name))
pom_project = pom.getroot()
# Change the artifact ID
pom_project.find("{*}artifactId").text = "snowflake-maven-plugin"
# Remove reference to parent POM
pom_parent_elem = pom_project.find("{*}parent")
version_text = pom_parent_elem.find("{*}version").text
pom_project.remove(pom_parent_elem)
# Set release version
pom_version = ET.SubElement(pom_project, 'version')
pom_version.text = version_text
# Remove dependency on snowflake core
pom_dependencies = pom_project.find("{*}dependencies")
for dependency in pom_dependencies:
    dependency_name = dependency.find("{*}artifactId").text
    if dependency_name == core_path:
        core_dependency = dependency
pom_dependencies.remove(core_dependency)
# Copy dependencies from snowflake core
# Dependencies from parent project may need to be copied in the future 
core_pom = ET.parse(path.join(core_path, pom_file_name))
core_pom_project = core_pom.getroot()
core_pom_dependencies = core_pom_project.find("{*}dependencies")
for dependency in core_pom_dependencies:
    pom_dependencies.append(dependency)
# Write new pom to release folder
pom.write(path.join(maven_release_path, pom_file_name))

chdir(path.join(getcwd(), maven_release_path))
p = subprocess.run(["mvn", "install"], check=True)