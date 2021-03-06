/*
 * 
 */
package bomber.AI;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import bomber.game.Block;
import bomber.game.Bomb;
import bomber.game.Constants;
import bomber.game.GameState;
import bomber.game.Player;

/**
 * Finding different routes and planning.
 *
 * @author Jokubas Liutkus
 * 
 */
public class RouteFinder {

  /** The game state. */
  private GameState state;

  /** The game AI. */
  private GameAI gameAI;

  /** The safety checker. */
  private SafetyChecker safetyCh;

  /**
   * Instantiates a new route finder.
   *
   * @param state
   *          the game state
   * @param gameAI
   *          the game AI
   * @param safetyCh
   *          the safety checker
   */
  public RouteFinder(GameState state, GameAI gameAI, SafetyChecker safetyCh) {
    this.state = state;
    this.gameAI = gameAI;
    this.safetyCh = safetyCh;
  }

  /**
   * Find route. Finds the fastest route to the certain place in the map using A* search algorithm
   * 
   * @param start
   *          the start position
   * @param goal
   *          the goal position
   * @return the sequence of moves
   */
  public LinkedList<AIActions> findRoute(Point start, Point goal) {
    PriorityQueue<Node> open = new PriorityQueue<>();
    HashSet<Node> closed = new HashSet<>();

    if (start == null || goal == null) {
      return null;
    }

    // heuristic value h
    int hValue = Math.abs(goal.x - start.x) + Math.abs(goal.y - start.y);
    Node startNode = new Node(0, hValue, null, start);

    // adding start node to the queue
    open.add(startNode);

    // finish node
    Node finish = null;

    // loop until the queue is not empty
    while (!open.isEmpty()) {

      // take the head of the queue
      Node temp = open.poll();

      // if the head is final position we finish
      if (temp.getCoord().equals(goal)) {
        finish = temp;
        break;
      }
      // else we loop through all the neighbours
      // adding them to the queue
      getNeighbours(temp).stream()
          .forEach(p -> checkNeighbour(temp, goal, temp.getgValue() + 1, p, open, closed));

      // adding the head of the queue to visited list
      closed.add(temp);

    }

    if (finish == null) {
      return null;
    }

    return getMovesFromPoints(finish);
  }

  /**
   * Gets the map from the game state.
   *
   * @return the map
   */
  private Block[][] getMap() {
    return state.getMap().getGridMap();
  }

  /**
   * Check neighbour. Checks if the neighbour tile is a possible move
   *
   * @param parent
   *          the parent node
   * @param goal
   *          the goal position
   * @param cost
   *          the cost from starting position (g value)
   * @param neigh
   *          the neighbouring tile
   * @param open
   *          the open list of tile
   * @param closed
   *          the closed list of tiles
   */
  private void checkNeighbour(Node parent, Point goal, int cost, Point neigh,
      PriorityQueue<Node> open, HashSet<Node> closed) {
    // we check if the coordinates are valid
    // if not we return
    if (!checkMoveValidity(neigh)) {
      return;
    }

    //

    // if the neighbour is in the visited list we return
    for (Node nd : closed) {
      if (nd.getCoord().equals(neigh)) {
        return;
      }
    }

    // else we iterate through the queue and add the new element to it if
    // the path is better that the
    // previous already in the queue
    for (Node nd : open) {
      if (nd.getCoord().equals(neigh) && cost < nd.getgValue()) {
        open.remove(nd);
        int hValue = countDistance(goal, neigh);
        Node neighNode = new Node(cost, hValue, parent, neigh);
        open.add(neighNode);
        return;
      }
    }

    // else we add it to the queue
    int hValue = countDistance(goal, neigh);
    Node neighNode = new Node(cost, hValue, parent, neigh);
    open.add(neighNode);

  }

