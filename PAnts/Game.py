'''
Created on Nov 25, 2011

@author: zoran
'''

import os
import re

class GameFile(object):
  def __init__(self, fname, p1, p2, seq, path):
    self.file_name = fname
    self.sort_key = '%s %s %2d' % (p1, p2, seq)
    if seq % 2:
      self.p1 = p1
      self.p2 = p2
    else:
      self.p1 = p2
      self.p2 = p1
    self.seq = seq
    self.path = path

  def __repr__(self):
    return self.file_name

  def __str__(self):
    return "%s vs %s [Game %d]" % (self.p1, self.p2, self.seq)

class Ant(object):
  def __init__(self, uid, team, x, y):
    self.uid = uid            # ant's unique id (1 to 100)
    self.team = team          # ant's team
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
    self.nest = nest      # object of type Tile()
    assert nest.nest == None
    self.nest.nest = self
    if uid == 1:
      self.rgb = (255, 0, 0)
    else:
      self.rgb = (0, 0, 255)

  def food(self):
    return self.nest.food

class Direction(object):
  def __init__(self, dx, dy):
    self.dx = dx
    self.dy = dy

class Directions(object):
  here = Direction(0, 0)
  northeast = Direction(-1, 1)
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
  maction = re.compile('(\*dies\*|Writes|Drops food|Gets food from|Moves|Says) ?(.*)')

  def __init__(self, board, representation):
    self.valid = 0
    m = Action.mant.match(representation)
    if m:
      self.ant = board.ant(int(m.group(1)))
      rest = m.group(2)
      m = Action.maction.match(rest)
      if m:
        self.name = m.group(1)
        self.executor = Actions.by_name[self.name]
        self.value = m.group(2)
        self.valid = 1

  def target_tile(self):
    direction = Directions.by_name[self.value]
    return self.ant.board.tile(self.ant.x + direction.dx, self.ant.y + direction.dy)

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

  def dies(self, view):
    current_tile = self.ant.tile()
    if self.ant.food:
      current_tile.put_food()
    current_tile.remove_ant(self.ant)
    self.ant.team.ants.remove(self.ant)
    view.update_tile(current_tile)

  def write(self, view):
    current_tile = self.ant.tile()
    current_tile.scent = self.value
    view.update_tile(current_tile)

class Actions(object):
  by_name = {
    'Drops food': Action.drop_food,
    'Gets food from': Action.get_food,
    'Moves': Action.move,
    'Says': Action.say,
    '*dies*': Action.dies,
    'Writes': Action.write
  }

class Tile(object):
  obstacle_color = (0, 0, 0)
  board_color = (255, 255, 255)
  fog_color = (60, 60, 60)

  def __init__(self, x, y):
    self.x = x
    self.y = y
    self.passable = 0
    self.food = 0
    self.nest = None          # will hold an object of type Team() object when the tile is a nest
    self.visited = [0, 0, 0]  # number of times tile was visited by corresponding team (0: total, 1: team 1, 2: team 2)
    self.ants = [0, 0, 0]     # number of ants on tile (total + by team)
    self.scent = None
    self.nest_nearby = None   # reference to nest tile, if we are close enough to the nest

  def __str__(self):
    return "Tile %d,%d pass=%d food=%d" % (self.x, self.y, self.passable, self.food)

  def take_food(self):
    self.food -= 1

  def put_food(self):
    self.food += 1

  def add_ant(self, ant):
    self.ants[0] += 1
    self.ants[ant.team.uid] += 1
    self.visited[0] += 1
    self.visited[ant.team.uid] += 1

  def remove_ant(self, ant):
    self.ants[0] -= 1
    self.ants[ant.team.uid] -= 1

  def rgb(self, fog):
    if self.passable:
      if self.food:
        g = 255
        r = b = 50 - min(self.food, 50)
        return (r, g, b)
      elif self.ants[0]:
        r = self.ants[1]
        b = self.ants[2]
        if r: r = 255 - r
        if b: b = 255 - b
        return (r, 0, b)
      elif self.nest_nearby:
          r, g, b = self.nest_nearby.nest.rgb
          g = 50 + max(abs(self.x - self.nest_nearby.x), abs(self.y - self.nest_nearby.y)) * 30
          if r == 0: r = g
          if b == 0: b = g
          return (r, g, b)
      elif self.visited[0]:
        r = self.visited[1]
        b = self.visited[2]
        g = 255
        if r and b:
          g = 255 - min(r + b, 20) * 5
          r = 200 + min(r, 55)
          b = 200 + min(b, 55)
        elif r:
          g = b = 255 - min(r, 20) * 5
          r = 255
        else:
          g = r = 255 - min(b, 20) * 5
          b = 255
        return (r, g, b)
      elif fog:
        return Tile.fog_color
      else:
        return Tile.board_color
    else:
      return Tile.obstacle_color

