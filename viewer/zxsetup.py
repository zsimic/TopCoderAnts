#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys

from cx_Freeze import setup, Executable
from PySide import QtCore, QtGui, QtOpenGL
from OpenGL.GL import *

base = None
if sys.platform == "win32":
    base = "Win32GUI"

includes = ['numpy', 'OpenGL', 'OpenGL.platform']
excludes = ['_gtkagg', '_tkagg', 'bsddb', 'curses', 'email', 'pywin.debugger', 'nose',
             'pywin.debugger.dbgcon', 'pywin.dialogs', 'tcl',
             'Tkconstants', 'Tkinter']
packages = []
path = []

Target_1 = Executable(
  script = "AntsViewer.py",
  initScript = None,
  base = base,
  compress = True,
  copyDependentFiles = True,
  appendScriptToExe = False,
  appendScriptToLibrary = False,
  icon = None
)

setup(
        name = "AntsViewer",
        version = "1.0",
        description = "Python Ants",
        author = "Zoran Simic",
        options = {
          "build_exe": {
            "includes": includes,
              "excludes": excludes,
              "packages": packages,
              "path": path
              }
        },
        executables = [Target_1]
)