  /**
   * Check neighbour with soft tiles. Checks if the neighbour tile is a possible move including the
   * soft blocks.
   *
   * @param parent
   *          the parent node
   * @param goal
   *          the goal position
   * @param cost
   *          the cost from starting position (g value)
   * @param neigh
   *          the neighbouring tile
   * @param open
   *          the open list of tile
   * @param closed
   *          the closed list of tiles
   */
  private void checkNeighbourWithSoftTiles(Node parent, Point goal, int cost, Point neigh,
      PriorityQueue<Node> open, HashSet<Node> closed) {
    int x = neigh.x;
    int y = neigh.y;
    Block[][] map = getMap();

    if ((x < 0) || (y < 0) || map.length <= x || map[0].length <= y || map[x][y] == Block.SOLID
        || map[x][y] == Block.HOLE) {
      return;
    }

    for (Node nd : closed) {
      if (nd.getCoord().equals(neigh)) {
        return;
      }
    }

    for (Node nd : open) {
      if (nd.getCoord().equals(neigh) && cost < nd.getgValue()) {
        open.remove(nd);
        int hValue = countDistance(goal, neigh);
        Node neighNode = new Node(cost, hValue, parent, neigh);
        open.add(neighNode);
        return;
      }
    }

    int hValue = countDistance(goal, neigh);
    Node neighNode = new Node(cost, hValue, parent, neigh);
    open.add(neighNode);
  }

  /**
   * Returns the sequence of moves from the final finish node. Loops recursively to get all the
   * sequence of actions. Backtracks the route from final node.
   *
   * @param finish
   *          the finish node
   * @return the sequence of moves
   */
  private LinkedList<AIActions> getMovesFromPoints(Node finish) {
    LinkedList<AIActions> moves = new LinkedList<>();

    while (finish.getParent() != null) {
      int x = finish.getCoord().x;
      int y = finish.getCoord().y;
      int xParent = finish.getParent().getCoord().x;
      int yParent = finish.getParent().getCoord().y;

      if (x - 1 == xParent) {
        moves.addFirst(AIActions.RIGHT);
      } else if (x + 1 == xParent) {
        moves.addFirst(AIActions.LEFT);
      } else if (y - 1 == yParent) {
        moves.addFirst(AIActions.DOWN);
      } else {
        moves.addFirst(AIActions.UP);
      }

      finish = finish.getParent();
    }
    return moves;
  }

  /**
   * Gets four neighbours from a particular position.
   *
   * @param parent
   *          the node
   * @return the neighbours of the position
   */
  private ArrayList<Point> getNeighbours(Node parent) {
    int x = parent.getCoord().x;
    int y = parent.getCoord().y;
    ArrayList<Point> neighbours = new ArrayList<>();
    neighbours.add(new Point(x + 1, y));
    neighbours.add(new Point(x - 1, y));
    neighbours.add(new Point(x, y + 1));
    neighbours.add(new Point(x, y - 1));

    return neighbours;
  }

  /**
   * Check neighbour if the neighbours tile is possible move.
   *
   * @param parent
   *          the parent node
   * @param tile
   *          the position of the tile
   * @param open
   *          the open list of positions to be visited
   * @param closed
   *          the closed list of positions already visited
   * @param map
   *          the map
   */
  private void checkNeighbour(Node parent, Point tile, LinkedList<Node> open, HashSet<Node> closed,
      Block[][] map) {
    int x = tile.x;
    int y = tile.y;
    // Block[][] map = getMap();

    if ((x < 0) || (y < 0) || map.length <= x || map[0].length <= y || map[x][y] == Block.SOFT
        || map[x][y] == Block.SOLID || map[x][y] == Block.MINUS_BOMB
        || map[x][y] == Block.MINUS_RANGE || map[x][y] == Block.MINUS_SPEED
        || map[x][y] == Block.HOLE) {
      return;
    }

    List<Bomb> bombs = new ArrayList<Bomb>(state.getBombs());

    for (Bomb b : bombs) {
      // System.out.println(b);
      if (b != null) {
        if (b.getGridPos().equals(tile)) {
          return;
        }
      }
    }

    for (Node nd : closed) {
      if (nd.getCoord().equals(tile)) {
        return;
      }
    }

    for (Node nd : open) {
      if (nd.getCoord().equals(tile)) {
        return;
      }

    }

    Node neighNode = new Node(parent, tile);
    open.add(neighNode);
  }

