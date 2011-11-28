#!/usr/bin/env python
# -*- coding: utf-8 -*-
# about.py - display about box with info on platform etc.

'''
Created on Nov 23, 2011

@author: zoran
'''

import numpy
import os
import re
import sys

import Gui

from Game import Board, GameFile
from PySide import QtCore, QtGui, QtOpenGL
from OpenGL.GL import *

SPEED_NORMAL_INTERVAL = 1000

class Point(object):

  def __init__(self, x=0, y=0, z=0):
    self.x = x
    self.y = y
    self.z = z

  def create_signals(self):
    if getattr(self, 'xChanged', None): return
    self.xChanged = QtCore.Signal(int)
    self.yChanged = QtCore.Signal(int)
    self.zChanged = QtCore.Signal(int)

  def setCoordinate(self, name, value):
    normalizer = getattr(self, 'normalizer', None)
    if callable(normalizer): value = normalizer(value)
    if value != getattr(self, name):
      setattr(self, name, value)
      sig = getattr(self, name + 'Changed', None)
      if sig: sig.emit(value)
      sig = getattr(self, 'glParent', None)
      if sig: sig.updateGL()

  def setX(self, x): self.setCoordinate('x', x)
  def setY(self, y): self.setCoordinate('y', y)
  def setZ(self, z): self.setCoordinate('z', z)
  def addX(self, dx): self.setX(self.x + dx)
  def addY(self, dy): self.setY(self.y + dy)
  def addZ(self, dz): self.setZ(self.z + dz)

class BoardView(QtOpenGL.QGLWidget):

  def __init__(self, parent=None):
    super(BoardView, self).__init__(parent)
    self.is_gl_initialized = 0
    self.is_game_finished = 0
    self.setMinimumSize(2*Board.BOARD_SIZE, 2*Board.BOARD_SIZE)
    self.zoom = 1.0
    self.camera = Point(0, 0, -Board.BOARD_SIZE)
    self.camera.glParent = self
    self.rotation = Point()
    self.rotation.glParent = self
    self.rotation.normalizer = self.normalizeAngle
    self.turn = 0
    board = Board()
    self.set_board(board)
    self.setFocusPolicy(QtCore.Qt.StrongFocus)
    self.shift_pressed = 0
    self.board_vertices = self.new_array(numpy.short, 8 * Board.BOARD_SIZE * Board.BOARD_SIZE)
    self.board_colors = self.new_array(numpy.ubyte, 12 * Board.BOARD_SIZE * Board.BOARD_SIZE)
    i = 0
    for y in range(Board.BOARD_SIZE):
      for x in range(Board.BOARD_SIZE):
        self.board_vertices[i+0] = x
        self.board_vertices[i+1] = y
        self.board_vertices[i+2] = x+1
        self.board_vertices[i+3] = y
        self.board_vertices[i+4] = x+1
        self.board_vertices[i+5] = y+1
        self.board_vertices[i+6] = x
        self.board_vertices[i+7] = y+1
        i += 8

  def new_array(self, dtype, n):
    return numpy.zeros(shape=n, dtype=dtype, order='C')

  def set_board(self, board):
    self.board = board
    if self.is_gl_initialized:
      self.camera.x = 0
      self.camera.y = 0
      self.zoom = 0.0
      self.set_zoom(1.0)
      self.update_model()
      self.updateGL()

  def set_zoom(self, zoom):
    if zoom < 0.1: zoom = 0.1
    elif zoom > 1.1:
      zoom = 1.1
