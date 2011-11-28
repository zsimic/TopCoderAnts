'''
Created on Nov 25, 2011

@author: zoran
'''

import os
import re

class GameFile(object):
  def __init__(self, p1, p2, path):
    self.p1 = p1
    self.p2 = p2
    self.path = path

  def __str__(self):
    return "%s vs %s" % (self.p1, self.p2)

class Ant(object):
  def __init__(self, uid, board):
    self.board = board
    self.uid = uid
    self.alive = 1
    self.food = 0
    self.team = None
    self.turn = 0
    self.x = 0
    self.y = 0

  def tile(self):
    return self.board.tile(self.x, self.y)

class Team(object):
  def __init__(self, uid):
    self.uid = uid
    self.ants = []
    self.food = 0
    self.nest = None    # will hold an object of type Tile()

class Direction(object):
  def __init__(self, dx, dy):
    self.dx = dx
    self.dy = dy

class Directions(object):
  here = Direction(0, 0)
  northeast = Direction(0, 0)
  east = Direction(0, 1)
  southeast = Direction(1, 1)
  south = Direction(1, 0)
  southwest = Direction(1, -1)
  west = Direction(0, -1)
  northwest = Direction(-1, -1)
  north = Direction(-1, 0)

  by_name = {
    'here': here,
    'northeast': northeast,
    'east': east,
    'southeast': southeast,
    'south': south,
    'southwest': southwest,
    'west': west,
    'northwest': northwest,
    'north': north
  }

class Action(object):
  mant = re.compile('([0-9]+): (.+)')
  mwrites = re.compile('Writes ([0-9]+)')
  mdirected = re.compile('(Drops food|Gets food from|Moves) (.+)')

  def __init__(self, board, representation):
    self.valid = 0
    m = Action.mant.match(representation)
    if m:
      self.ant = board.ant(int(m.group(1)))
      rest = m.group(2)
      m = Action.mwrites.match(rest)
      if m:
        self.executor = self.write
        self.value = m.group(1)
        self.valid = 1
      else:
        m = Action.mdirected.match(rest)
        if m:
          actname = m.group(1)
          if actname == 'Drops food': self.executor = self.drop_food
          elif actname == 'Gets food from': self.executor = self.get_food
          elif actname == 'Moves': self.executor = self.move
          elif actname == 'Says': self.executor = self.say
          elif actname == 'Writes': self.executor = self.write
          self.direction = Directions.by_name[m.group(2)]
          self.valid = self.executor

  def target_tile(self):
    return self.ant.board.tile(self.ant.x - self.direction.dx, self.ant.y - self.direction.dy)

  def drop_food(self, view):
    target_tile = self.target_tile()
    target_tile.put_food()
    self.ant.food = 0
    view.update_tile(target_tile)

  def get_food(self, view):
    target_tile = self.target_tile()
    target_tile.take_food()
    self.ant.food = 1
    view.update_tile(target_tile)

  def move(self, view):
    target_tile = self.target_tile()
    current_tile = self.ant.tile()
    current_tile.remove_ant(self.ant)
    self.ant.x = target_tile.x
    self.ant.y = target_tile.y
    target_tile.add_ant(self.ant)
    view.update_tile(current_tile)
    view.update_tile(target_tile)

  def pass_turn(self, view):
    pass

  def say(self, view):
    pass

  def write(self, view):
    current_tile = self.ant.tile()
    current_tile.scent = self.value
    view.update_tile(current_tile)

class Tile(object):
  def __init__(self, x, y):
    self.x = x
    self.y = y
    self.passable = 0
    self.food = 0
    self.team = None    # will hold an object of type Team() object when the tile is a nest
    self.ants = []
    self.scent = None

  def take_food(self):
    self.food -= 1

  def put_food(self):
    self.food += 1

  def add_ant(self, ant):
    self.ants.append(ant)

  def remove_ant(self, ant):
    self.ants.remove(ant)