  /**
   * Escape from explosion. Finds and returns the fastest route from the explosion when the AI is in
   * danger. Using breadth-first search
   *
   * @param dangerTiles
   *          the danger tiles which might damage the AI
   * @return the list of moves to be made to escape from explosion.
   */
  public LinkedList<AIActions> escapeFromExplotion(ArrayList<Point> dangerTiles) {
    Point pos = gameAI.getGridPos();
    LinkedList<Node> open = new LinkedList<>();
    HashSet<Node> closed = new HashSet<>();

    Node startNode = new Node(null, pos);
    open.add(startNode);
    List<Node> finishPositions = new ArrayList<>();

    // loop until the queue is not empty
    Node finish = null;
    while (!open.isEmpty()) {

      // take the head of the queue
      Node temp = open.poll();

      // if the head is final position we finish
      if (!dangerTiles.contains(temp.getCoord())) {
        finishPositions.add(temp);
        if (finishPositions.size() > 3) {
          break;
        }

        finish = temp;
      }

      for (Point p : getNeighbours(temp)) {
        checkNeighbour(temp, p, open, closed, getMap());
      }

      // else we loop through all the neighbours
      closed.add(temp);
    }
    // finishPositions.stream().forEach(n -> System.out.println(n.getCoord()));
    // System.out.println('\n');
    finish = findFurthestPositionFromEnemies(finishPositions);
    // System.out.println(finish.getCoord());
    // System.out.println('\n');
    if (finish == null) {
      return null;
    }

    return getMovesFromPoints(finish);

  }

  /**
   * Gets the nearest enemy.
   *
   * @return the nearest enemy of the AI.
   */
  public Point getNearestEnemy() {
    Point aiPos = gameAI.getGridPos();
    Point pos = null;
    int distance = Integer.MAX_VALUE;
    int temp = 0;
    for (Player p : state.getPlayers()) {
      if (!p.equals(gameAI) && p.isAlive()
          && (temp = countDistance(aiPos, p.getGridPos())) < distance) {
        distance = temp;
        pos = p.getGridPos();
      }
    }

    return pos;
  }

  /**
   * Checks if is soft block after move.
   *
   * @param move
   *          the move
   * @param aiPos
   *          the AI position
   * @param map
   *          the map
   * @return true, if the block is soft after move
   */
  private boolean isSoftBlockAfterMove(AIActions move, Point aiPos, Block[][] map) {

    switch (move) {
    case UP:
      if (map[aiPos.x][aiPos.y - 1] == Block.SOFT)
        return true;
      break;
    case DOWN:
      if (map[aiPos.x][aiPos.y + 1] == Block.SOFT)
        return true;
      break;
    case LEFT:
      if (map[aiPos.x - 1][aiPos.y] == Block.SOFT)
        return true;
      break;
    case RIGHT:
      if (map[aiPos.x + 1][aiPos.y] == Block.SOFT)
        return true;
      break;
    default:
      break;
    }
    return false;
  }

  /**
   * Reverse moves. Reverses the moves in the planning phase. For example when AI places the bomb,
   * finds the escape route and after the bomb exploded it wants to get back to the previous
   * position where he places the bomb.
   * 
   * @param moves
   *          the moves
   * @return the reversed sequence of moves
   */
  private LinkedList<AIActions> reverseMoves(LinkedList<AIActions> moves) {
    LinkedList<AIActions> revMoves = new LinkedList<>();
    for (AIActions m : moves) {
      switch (m) {
      case UP:
        revMoves.addFirst(AIActions.DOWN);
        break;
      case DOWN:
        revMoves.addFirst(AIActions.UP);
        break;
      case LEFT:
        revMoves.addFirst(AIActions.RIGHT);
        break;
      case RIGHT:
        revMoves.addFirst(AIActions.LEFT);
        break;
      default:
        break;
      }
    }

    return revMoves;
  }