#      self.camera.x = 0
#      self.camera.y = 0
    if zoom != self.zoom:
      self.zoom = zoom
      if self.is_gl_initialized:
        self.resizeGL(self.width(), self.height())
        self.updateGL()

  def normalizeAngle(self, angle):
    while (angle < 0): angle += 360 * 16
    while (angle > 360 * 16): angle -= 360 * 16
    return angle

  def xfactor(self):
    return self.zoom * float(Board.BOARD_SIZE) / self.side

  def yfactor(self):
    return self.zoom * float(Board.BOARD_SIZE) / self.side

  def keyReleaseEvent(self, event):
    self.shift_pressed = event.modifiers() & QtCore.Qt.ShiftModifier
    QtGui.QWidget.keyReleaseEvent(self, event)

  def keyPressEvent(self, event):
    self.shift_pressed = event.modifiers() & QtCore.Qt.ShiftModifier
    key = event.key()
    if key == QtCore.Qt.Key_Left:
      self.camera.addX(4 * self.xfactor())
    elif key == QtCore.Qt.Key_Right:
      self.camera.addX(-4 * self.xfactor())
    elif key == QtCore.Qt.Key_Up:
      self.camera.addY(-4 * self.yfactor())
    elif key == QtCore.Qt.Key_Down:
      self.camera.addY(4 * self.yfactor())
    else:
      QtGui.QWidget.keyPressEvent(self, event)

  def wheelEvent(self, event):
    d = event.delta()
    if d > 0: self.set_zoom(self.zoom / 1.1)
    elif d < 0: self.set_zoom(self.zoom * 1.1)
    event.accept()

  def mousePressEvent(self, event):
    self.lastPos = event.pos()
    event.accept()

  def mouseMoveEvent(self, event):
    dx = event.x() - self.lastPos.x()
    dy = event.y() - self.lastPos.y()
    if self.shift_pressed:
      if event.buttons() & QtCore.Qt.LeftButton:
        self.rotation.addX(8 * dy)
        self.rotation.addY(8 * dx)
      elif event.buttons() & QtCore.Qt.RightButton:
        self.rotation.addZ(8 * dx)
    elif event.buttons() & QtCore.Qt.LeftButton:
      self.camera.addX(dx * self.xfactor())
      self.camera.addY(-dy * self.yfactor())
    self.lastPos = event.pos()
    event.accept()

  def update_model(self):
    if not self.is_gl_initialized: return
    if not self.board: return
    glEnable(GL_COLOR_MATERIAL)
    if self.board.problem:
      i = len(self.board_colors) - 1
      while i >= 0:
        self.board_colors[i] = 0
        i -= 1
      return
    r = g = b = 255
    i = 0
    for y in range(Board.BOARD_SIZE):
      for x in range(Board.BOARD_SIZE):
        t = self.board.tile(x, y)
        self.update_tile_color(t, i)
        i += 12

  def update_tile_color(self, t, i):
    if not t.passable:
      r = g = b = 0
    elif len(t.ants):
      if t.ants[0].team.uid == 1:
        r = 255
        g = 0
        b = 0
      else:
        r = 0
        g = 0
        b = 255
    elif t.team:
      g = 50
      if t.team.nest != t:
        g += max(abs(t.x - t.team.nest.x), abs(t.y - t.team.nest.y)) * 30
      if t.team.uid == 1:
        r = 255
        b = g
      else:
        r = g
        b = 255
    elif t.food:
      g = 255
      r = b = 0
    else:
      r = g = b = 255
    self.board_colors[i+0] = r
    self.board_colors[i+1] = g
    self.board_colors[i+2] = b
    self.board_colors[i+3] = r
    self.board_colors[i+4] = g
    self.board_colors[i+5] = b
    self.board_colors[i+6] = r
    self.board_colors[i+7] = g
    self.board_colors[i+8] = b
    self.board_colors[i+9] = r
    self.board_colors[i+10] = g
    self.board_colors[i+11] = b

  def game_finished(self):
    self.is_game_finished = 1

  def run_turn(self):
    if not self.is_game_finished:
      self.board.run_turn(self)
      self.updateGL()

  def update_tile(self, tile):
    i = (tile.y * Board.BOARD_SIZE + tile.x) * 12
    self.update_tile_color(tile, i)

  def initializeGL(self):
    self.is_gl_initialized = 1
    glEnable(GL_LIGHTING)
    glEnable(GL_LIGHT0)
