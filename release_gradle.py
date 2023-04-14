from distutils.dir_util import copy_tree
from os import path, chdir, getcwd
import subprocess
import re
import shutil


gradle_module_path = "snowflake-gradle-plugin"
core_path = "snowflake-plugins-core"
gradle_release_path = "release/snowflake-gradle-plugin"
java_src_path = "src/main/java"
build_file_name = "build.gradle"
settings_file_name = "settings.gradle"


# Copy source code from core and plugin modules
copy_tree(path.join(gradle_module_path, java_src_path), path.join(gradle_release_path, java_src_path))
copy_tree(path.join(core_path, java_src_path), path.join(gradle_release_path, java_src_path))
shutil.copyfile(path.join(gradle_module_path, build_file_name), path.join(gradle_release_path, build_file_name))
shutil.copyfile(path.join(gradle_module_path, settings_file_name), path.join(gradle_release_path, settings_file_name))

# Read core build script
with open(path.join(core_path, build_file_name), 'r') as core_build_file:
    file_string = core_build_file.read()
# Match the dependencies block in core build script
match = re.search(r'dependencies \{(.+?)\}', file_string, re.DOTALL)
if match:
    # Extract the matched block of lines and split them into a list
    matched = match.group(1).split('\n')
    core_dependencies_lines = [line + "\n" for line in matched if line]
else:
    print("No dependencies found in core build")
# Read release build script
with open(path.join(gradle_release_path, build_file_name), 'r') as release_build_file:
    release_build_lines = release_build_file.readlines()
# Replace the "snowflake-plugins-core" dependency with transitive dependencies
for i, line in enumerate(release_build_lines):
    if "snowflake-plugins-core" in line:
        # Replace the line with the new lines
        if (core_dependencies_lines): 
            release_build_lines = release_build_lines[:i] + core_dependencies_lines + release_build_lines[i+1:]
        else:
            release_build_lines = release_build_lines[:i] + release_build_lines[i+1:]
        break
# Overwrite the release build script
with open(path.join(gradle_release_path, build_file_name), 'w') as release_build_file:
    release_build_file.writelines(release_build_lines)

chdir(path.join(getcwd(), gradle_release_path))
p = subprocess.run(["gradle", "publishToMavenLocal"], check=True)