  /**
   * Update planned position of AI and map according to the move.
   *
   * @param action
   *          the action (move)
   * @param pos
   *          the position of AI
   * @param map
   *          the map
   */
  private void updatePositionAndMap(AIActions action, Point pos, Block[][] map) {

    switch (action) {
    case UP:
      pos.setLocation(pos.x, pos.y - 1);
      break;
    case DOWN:
      pos.setLocation(pos.x, pos.y + 1);
      break;
    case LEFT:
      pos.setLocation(pos.x - 1, pos.y);
      break;
    case RIGHT:
      pos.setLocation(pos.x + 1, pos.y);
      break;
    default:
      break;
    }
    map[pos.x][pos.y] = Block.BLANK;
  }

  /**
   * Returns the planned actions of the AI including bomb placement and moves.
   *
   * @param movesWithoutObstacles
   *          the moves without obstacles (moves which doesn't consider soft blocks)
   * @param position
   *          the position
   * @return the planned actions of the AI including bomb placement and moves.
   */
  private LinkedList<AIActions> getPathWithBombs(LinkedList<AIActions> movesWithoutObstacles,
      Point position) {

    Block[][] map2 = getMap();

    // copy the real map
    Block[][] map = new Block[map2.length][map2[0].length];
    for (int x = 0; x < map2.length; x++) {
      for (int y = 0; y < map2[0].length; y++) {
        map[x][y] = map2[x][y];
      }
    }

    LinkedList<AIActions> realMoves = new LinkedList<>();
    Point pos = new Point(position.x, position.y);
    AIActions move = null;
    while ((move = movesWithoutObstacles.peek()) != null) {
      movesWithoutObstacles.removeFirst();
      if (isSoftBlockAfterMove(move, pos, map)) {
        realMoves.add(AIActions.BOMB);

        LinkedList<AIActions> escapeMoves = (escapeFromExplotion(safetyCh.getBombCoverage(
            new Bomb(null,
                new Point(pos.x * Constants.MAP_BLOCK_TO_GRID_MULTIPLIER,
                    pos.y * Constants.MAP_BLOCK_TO_GRID_MULTIPLIER),
                0, gameAI.getBombRange()),
            map), pos, map));
        if (escapeMoves == null) {
          return realMoves;
        }
        realMoves.addAll(escapeMoves);
        realMoves.add(AIActions.NONE);
        realMoves.addAll(reverseMoves(escapeMoves));

      }

      realMoves.addLast(move);
      updatePositionAndMap(move, pos, map);
    }

    return realMoves;
  }

  /**
   * Gets the planned sequence of actions to enemy.
   *
   * @param start
   *          the starting position
   * @param goal
   *          the goal position
   * @return the planned sequence of actions to enemy
   */
  public LinkedList<AIActions> getPlanToEnemy(Point start, Point goal) {

    PriorityQueue<Node> open = new PriorityQueue<>();
    HashSet<Node> closed = new HashSet<>();
    if (start == null || goal == null)
      return null;
    int hValue = Math.abs(goal.x - start.x) + Math.abs(goal.y - start.y);
    Node startNode = new Node(0, hValue, null, start);
    open.add(startNode);

    // loop until the queue is not empty
    Node finish = null;
    while (!open.isEmpty()) {

      // take the head of the queue
      Node temp = open.poll();
      // if the head is final position we finish
      if (temp.getCoord().equals(goal)) {
        finish = temp;
        break;
      }

      // else we loop through all the neighbours
      getNeighbours(temp).stream().forEach(
          p -> checkNeighbourWithSoftTiles(temp, goal, temp.getgValue() + 1, p, open, closed));

      closed.add(temp);

    }

    if (finish == null) {
      return null;
    }

    return getPathWithBombs(getMovesFromPoints(finish), start);

  }