#    glEnable(GL_LIGHT1)
    glEnable(GL_NORMALIZE)
    glEnable(GL_DEPTH_TEST)
    if 1:
      glLightfv(GL_LIGHT0, GL_AMBIENT, (0.5, 0.5, 0.5, 1.0))
    else:
      glLightModelfv(GL_LIGHT_MODEL_AMBIENT, (0.9, 0.9, 0.9, 1.0))
      glLightfv(GL_LIGHT0, GL_DIFFUSE, (1.0, 1.0, 1.0, 1.0))      # Color
      glLightfv(GL_LIGHT0, GL_POSITION, (4.0, 0.0, -80.0, 1.0))   # Position
      glLightfv(GL_LIGHT1, GL_DIFFUSE, (0.5, 0.2, 0.2, 1.0))      # Color
      glLightfv(GL_LIGHT1, GL_POSITION, (1.0, 0.5, 0.5, 0.0))     # Direction
    glClearColor(0.0, 1.0, 0.0, 1.0)
    glEnableClientState(GL_VERTEX_ARRAY)
    glEnableClientState(GL_COLOR_ARRAY)
    glVertexPointer(2, GL_SHORT, 0, self.board_vertices)
    glColorPointer(3, GL_UNSIGNED_BYTE, 0, self.board_colors)
    self.update_model()

  def paintGL(self):
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
    glPushMatrix()
    glLoadIdentity()
    glTranslated(self.camera.x, self.camera.y, self.camera.z)
    glRotated(self.rotation.x / 16.0, 1.0, 0.0, 0.0)
    glRotated(self.rotation.y / 16.0, 0.0, 1.0, 0.0)
    glRotated(self.rotation.z / 16.0, 0.0, 0.0, 1.0)
    glTranslated(-Board.BOARD_SIZE/2, -Board.BOARD_SIZE/2, 0)
#    glColorPointer(3, GL_UNSIGNED_BYTE, 0, self.board_colors)
    glDrawArrays(GL_QUADS, 0, 4 * Board.BOARD_SIZE * Board.BOARD_SIZE)
    glPopMatrix()

  def resizeGL(self, width, height):
    side = float(min(width, height))
    if side <= 0.01: return
    self.side = side
    glViewport(0, 0, width, height)
    glMatrixMode(GL_PROJECTION)
    glLoadIdentity()
    xf = float(width) / side
    yf = float(height) / side
    glFrustum(-xf * self.zoom, xf * self.zoom, -yf * self.zoom, yf * self.zoom, 2.0, 100000.0)
    glMatrixMode(GL_MODELVIEW)

class LoadThread(QtCore.QThread):
  ready = QtCore.Signal()

  def run(self):
    self.board = Board()
    self.board.load(self.game_file.path)
    self.ready.emit()

