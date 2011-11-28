#!/usr/bin/env python
# -*- coding: utf-8 -*-
# about.py - display about box with info on platform etc.

'''
Created on Nov 23, 2011

@author: zoran
'''

import sys

from Game import Board

from PySide import QtCore, QtGui, QtOpenGL

class BoardView(QtGui.QWidget):

  def __init__(self, parent=None):
    super(BoardView, self).__init__(parent)
    self.zoom = 1.0
    self.xRot = -80
    self.yRot = 0
    self.zRot = 0
    self.turn = 0
    self.board_color = QtGui.QColor(0xFFFFFF)
    self.obstacle_color = QtGui.QColor(0x202020)
    board = Board()
    board.load('data/game2.txt')
    self.set_board(board)
    self.setFocusPolicy(QtCore.Qt.StrongFocus)
    self.sqW = float(1)
    self.sqH = float(1)

  def set_board(self, board):
    self.board = board
    self.update()

  def keyPressEvent(self, event):
    key = event.key()
    if key == QtCore.Qt.Key_P:
      pass

  def wheelEvent(self, event):
    d = event.delta()

  def mousePressEvent(self, event):
    self.lastPos = event.pos()

  def mouseMoveEvent(self, event):
    dx = event.x() - self.lastPos.x()

  def resizeEvent(self, event):
    self.sqW = float(event.size().width()) / float(Board.BOARD_SIZE)
    self.sqH = float(event.size().height()) / float(Board.BOARD_SIZE)
    self.update()

  def paintEvent(self, event):
    painter = QtGui.QPainter(self)
    rect = self.contentsRect()
    boardTop = rect.bottom() - Board.BOARD_SIZE * self.sqH
    for row in self.board.tiles:
      for t in row:
        self.draw_tile(painter, t)

  def draw_tile(self, painter, t):
    color = self.board_color
    if not t.passable:
      color = self.obstacle_color
#    painter.setPen(color)
    px = t.x * self.sqW
    py = t.y * self.sqW
    painter.fillRect(px, py, px + self.sqW, py + self.sqH, color)

class MainWindow(QtGui.QWidget):

  def __init__(self):
    super(MainWindow, self).__init__()
    vbox = QtGui.QVBoxLayout()
    # Toolbar
    hbox = QtGui.QHBoxLayout()
    vbox.addChildLayout(hbox)
    # Board view
    self.board_view = BoardView()
    self.board_view.setMinimumSize(Board.BOARD_SIZE, Board.BOARD_SIZE)
    vbox.addWidget(self.board_view)
    self.setLayout(vbox)
#    timer = QtCore.QTimer(self)
#    timer.timeout.connect(self.run_turn)
#    timer.start(100)
    self.setWindowTitle("PAnts")

def main():
  app = QtGui.QApplication(sys.argv)
  window = MainWindow()
  window.show()
  sys.exit(app.exec_())


if __name__ == '__main__':
  main()