  /**
   * Can put bomb and escape.
   * 
   * Method for checking if the AI can put bomb and safely escape
   *
   * @return the linked list of moves
   */
  public LinkedList<AIActions> canPutBombAndEscape() {
    LinkedList<AIActions> moves = null;
    if (safetyCh.isEnemyInBombRange()) {
      ArrayList<Point> bombs = safetyCh.getTilesAffectedByBombs();
      ArrayList<Point> coverage = safetyCh.getBombCoverage(new Bomb(gameAI.getName(),
          gameAI.getPos(), Constants.DEFAULT_BOMB_TIME, gameAI.getBombRange()), getMap());
      bombs.addAll(coverage);
      moves = escapeFromExplotion(bombs);

    }
    if ((moves != null) && (moves.size() < 4)) {
      return moves;
    }

    return null;
  }

  /**
   * Escape from explosion.
   *
   * @param dangerTiles
   *          the danger tiles
   * @param pos
   *          the positions
   * @param map
   *          the map
   * @return the linked list of moves
   */
  private LinkedList<AIActions> escapeFromExplotion(ArrayList<Point> dangerTiles, Point pos,
      Block[][] map) {
    LinkedList<Node> open = new LinkedList<>();
    HashSet<Node> closed = new HashSet<>();
    Node startNode = new Node(null, pos);
    open.add(startNode);

    // loop until the queue is not empty
    Node finish = null;
    while (!open.isEmpty()) {

      // take the head of the queue
      Node temp = open.poll();

      // if the head is final position we finish
      if (!dangerTiles.contains(temp.getCoord())) {
        finish = temp;
        break;
      }

      getNeighbours(temp).stream().forEach(p -> checkNeighbour(temp, p, open, closed, map));

      // else we loop through all the neighbours
      closed.add(temp);
    }

    if (finish == null) {
      return null;
    }

    return getMovesFromPoints(finish);

  }

  /**
   * Gets the nearest enemy.
   * 
   * Returns the nearest enemy excluding AIs
   * 
   * AIs can collaborate in that way.
   *
   * @return the nearest enemy of the AI.
   */
  public Point getNearestEnemyExcludeAIs() {
    Point aiPos = gameAI.getGridPos();
    Point pos = null;
    int distance = Integer.MAX_VALUE;
    List<Player> players = state.getPlayers().stream()
        .filter(p -> !(p instanceof GameAI) && p.isAlive()).collect(Collectors.toList());

    int temp = 0;
    for (Player p : players) {
      if ((temp = countDistance(aiPos, p.getGridPos())) < distance) {
        distance = temp;
        pos = p.getGridPos();
      }
    }

    return pos;
  }

  /**
   * Can put bomb and escape.
   * 
   * Method for checking if the AI can put bomb and safely escape.
   * 
   * Excludes other AIs so that AIs can collaborate
   *
   * @return the linked list of moves
   */
  public LinkedList<AIActions> canPutBombAndEscapeExcludeAIs() {
    LinkedList<AIActions> moves = null;
    if (safetyCh.isEnemyInBombRangeExludeAIs()) {
      ArrayList<Point> bombs = safetyCh.getTilesAffectedByBombs();
      ArrayList<Point> coverage = safetyCh.getBombCoverage(
          new Bomb(gameAI.getName(), gameAI.getPos(), 0, gameAI.getBombRange()), getMap());
      bombs.addAll(coverage);
      moves = escapeFromExplotion(bombs);

    }
    if ((moves != null) && (moves.size() < 5)) {
      return moves;
    }

    return null;
  }

  /**
   * Find route to the upgrade. Finds the fastest route to the upgrade using breadth-first algorithm
   *
   * @return the linked list
   */
  public LinkedList<AIActions> findRouteToUpgrade() {
    Point pos = gameAI.getGridPos();
    LinkedList<Node> open = new LinkedList<>();
    HashSet<Node> closed = new HashSet<>();
    Block[][] map = getMap();

    Node startNode = new Node(null, pos);
    open.add(startNode);

    // loop until the queue is not empty
    Node finish = null;
    while (!open.isEmpty()) {

      // take the head of the queue
      Node temp = open.poll();

      // if the head is final position we finish
      Block singleBlock = map[temp.getCoord().x][temp.getCoord().y];
      if (singleBlock == Block.PLUS_BOMB || singleBlock == Block.PLUS_RANGE
          || singleBlock == Block.PLUS_SPEED) {
        if (isNearestAI(temp.getCoord())) {
          finish = temp;
          break;
        }

      }

      for (Point p : getNeighbours(temp)) {
        checkNeighbour(temp, p, open, closed, getMap());
      }

      // else we loop through all the neighbours
      closed.add(temp);
    }

    if (finish == null) {
      return null;
    }

    return getMovesFromPoints(finish);

  }

