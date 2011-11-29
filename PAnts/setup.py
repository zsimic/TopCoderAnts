#!/usr/bin/env python

"""
This is a setup.py script generated by py2applet

Usage:
    python setup.py py2app
"""

from setuptools import setup

includes = ['numpy']
excludes = []
DATA_FILES = []
OPTIONS = {
  'argv_emulation': False,
  'includes': includes
}

#need to add this to dist/PAnts.app/Contents/Resources/__boot__.py:
#    sys.path = [os.path.join(os.environ['RESOURCEPATH'], 'lib', 'python2.6')] + sys.path
#    sys.path = [os.path.join(os.environ['RESOURCEPATH'], 'lib', 'python2.6', 'lib-dynload')] + sys.path

setup(
    app=['PAnts.py'],
    data_files=DATA_FILES,
    options={'py2app': OPTIONS},
    setup_requires=['py2app'],
)
