# Sokoban Proyect

Java implementation of the Sokoban game. The application can be used with a
Swing graphical interface or in console mode.

## Requirements

- Java 11 or newer
- Maven 3.x

## Build, Run, and Test

Compile and run the tests:

```sh
mvn test
```

Run the graphical version:

```sh
mvn exec:java
```

Run the console version:

```sh
mvn exec:java -Dexec.args="--console"
```

Build an executable JAR:

```sh
mvn package
java -jar target/sokoban-1.0-SNAPSHOT.jar
```

To run the JAR in console mode:

```sh
java -jar target/sokoban-1.0-SNAPSHOT.jar --console
```

## Files Read and Written by the Application

### Level Files

The game reads level files from the application resources. In the source tree,
the bundled levels are stored in:

```text
src/main/resources/
```

Current bundled levels:

- `level 1.txt`
- `level 2.txt`
- `level 3.txt`

After building, Maven copies these resources to the runtime classpath under
`target/classes/`. The controller loads them by name as `level N.txt`, starting
at `level 1.txt` and advancing to the next number when a level is solved.

### Saved Games

The graphical interface can write and read saved games with the `.sok`
extension. The user chooses the storage folder through the Swing file chooser.
If the selected save filename does not end in `.sok`, the application appends
the extension automatically.

Saved games are binary Java serialized files. They store:

- current level number
- current board state
- current level score
- global score
- undo history

Saved-game files are not intended to be edited manually. They should be opened
with the same application version that created them.

The console mode does not provide save/load commands.

## Level File Format

Level files are UTF-8 text files.

```text
<level name>
<rows> <columns>
<row 1>
<row 2>
...
<row N>
```

Example:

```text
Simple
3 5
+++++
+W#*+
+++++
```

The second line contains the number of rows and columns. The file must then
contain exactly that number of board rows, and each board row must have exactly
the declared number of columns. Spaces are meaningful board cells, including
leading and trailing spaces.

Board symbols:

| Symbol | Meaning |
| --- | --- |
| `+` | Wall |
| `W` | Player / warehouse man |
| `#` | Box |
| `*` | Goal position |
| space | Empty floor |

Validation rules:

- each level must have exactly one player
- each level must have at least one box and one goal
- the number of boxes must equal the number of goals
- no unsupported characters are allowed
- no extra or missing board rows are allowed

## Controls

### Graphical Mode

Movement:

- Arrow keys: move up, down, left, or right
- `W`, `A`, `S`, `D`: move up, left, down, or right

Other keyboard controls:

- `U`: undo the last movement
- `R`: restart the current level

Buttons:

- `Undo`: undo the last movement
- `Restart`: restart the current level

Game menu:

- `New Game`: start again from level 1 and reset scores
- `Restart Current Level`: reload the current level and reset its score
- `Undo Last Movement`: undo the last movement
- `Save Game`: save a `.sok` file
- `Open Saved Game`: load a `.sok` file
- `Close Application`: exit the application

### Console Mode

Enter one command per line:

| Command | Action |
| --- | --- |
| `w` / `W` | Move up |
| `a` / `A` | Move left |
| `s` / `S` | Move down |
| `d` / `D` | Move right |
| `u` / `U` | Undo the last movement |
| `r` / `R` | Restart the current level |

## Scoring and Game Rules

The level score is the number of successful moves made in the current level.
Blocked moves do not increase the score. Undo decreases the current level score
by one, down to zero.

The global score is the sum of completed level scores. A level is solved when
all boxes are on goal positions. When the graphical version solves a level, it
automatically loads the next one. If there is no next level, the game is
completed and the total score is shown.

## Project Structure

```text
src/main/java/es/upm/pproject/sokoban/
  App.java                         Application entry point
  controller/GameController.java   Game flow, input, save/load
  model/dto/                       Board and game object classes
  model/services/                  Level parsing and board logic
  view/                            Swing and console views

src/main/resources/                Bundled level files
src/test/java/                     JUnit tests
deliverables/                      Project deliverables
```

## Authors

- Gjorgi Andonovski
- Franco Aldair Sosa Martinez
- Da Wei Wu Chen
- Daniel Rozano Herrera
