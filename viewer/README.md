The viewer is a small python script that allows to replay a game and see the ants move around the board collecting their food.
It's written in python, using **PySide** (a python wrapper of **Qt**), it renders the board using OpenGL.

- It shows the 512x512 game board
- Game replays are searched for in ~/play/ants/dist by default, but you can browse to any location, the specified folder is searched recursively for files of the form <player>Vs<player>.<number>
- Select the game you want to view from the dropdown list (which should list all found games under above specified folder)
- Push the 'Play' button and see the ants in action
- The progress bar shows how much of the game has elapsed, with an approximate ETA until the end of the game (depends on the play speed selected)
- Obstacles are black, unexplored cells are gray, explored ones are white (one can turn this off by unchecking the 'Fog' checkbox in the upper right of the window)
- Team 1 in the replay file is red, Team 2 is blue
- Cells where ants left a "scent" are slightly colored (bright yellowish/blueish cells depending on the team that left the scent)
- Cells containing food are green
- Nests are highlighted in a way that should make it easy to see them on the board
- Each board cell is 2x2 pixels originally, and
- You can move the board around by left-clicking on the board and dragging around (or use the arrow keys)
- You can zoom in/out using the mouse wheel
- You can rotate the board by holding SHIFT, then right or left-click and drag around
- You can re-center the board by hitting SPACE
- You can type 'P' to switch to Qt's "plastique" look
- You can type 'M' to switch to Qt's "mac" look
- You can play/pause/step the game, but can't "rewind" unfortunately (you can always reload the game and start over...)

To run the viewer:
------------------
- install [Qt](http://qt.nokia.com/downloads/qt-for-open-source-cpp-development-on-mac-os-x/) (pick latest binary package, currently 4.7.4)
- install [PySide](http://developer.qt.nokia.com/wiki/PySide_Binaries_MacOSX) (pick the one corresponding to your python version **...py26...** or **...py27...**)
- make sure you're not running in a python virtualenv for the commands below, or if you are be aware that the below commands are going to install the modules in your virtualenv
- **alternative way to get PySide**: `sudo env ARCHFLAGS="-arch i386 -arch x86_64" easy_install PySide` (this one unfortunately doesn't always work on OSX)
- install the following python modules:

		sudo env ARCHFLAGS="-arch i386 -arch x86_64" easy_install PyOpenGL
		sudo env ARCHFLAGS="-arch i386 -arch x86_64" easy_install PyOpenGL-accelerate
		sudo env ARCHFLAGS="-arch i386 -arch x86_64" easy_install scipy
		sudo env ARCHFLAGS="-arch i386 -arch x86_64" easy_install numpy

- you should be now able to run the **AntsViewer.py** script:

		python AntsViewer.py

Example:
--------

![Viewer example](https://github.com/zsimic/TopCoderAnts/raw/master/viewer/viewer.png)