class Board(object):
  BOARD_SIZE = 512

  def __init__(self):
    self.actions = []
    self.action_index = 0
    self.ants = []
    for i in range(100):
      ant = Ant(i + 1, self)
      self.ants.append(ant)
    self.nests = []
    self.tiles = []
    self.turn = 0
    self.path = None
    self.problem = 'Please select a game replay to load'

  def ant(self, uid):
    return self.ants[uid-1]

  def next_action(self):
    i = self.action_index
    if i >= len(self.actions):
      return None
    self.action_index += 1
    return self.actions[i]

  def run_turn(self, view):
    self.turn += 1
    act = None
    while True:
      act = self.next_action()
      if act:
        if act.ant.turn >= self.turn:
          self.action_index -= 1
          break
        else:
          act.ant.turn = self.turn
          act.executor(view)
#          print '%d: %d %d' % (act.ant.uid, act.ant.x, act.ant.y)
      else:
        view.game_finished()
        break

  def load(self, path):
    self.path = path
    self.problem = self.effective_load()

  def effective_load(self):
    self.actions = []
    self.action_index = 0
    self.nests = []
    self.tiles = []
    self.turn = 0
    for i in range(Board.BOARD_SIZE):
      self.tiles.append(self.new_row(i))
    n = 0
    mstate = re.compile('\(([0-9]+),([0-9]+)\) *(.*)')
    mfood = re.compile('Food: ([0-9]+)')
    mstarts = re.compile('([0-9]+): Starts at ([0-9]+),([0-9]+)')
    if not os.path.isfile(self.path):
      return "No file '%s'" % self.path
    with open(self.path, 'r') as fh:
      line = fh.readline()
      n += 1
      while line:
        m = mstate.match(line)
        if m:
          x = int(m.group(1))
          y = int(m.group(2))
          s = m.group(3)
          t = self.tile(x, y)
          t.passable = 1
          if len(s):
            m = mfood.match(s)
            if m:
              t.food = int(m.group(1))
            elif s == 'Nest':
              self.nests.append(t)
            else:
              return "Malformed state line %d: '%s'" % (n, s)
        else:
          break
        line = fh.readline()
        n += 1
      if len(self.nests) != 2:
        return "%d nests found instead of 2" % len(self.nests)
      lastx, lasty = (0, 0)
      team = None
      nest_tile = None
      while line:
        m = mstarts.match(line)
        if m:
          uid = int(m.group(1))
          x = int(m.group(2))
          y = int(m.group(3))
          if lastx != x or lasty != y:
            lastx, lasty = (x, y)
            if team:
              team = Team(team.uid + 1)
            else:
              team = Team(1)
            if self.nests[0].x == x and self.nests[0].y == y:
              nest_tile = self.nests[0]
            else:
              nest_tile = self.nests[1]
            team.nest = nest_tile
            nest_tile.team = team
          ant = self.ant(uid)
          team.ants.append(ant)
          team.nest.add_ant(ant)
          ant.alive = 1
          ant.food = 0
          ant.team = team
          ant.turn = 0
          ant.x = x
          ant.y = y
        else:
          break
        line = fh.readline()
        n += 1
      self.expand_nest(self.nests[0])
      self.expand_nest(self.nests[1])
      while line:
        if n % 100000 == 0: print 'read %d' % (n / 100000)
        act = Action(self, line)
        if act.valid:
          self.actions.append(act)
        else:
          return "Invalid action line %d: '%s'" % (n, line)
        line = fh.readline()
        n += 1
    return None

  def expand_nest(self, nest_tile):
    mid = 3
    for i in range(2*mid+1):
      for j in range(2*mid+1):
        if i == mid and j == mid: continue
        self.tile(nest_tile.x+i-mid, nest_tile.y+j-mid).team = nest_tile.team

  def tile(self, x, y):
    return self.tiles[y][x]

  def new_row(self, y):
    row = []
    for i in range(Board.BOARD_SIZE):
      row.append(Tile(i, y))
    return row