class Board(object):
  BOARD_SIZE = 512

  def __init__(self):
    self.actions = []
    self.ants = dict()
    self.nests = []
    self.tiles = []
    self.teams = []
    self.turn = 0
    self.played = 0       # Number of actions played so far
    self.total = 0        # Total number of actions
    self.path = None
    self.problem = 'Please select a game replay'

  def ant(self, uid):
    return self.ants[uid]

  def next_action(self):
    i = self.played
    if i >= len(self.actions):
      return None
    self.played += 1
    return self.actions[i]

  def run_turn(self, view):
    self.turn += 1
    act = None
    while True:
      act = self.next_action()
      if act:
        if act.ant.turn >= self.turn:
          self.played -= 1
          break
        else:
          act.ant.turn = self.turn
          act.executor(act, view)
#          if act.ant.uid == 19:
#            print "%d: (%d,%d,%d) %s %s" % (act.ant.uid, self.turn, act.ant.x, act.ant.y, act.name, act.value)
#            print act.ant.tile()
      else:
        view.game_finished()
        break

  def load(self, path, progress):
    self.path = path
    self.problem = self.effective_load(progress)

  def effective_load(self, progress):
    self.actions = []
    self.ants = dict()
    self.nests = []
    self.teams = []
    self.tiles = []
    self.played = 0
    self.total = 0
    self.turn = 0
    for i in range(Board.BOARD_SIZE):
      self.tiles.append(self.new_row(i))
    linenumber = 0
    loaded = 0
    mstate = re.compile('\(([0-9]+),([0-9]+)\) *(.*)')
    mfood = re.compile('Food: ([0-9]+)')
    mstarts = re.compile('([0-9]+): Starts at ([0-9]+),([0-9]+)')
    if not os.path.isfile(self.path):
      return "No file '%s'" % self.path
    total = os.path.getsize(self.path)
    with open(self.path, 'r') as fh:
      line = fh.readline()
      linenumber += 1
      loaded += len(line)
      while line:
        if linenumber % 10000 == 0: progress.emit(loaded, total)
        m = mstate.match(line)
        if m:
          x = int(m.group(1))
          y = int(m.group(2))
          s = m.group(3)
          t = self.tile(x, y)
          t.passable = 1
          t.ants = [0, 0, 0]
          if len(s):
            m = mfood.match(s)
            if m:
              t.food = int(m.group(1))
            elif s == 'Nest':
              self.nests.append(t)
            else:
              return "Malformed state line %d: '%s'" % (linenumber, s)
        else:
          break
        line = fh.readline()
        linenumber += 1
        loaded += len(line)
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
            self.teams.append(team)
          ant = Ant(uid, team, x, y)
          team.ants.append(ant)
          team.nest.add_ant(ant)
          self.ants[uid] = ant
        else:
          break
        line = fh.readline()
        linenumber += 1
        loaded += len(line)
      if len(self.teams) != 2:
        return "Only %d team effectively defined (bug in replay file generation)" % len(self.teams)
      self.expand_nest(self.nests[0])
      self.expand_nest(self.nests[1])
      while line:
        if linenumber % 10000 == 0: progress.emit(loaded, total)
        act = Action(self, line)
        if act.valid:
          self.actions.append(act)
        else:
          return "Invalid action line %d: '%s'" % (linenumber, line)
        line = fh.readline()
        linenumber += 1
        loaded += len(line)
    self.total = len(self.actions)
    return None

  def expand_nest(self, nest_tile):
    if not nest_tile: return
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

