#!/usr/bin/env python3
import argparse
import os
import shutil
import signal
import subprocess

# Absolute path to Gluon JavaFX directory
SYSTEM_INIT_BIN = "/usr/sbin/init"
GLUON_JAVAFX_PATH = "/opt/javafx-sdk"


# Helper method to split JVM properties specified as -Dkey=value
def jvm_property(data):
    parts = tuple(str(data).split('=', 1))
    return parts if len(parts) == 2 else (parts[0], '')


# Parse known arguments and preserve others
parser = argparse.ArgumentParser(description='Gluon JavaFX Kiosk Launcher', allow_abbrev=False)
parser.add_argument('--add-modules', default='')
parser.add_argument('-p', '--module-path', default='')
parser.add_argument('-D', default=[], action='append', type=jvm_property, dest='properties')
args, unknown_args = parser.parse_known_args()

# Patch '--module-path' option
module_path = list(filter(None, args.module_path.split(':')))
module_path.insert(0, GLUON_JAVAFX_PATH + '/lib')

# Patch '--add-modules' option
add_modules = list(filter(None, args.add_modules.split(',')))
add_modules.insert(0, 'javafx.controls')

# Patch generic properties
properties = dict(filter(None, args.properties))
properties.setdefault('glass.platform', 'Monocle')
properties.setdefault('monocle.platform', 'EGL')
properties.setdefault('monocle.platform.traceConfig', 'false')
properties.setdefault('monocle.egl.lib', GLUON_JAVAFX_PATH + '/lib/libgluon_drm.so')
properties.setdefault('egl.displayid', '/dev/dri/card0')
properties.setdefault('javafx.verbose', 'false')
properties.setdefault('prism.verbose', 'false')

# Patch 'java.library.path' property
java_library_path = list(filter(None, properties.get('java.library.path', '').split(':')))
java_library_path.insert(0, GLUON_JAVAFX_PATH + '/lib')
properties['java.library.path'] = ':'.join(java_library_path)

# Patch environment variables
jvm_env = os.environ.copy()
jvm_env['ENABLE_GLUON_COMMERCIAL_EXTENSIONS'] = 'true'

# Build final list of JVM arguments
jvm_args = [
    '--module-path', ':'.join(module_path),
    '--add-modules', ','.join(add_modules),
]
jvm_args.extend(['-D' + key + '=' + value for key, value in properties.items()])
jvm_args.extend(unknown_args)

# Search for absolute path of JVM
jvm_path = shutil.which('java')
if jvm_path is None:
    parser.error("Unable to find 'java' binary in current PATH")

# Ensure we are running as root
if os.geteuid() != 0:
    parser.error("Unable to execute 'java-kiosk' without running as root")

# Run application in kiosk mode
try:
    # Ignore Ctrl+C for python process to ensure completion
    signal.signal(signal.SIGINT, lambda signum, frame: None)

    # Switch to runlevel 3 to stop X11
    subprocess.run([SYSTEM_INIT_BIN, '3'])

    # Execute JVM with patched options
    subprocess.run([jvm_path] + jvm_args, env=jvm_env)
except KeyboardInterrupt:
    # Silently ignore KeyboardInterrupt, we expect the user to sometimes abort the script
    pass
finally:
    # Switch back to runlevel 5 to start X11
    subprocess.run([SYSTEM_INIT_BIN, '5'])
