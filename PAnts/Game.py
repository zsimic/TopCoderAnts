'''
Created on Nov 25, 2011

@author: zoran
'''

import os
import re

class GameFile(object):
  def __init__(self, p1, p2, seq, path):
    if seq % 2:
      self.p1 = p1
      self.p2 = p2
    else:
      self.p1 = p2
      self.p2 = p1
    self.seq = seq
    self.path = path

  def __str__(self):
    return "%s vs %s [Game %d]" % (self.p1, self.p2, self.seq)

class Ant(object):
  def __init__(self, uid):
    self.uid = uid            # ant's unique id (1 to 100)

  def initialize(self, team, x, y):
    self.team = team          # ant's team
    team.ants.append(self)
    team.nest.add_ant(self)
    team.board.ants.append(self)
    self.board = team.board   # reference to parent board object
    self.alive = 1            # is ant still alive?
    self.food = 0             # does ant carry food currently?
    self.turn = 0             # ant's turn
    self.x = x                # ant's X,Y position on board
    self.y = y

  def tile(self):
    return self.board.tile(self.x, self.y)

class Team(object):
  def __init__(self, uid, board, nest):
    self.board = board    # reference to parent board object
    self.uid = uid        # team id (1 or 2)
    self.ants = []        # all ants in the team
    self.food = 0         # amount of food gathered
    self.nest = nest      # object of type Tile()
    assert nest.nest == None
    self.nest.nest = self
    if uid == 1:
      self.rgb = (255, 0, 0)
    else:
      self.rgb = (0, 0, 255)
    print "Team %d: %d %d" % (uid, nest.x, nest.y)

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
    return self.ant.board.tile(self.ant.x + self.direction.dx, self.ant.y + self.direction.dy)

  def drop_food(self, view):
    target_tile = self.target_tile()
    if not target_tile.passable:
      print 'ant %d (%d %d) drops food to non-passable tile (%d %d)' % (self.ant.uid, self.ant.x, self.ant.y, target_tile.x, target_tile.y)
    if not self.ant.food:
      print 'ant %d (%d %d) drops imaginary food' % (self.ant.uid, self.ant.x, self.ant.y)
    target_tile.put_food()
    self.ant.food = 0
    view.update_tile(target_tile)
    view.update_tile(self.ant.tile())

  def get_food(self, view):
    target_tile = self.target_tile()
    target_tile.take_food()
    if self.ant.food:
      print 'ant %d (%d %d) takes food while already carrying some' % (self.ant.uid, self.ant.x, self.ant.y)
    self.ant.food = 1
    view.update_tile(target_tile)
    view.update_tile(self.ant.tile())
    if target_tile.food < 0:
      print 'ant %d (%d %d) takes non-existing food from tile (%d %d)' % (self.ant.uid, self.ant.x, self.ant.y, target_tile.x, target_tile.y)

  def move(self, view):
    target_tile = self.target_tile()
    if not target_tile.passable:
      print 'ant %d (%d %d) goes to non-passable tile (%d %d)' % (self.ant.uid, self.ant.x, self.ant.y, target_tile.x, target_tile.y)
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
  obstacle_color = (0, 0, 0)
  board_color = (255, 255, 255)

  def __init__(self, x, y):
    self.x = x
    self.y = y
    self.passable = 0
    self.food = 0
    self.nest = None          # will hold an object of type Team() object when the tile is a nest
    self.visited = [0, 0, 0]  # number of times tile was visited by corresponding team (only indices 1 and 2 are used)
    self.ants = None
    self.scent = None
    self.nest_nearby = None   # reference to nest tile, if we are close enough to the nest

  def take_food(self):
    self.food -= 1

  def put_food(self):
    self.food += 1

  def add_ant(self, ant):
    self.ants.append(ant)

  def remove_ant(self, ant):
    self.ants.remove(ant)

  def rgb(self):
    if not self.passable:
      return Tile.obstacle_color
    elif self.ants:
      return self.ants[0].team.rgb
    elif self.nest_nearby:
      r, g, b = self.nest_nearby.nest.rgb
      g = 50 + max(abs(self.x - self.nest_nearby.x), abs(self.y - self.nest_nearby.y)) * 30
      if r == 0: r = g
      if b == 0: b = g
      return (r, g, b)
    elif self.food:
      g = 255
      r = b = 50 - min(self.food, 50)
      return (r, g, b)
    else:
      return Tile.board_color

class Board(object):
  BOARD_SIZE = 512

  def __init__(self):
    self.actions = []
    self.action_index = 0
    self.ants = []
    for i in range(100):
      self.ants.append(Ant(i+1))
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
          t.ants = []
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
      while line:
        m = mstarts.match(line)
        if m:
          uid = int(m.group(1))
          x = int(m.group(2))
          y = int(m.group(3))
          if lastx != x or lasty != y:
            lastx, lasty = (x, y)
            if self.nests[0].x == x and self.nests[0].y == y:
              nest_tile = self.nests[0]
            else:
              nest_tile = self.nests[1]
            if team:
              team = Team(team.uid + 1, self, nest_tile)
            else:
              team = Team(1, self, nest_tile)
          self.ant(uid).initialize(team, x, y)
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
        self.tile(nest_tile.x+i-mid, nest_tile.y+j-mid).nest_nearby = nest_tile

  def tile(self, x, y):
    return self.tiles[y][x]

  def new_row(self, y):
    row = []
    for i in range(Board.BOARD_SIZE):
      row.append(Tile(i, y))
    return row