  /**
   * Finds the furthest position from enemies. Given a list of possible positions it returns the one
   * which is furthest from the enemy, so that AI could avoid going to the enemy direction after
   * bomb was placed
   *
   * @param finishPositions
   *          the finish positions
   * @return the node
   */
  private Node findFurthestPositionFromEnemies(List<Node> finishPositions) {
    Node furthestPos = null;
    int furthest = Integer.MIN_VALUE;
    int temp;
    for (Node n : finishPositions) {
      int smallestDist = Integer.MAX_VALUE;
      for (Player p : state.getPlayers()) {
        if (!(p instanceof GameAI)
            && smallestDist > (temp = countDistance(n.getCoord(), p.getGridPos()))) {
          smallestDist = temp;
        }
      }

      if (smallestDist > furthest) {
        furthest = smallestDist;
        furthestPos = n;
      }
    }
    return furthestPos;
  }

  /**
   * Count the distance from one position to another
   *
   * @param p1
   *          the position 1
   * @param p2
   *          the position 2
   * @return the distance
   */
  private int countDistance(Point p1, Point p2) {
    return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
  }

  /**
   * Check move validity. Helps AI to avoid going into the wall, soft block, or out of the map
   *
   * @param p
   *          the position
   * @return true, if the move is valid
   */
  private boolean checkMoveValidity(Point p) {
    int x = p.x;
    int y = p.y;
    Block[][] map = getMap();
    return !((x < 0) || (y < 0) || map.length <= x || map[0].length <= y || map[x][y] == Block.SOFT
        || map[x][y] == Block.SOLID || map[x][y] == Block.MINUS_BOMB
        || map[x][y] == Block.MINUS_RANGE || map[x][y] == Block.MINUS_SPEED
        || map[x][y] == Block.HOLE);
  }

  /**
   * Checks if the position is enclosure.
   *
   * @param dangerTiles
   *          the danger tiles affected by bombs
   * @param position
   *          the position
   * @return true, if the position is enclosure so avoid it
   */
  public boolean isEnclosure(ArrayList<Point> dangerTiles, Point position) {
    LinkedList<Point> positions = new LinkedList<Point>();
    positions.add(position);
    Point temp = null;
    int numberOfPossibleMoves = 0;
    while (!positions.isEmpty() && numberOfPossibleMoves < 5) {
      temp = positions.poll();
      if (temp == null || dangerTiles.contains(temp)) {
        continue;
      }

      else if (checkMoveValidity(temp)) {
        numberOfPossibleMoves++;
        positions.addAll(getNeighbours(new Node(null, temp)));
      }

    }
    return numberOfPossibleMoves < 5;

  }

  /**
   * Checks if this AI is nearest to the goal
   *
   * @param goal
   *          the goal
   * @return true, if this AI is nearest to the goal
   */
  private boolean isNearestAI(Point goal) {
    List<Player> ais = state.getPlayers().stream()
        .filter(p -> (p instanceof GameAI) && p.isAlive() && !p.equals(gameAI))
        .collect(Collectors.toList());
    int distanceFromThisAI = countDistance(gameAI.getGridPos(), goal);
    int smallestDistance = Integer.MAX_VALUE;
    int temp;
    for (Player p : ais) {
      if ((temp = countDistance(p.getGridPos(), goal)) < smallestDistance) {
        smallestDistance = temp;
      }

    }

    return distanceFromThisAI <= smallestDistance;
  }
}
