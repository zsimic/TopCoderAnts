'''
Created on Nov 27, 2011

@author: zoran
'''

import os

from PySide import QtCore, QtGui

GRAY_TEXT = QtGui.QPalette()
RED_TEXT = QtGui.QPalette()
BLUE_TEXT = QtGui.QPalette()
BLACK_TEXT = QtGui.QPalette()

class Holder(object):
  pass

def resolved_path(path):
  path = os.path.expanduser(path)
  return os.path.abspath(path)

def set_box_padding(box, margin=4, padding=4):
  box.setContentsMargins(margin, margin, margin, margin)
  box.setSpacing(padding)

def attach_layout(parent, layout, width=0, height=0):
  if parent:
    parent.setLayout(layout)
  set_sizing(parent, width, height)

def new_hbox(parent, margin=4, padding=4, width=0, height=0):
  hbox = QtGui.QHBoxLayout()
  set_box_padding(hbox, margin, padding)
  attach_layout(parent, hbox, width, height)
  return hbox

def new_vbox(parent, margin=4, padding=4, width=0, height=0):
  vbox = QtGui.QVBoxLayout()
  set_box_padding(vbox, margin, padding)
  attach_layout(parent, vbox, width, height)
  return vbox

def new_Label(container, text, width):
  widget = QtGui.QLabel()
  widget.setAutoFillBackground(True)
  embox_and_size(container, widget, text, width)
  return widget

def new_Combo(container, items, action, width):
  widget = QtGui.QComboBox()
  if items:
    for i in items:
      widget.addItem(str(i))
  if action:
    widget.activated.connect(action)
  embox_and_size(container, widget, None, width)
  return widget

def new_PushButton(container, text, action, width):
  widget = QtGui.QPushButton()
  widget.setFocusPolicy(QtCore.Qt.NoFocus)
  embox_and_size(container, widget, text, width)
  if action:
    widget.clicked.connect(action)
  return widget

def new_CheckBox(container, text, action, width):
  widget = QtGui.QCheckBox()
  widget.setFocusPolicy(QtCore.Qt.NoFocus)
  embox_and_size(container, widget, text, width)
  if action:
    widget.clicked.connect(action)
  return widget

def new_RadioButton(container, text, action, width):
  widget = QtGui.QRadioButton()
  widget.setFocusPolicy(QtCore.Qt.NoFocus)
  embox_and_size(container, widget, text, width)
  if action:
    widget.clicked.connect(action)
  return widget

def new_HSpacer(container):
  spacer = QtGui.QSpacerItem(1, 1, QtGui.QSizePolicy.Expanding)
  container.addItem(spacer)
  return spacer

def new_LineEdit(container, text, width):
  widget = QtGui.QLineEdit()
  embox_and_size(container, widget, text, width)
  return widget

def embox_and_size(container, widget, text, width, height=0):
  if text:
    widget.setText(text)
  set_sizing(widget, width, height)
  container.addWidget(widget)

def set_sizing(widget, width, height):
  if not widget:
    return
  if abs(width) > 0:
    widget.setMinimumWidth(abs(width))
  sizePolicy = QtGui.QSizePolicy()
  sizePolicy.setHeightForWidth(False)
  if width > 0:
    sizePolicy.setHorizontalPolicy(QtGui.QSizePolicy.Fixed)
    sizePolicy.setHorizontalStretch(0)
  else:
    sizePolicy.setHorizontalPolicy(QtGui.QSizePolicy.Expanding)
    sizePolicy.setHorizontalStretch(1)
  if height > 0:
    sizePolicy.setVerticalPolicy(QtGui.QSizePolicy.Fixed)
    sizePolicy.setVerticalStretch(0)
  else:
    sizePolicy.setVerticalPolicy(QtGui.QSizePolicy.Expanding)
    sizePolicy.setVerticalStretch(1)
  widget.setSizePolicy(sizePolicy)

GRAY_TEXT.setColor(QtGui.QPalette.WindowText, QtCore.Qt.gray)
RED_TEXT.setColor(QtGui.QPalette.WindowText, QtCore.Qt.red)
BLUE_TEXT.setColor(QtGui.QPalette.WindowText, QtCore.Qt.blue)
BLACK_TEXT.setColor(QtGui.QPalette.WindowText, QtCore.Qt.black)
