#!/usr/bin/env python
# -*- coding: utf-8 -*-

#import sys

from cx_Freeze import setup, Executable
from PySide import QtCore, QtGui, QtOpenGL
from OpenGL.GL import *

setup(
        name = "PAnts",
        version = "1.0",
        description = "Python Ants",
        executables = [Executable("PAnts.py")])