class Toolbar(QtGui.QWidget):
  def __init__(self, parent = None):
    QtGui.QWidget.__init__(self, parent)
    self.games = []
    self.default_game_path = '~/play/ants/dist'
    self.default_game_path = '~/dev/python/PAnts/games'
    self.loader = LoadThread()
    self.loader.ready.connect(self.on_game_loaded)
    self.stack = QtGui.QStackedLayout()
    # Game Path selection
    self.gp = QtGui.QWidget()
    self.gp.hbox = Gui.new_hbox(self.gp, 1, 6, -1)
    self.gp.hbox.setContentsMargins(4, 1, 1, 1)
    self.gp.folder = Gui.new_LineEdit(self.gp.hbox, '', 300)
    self.gp.folder.setFocusPolicy(QtCore.Qt.NoFocus)
    self.gp.browse = Gui.new_PushButton(self.gp.hbox, " Browse ", self.on_browse_folder, 1)
    self.gp.status = Gui.new_Label(self.gp.hbox, 'Select folder where game replays reside', -1)
    self.gp.status.setPalette(Gui.GRAY_TEXT)
    self.gp.next = Gui.new_PushButton(self.gp.hbox, "Next", self.switch_to_gs, 1)
    self.gp.next.setEnabled(False)
    # Game Selection
    self.gs = QtGui.QWidget()
    self.gs.hbox = Gui.new_hbox(self.gs, 1, 6, -1)
    self.gs.back = Gui.new_PushButton(self.gs.hbox, "Back", self.switch_to_gp, 1)
    self.gs.combo = Gui.new_Combo(self.gs.hbox, None, self.on_game_selected, 1)
    self.gs.status = Gui.new_Label(self.gs.hbox, 'Please select a game', -1)
    self.gs.status.setPalette(Gui.GRAY_TEXT)
    self.gs.next = Gui.new_PushButton(self.gs.hbox, "Next", self.switch_to_gr, 1)
    self.gs.next.setEnabled(False)
    # Game running
    self.gr = QtGui.QWidget()
    self.gr.hbox = Gui.new_hbox(self.gr, 1, 4, -1)
    self.gr.back = Gui.new_PushButton(self.gr.hbox, "Back", self.switch_to_gs, 1)
    self.gr.sep1 = Gui.new_Label(self.gr.hbox, '', 16)
    self.gr.stop = Gui.new_PushButton(self.gr.hbox, "Stop", self.on_stop, 1)
    self.gr.pause = Gui.new_PushButton(self.gr.hbox, "Pause", self.on_pause, 1)
    self.gr.sep2 = Gui.new_Label(self.gr.hbox, '', 8)
    self.gr.play = Gui.new_PushButton(self.gr.hbox, "Play", self.on_play, 1)
    self.gr.sep3 = Gui.new_Label(self.gr.hbox, '', 4)
    self.gr.speed = Gui.new_Label(self.gr.hbox, 'Speed:', 1)
    self.gr.speed_normal = Gui.new_RadioButton(self.gr.hbox, "1x", self.on_speed_normal, 1)
    self.gr.speed_double = Gui.new_RadioButton(self.gr.hbox, "2x", self.on_speed_double, 1)
    self.gr.speed_quad = Gui.new_RadioButton(self.gr.hbox, "4x", self.on_speed_quad, 1)
    self.gr.speed_normal.setChecked(True)
    self.gr.status = Gui.new_Label(self.gr.hbox, '', -1)
    # timer
    self.timer = QtCore.QTimer(self)
    self.timer.timeout.connect(self.run_turn)
    self.timer.setInterval(SPEED_NORMAL_INTERVAL)
    # Stack
    self.stack.addWidget(self.gp)
    self.stack.addWidget(self.gs)
    self.stack.addWidget(self.gr)
    self.stack.setCurrentIndex(0)
    self.setLayout(self.stack)
    if os.path.isdir(Gui.resolved_path(self.default_game_path)):
      self.set_game_path(self.default_game_path)
    else:
      self.default_game_path = '~'

  def run_turn(self):
    self.main_window.board_view.run_turn()

  def activate_play_buttons(self):
    if self.main_window.board_view.board.problem:
      self.gr.stop.setEnabled(False)
      self.gr.pause.setEnabled(False)
      self.gr.play.setEnabled(False)
    else:
      self.gr.stop.setEnabled(self.timer.isActive())
      self.gr.pause.setEnabled(self.timer.isActive())
      self.gr.play.setEnabled(not self.timer.isActive())

  def on_stop(self):
    self.timer.stop()
    self.activate_play_buttons()

  def on_pause(self):
    self.timer.stop()
    self.activate_play_buttons()

  def on_play(self):
    self.timer.start()
    self.activate_play_buttons()

  def on_speed_normal(self):
    self.timer.setInterval(SPEED_NORMAL_INTERVAL)

  def on_speed_double(self):
    self.timer.setInterval(SPEED_NORMAL_INTERVAL / 2)

  def on_speed_quad(self):
    self.timer.setInterval(SPEED_NORMAL_INTERVAL / 4)

  def on_game_loaded(self):
    self.gs.combo.setEnabled(True)
    b = self.loader.board
    self.main_window.board_view.set_board (b)
    if b.problem:
      self.gs.status.setText(b.problem)
      self.gs.status.setPalette(Gui.RED_TEXT)
    else:
      self.gs.status.setText("Game loaded")
      self.gs.status.setPalette(Gui.BLUE_TEXT)
    self.gs.next.setEnabled(b.problem == None)

  def on_game_selected(self, item):
    self.gs.next.setEnabled(False)
    if item:
      gf = self.games[item-1]
      if self.main_window.board_view.board and gf.path == self.main_window.board_view.board.path:
        return
      self.gs.combo.setEnabled(False)
      self.gs.status.setText("Loading ...")
      self.gs.status.setPalette(Gui.BLUE_TEXT)
      self.loader.game_file = gf
      self.loader.run()
    else:
      b = Board()
      self.main_window.board_view.set_board (b)
      self.gs.status.setText("Please select a game")
      self.gs.status.setPalette(Gui.GRAY_TEXT)

  def switch_to_gp(self):
