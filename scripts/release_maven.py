from distutils.dir_util import copy_tree
from os import path, chdir, getcwd, chmod, listdir
import subprocess
import xml.etree.ElementTree as ET
import shutil
import stat
import sys
print(sys.version_info) 

maven_module_path = "snowflake-maven-plugin"
core_path = "snowflake-plugins-core"
maven_release_path = "release/snowflake-maven-plugin"
java_src_path = "src/main/java"
pom_file_name = "pom.xml"
scripts_path = "scripts/"
deploy_script = "deploy.sh"


# Copy source code from core and plugin modules to release folder
copy_tree(path.join(maven_module_path, java_src_path), path.join(maven_release_path, java_src_path))
copy_tree(path.join(core_path, java_src_path), path.join(maven_release_path, java_src_path))
# Copy deploy script to release folder
shutil.copyfile(path.join(scripts_path, deploy_script), path.join(maven_release_path, deploy_script))

# Parse pom xml
# ET.register_namespace('', "http://maven.apache.org/POM/4.0.0")



namespaces = {node[0]: node[1] for _, node in ET.iterparse(path.join(maven_module_path, pom_file_name), events=['start-ns'])}
#Iterates through the newly created namespace list registering each one.
for key, value in namespaces.items():
    ET.register_namespace(key, value)
default_ns = "{" + namespaces[""] + "}"

print(f"default namespace is {default_ns}")

pom = ET.parse(path.join(maven_module_path, pom_file_name))
pom_project = pom.getroot()
print(f"Current working directory: {getcwd()}")
print(f"Directory contents: {listdir()}")
print(f"Seeking file: {path.join(maven_module_path, pom_file_name)}")
print(ET.tostring(pom_project))
# Remove reference to parent POM
pom_parent_elem = pom_project.find(".//" + default_ns + "parent")
print(pom_parent_elem)
version_text = pom_parent_elem.find(".//" + default_ns + "version").text
pom_project.remove(pom_parent_elem)
# Set release version
pom_version = ET.SubElement(pom_project, 'version')
pom_version.text = version_text
# Remove dependency on snowflake core
pom_dependencies = pom_project.find(".//" + default_ns + "dependencies")
for dependency in pom_dependencies:
    dependency_name = dependency.find(".//" + default_ns + "artifactId").text
    if dependency_name == core_path:
        core_dependency = dependency
pom_dependencies.remove(core_dependency)
# Copy dependencies from snowflake core
# Dependencies from parent project may need to be copied in the future 
core_pom = ET.parse(path.join(core_path, pom_file_name))
core_pom_project = core_pom.getroot()
core_pom_dependencies = core_pom_project.find(".//" + default_ns + "dependencies")
for dependency in core_pom_dependencies:
    pom_dependencies.append(dependency)
# Write new pom to release folder
pom.write(path.join(maven_release_path, pom_file_name))

chdir(path.join(getcwd(), maven_release_path))
chmod("./deploy.sh", stat.S_IRWXU)
# p = subprocess.run(["./deploy.sh"], check=True)