#    self.animate_pos(self.gp, 1)
#    self.animate_pos(self.gs, -1)
    self.stack.setCurrentIndex(0)

  def switch_to_gs(self):
#    self.animate_pos(self.gp, -1)
#    self.animate_pos(self.gs, 1)
    self.stack.setCurrentIndex(1)

  def switch_to_gr(self):
    self.activate_play_buttons()
    self.stack.setCurrentIndex(2)

  def animate_pos(self, widget, direction):
    sp = self.pos()
    ep = sp
    if direction == 1:
      widget.move(sp.x() + self.width(), sp.y())
      sp = QtCore.QPoint(sp.x(), sp.y())
    else:
      ep = QtCore.QPoint(sp.x() - self.width(), sp.y())
    animation = QtCore.QPropertyAnimation(widget, "pos")
    animation.setDuration(1750)
    animation.setEasingCurve(QtCore.QEasingCurve.InBack)
#    animation.setStartValue(sp)
    animation.setEndValue(ep)
    animation.start()

  def on_browse_folder(self):
    dialog = QtGui.QFileDialog()
    dialog.setFileMode(QtGui.QFileDialog.Directory)
    dialog.setOption(QtGui.QFileDialog.ShowDirsOnly, True)
    path = dialog.getExistingDirectory(self, 'Directory', Gui.resolved_path(self.default_game_path))
    if path:
      self.set_game_path(path)

  def set_game_path(self, path):
    self.game_path = path
    self.gp.folder.setText(path)
    self.games = []
    if os.path.isdir(Gui.resolved_path(path)):
      for dirpath, dirnames, filenames in os.walk(Gui.resolved_path(path)):
        dirnames[:] = []
        for fname in filenames:
          fpath = os.path.join(dirpath, fname)
          mgamefilename = re.compile('(.+)Vs(.+)\.out$')
          m = mgamefilename.match(fname)
          if m:
            self.games.append(GameFile(m.group(1), m.group(2), fpath))
      if len(self.games):
        self.gp.status.setText("%d games found" % len(self.games))
        self.gp.status.setPalette(Gui.BLUE_TEXT)
      else:
        self.gp.status.setText("No games found in '%s'" % path)
        self.gp.status.setPalette(Gui.RED_TEXT)
    else:
      self.gp.status.setText("No folder '%s'" % path)
      self.gp.status.setPalette(Gui.RED_TEXT)
    self.gs.combo.clear()
    self.gs.combo.addItem('Select a game')
    for g in self.games:
      self.gs.combo.addItem(str(g))
    self.gp.next.setEnabled(len(self.games) > 0)
    self.gs.next.setEnabled(False)

class MainWindow(QtGui.QMainWindow):
  def __init__(self, parent = None):
    QtGui.QMainWindow.__init__(self, parent)
    widget = QtGui.QWidget()
    vbox = QtGui.QVBoxLayout()
    vbox.setContentsMargins(1, 1, 1, 1)
    vbox.setSpacing(1)
    self.toolbar = Toolbar()
    self.toolbar.main_window = self
    self.board_view = BoardView()
    vbox.addWidget(self.toolbar)
    vbox.addWidget(self.board_view)
    widget.setLayout(vbox)
    self.setCentralWidget(widget)
    self.unifiedTitleAndToolBarOnMac = True
    self.setWindowTitle(self.tr("PAnts"))


if __name__ == '__main__':
    app = QtGui.QApplication(sys.argv)
    window = MainWindow()
    window.show()
    sys.exit(app.exec_())
