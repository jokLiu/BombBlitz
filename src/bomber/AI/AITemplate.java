package bomber.AI;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import bomber.game.Constants;
import bomber.game.GameState;
import bomber.game.Movement;
import bomber.game.Player;

/**
 * Framework for the AI to run.
 * 
 * @author Jokubas Liutkus
 * 
 */
public abstract class AITemplate extends Thread {
  /** The game AI. */
  protected GameAI gameAI;

  /** The finder for finding the route. */
  protected RouteFinder finder;

  /** The safety checker for AI. */
  protected SafetyChecker safetyCh;

  /** The game state. */
  protected GameState gameState;

  /** The pause pressed checked. */
  protected boolean pause = false;

  /**
   * Instantiates a new template.
   *
   * @param ai
   *          the AI
   * @param gameState
   *          the game state
   */
  public AITemplate(GameAI ai, GameState gameState) {
    this.gameAI = ai;
    this.gameState = gameState;
    this.safetyCh = new SafetyChecker(gameState, ai);
    this.finder = new RouteFinder(gameState, ai, safetyCh);
  }

  /**
   * Pause.
   */
  public void pause() {
    pause = true;
  }

  /**
   * Update.
   */
  public void update() {
    pause = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Thread#run()
   */
  public void run() {
    try {
      sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    move();
  }

  /**
   * Updates the current AI position according to the particular move.
   *
   * @param move
   *          the AI move
   * @return the updated position after the move
   */
  protected Point updatedPos(AIActions move) {
    Point aiPos = (Point) gameAI.getGridPos().clone();
    switch (move) {
    case UP:
      aiPos.setLocation(aiPos.x, (aiPos.y - 1));
      break;
    case DOWN:
      aiPos.setLocation(aiPos.x, (aiPos.y + 1));
      break;
    case LEFT:
      aiPos.setLocation((aiPos.x - 1), aiPos.y);
      break;
    case RIGHT:
      aiPos.setLocation((aiPos.x + 1), aiPos.y);
      break;
    default:
      break;
    }
    return aiPos;
  }

  /**
   * From AI moves to game moves. Changes the AI moves to the general game moves
   *
   * @param action
   *          the move to be changed
   * @return move in Movement representation
   */
  protected Movement FromAIMovesToGameMoves(AIActions action) {
    Movement m = null;
    switch (action) {
    case UP:
      m = Movement.UP;
      break;
    case DOWN:
      m = Movement.DOWN;
      break;
    case LEFT:
      m = Movement.LEFT;
      break;
    case RIGHT:
      m = Movement.RIGHT;
      break;
    default:
      break;
    }
    return m;
  }

  /**
   * Check if the AI reached destination when making a single move.
   *
   * @param currentPixel
   *          the pixel position of the AI
   * @param updatedFinalPixelPos
   *          the updated final pixel position which has to be reached
   * @return true, if it is needed to stop moving
   */
  protected boolean checkIfReachedDestination(Point currentPixel, Point updatedFinalPixelPos) {
    boolean check = (currentPixel.x
        - updatedFinalPixelPos.x) < (Constants.MAP_BLOCK_TO_GRID_MULTIPLIER
            - Constants.PLAYER_WIDTH);
    check &= (updatedFinalPixelPos.x <= currentPixel.x);
    check &= (currentPixel.y - updatedFinalPixelPos.y) < (Constants.MAP_BLOCK_TO_GRID_MULTIPLIER
        - Constants.PLAYER_HEIGHT);
    check &= (updatedFinalPixelPos.y <= currentPixel.y);
    return !check;
  }

  /**
   * Make single move.
   * 
   * Method to make a single move in the real game.
   *
   * @param move
   *          the move to be made
   */
  protected void makeSingleMove(AIActions move) {
    // updated positions
    Point updatedPosPixel = updatedPos(move);
    Point updatedPos = new Point(updatedPosPixel);

    // sets the position to the pixel representation
    updatedPosPixel.setLocation(updatedPosPixel.x * Constants.MAP_BLOCK_TO_GRID_MULTIPLIER,
        updatedPosPixel.y * Constants.MAP_BLOCK_TO_GRID_MULTIPLIER);

    // sets the keyboard state for the move to be made
    gameAI.getKeyState().setMovement(FromAIMovesToGameMoves(move));

    // checking if the AI got stuck
    int stuckChecker = 0;

    // waiting for the move to be made
    while (checkIfReachedDestination(gameAI.getPos(), updatedPosPixel) && gameAI.isAlive()
        && (!safetyCh.isNextMoveBomb(updatedPos)) && (stuckChecker < 75)) {

      // checking if the game is paused
      pausedGame();
      stuckChecker++;
      try {
        sleep(10);
      } catch (InterruptedException e) {
      }
    }

    // setting keyboard back to normal
    gameAI.getKeyState().setMovement(Movement.NONE);
  }

  /**
   * Paused game.
   */
  protected void pausedGame() {
    while (pause) {
      try {
        sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Gets the moves to enemy.
   *
   * @return the moves to enemy, null if the route is blocked by bombs
   */
  protected LinkedList<AIActions> getMovesToEnemy() {

    // find the route to the nearest enemy
    LinkedList<AIActions> moves = finder.findRoute(gameAI.getGridPos(), finder.getNearestEnemy());
    if (moves != null) {
      return moves;
    }

    // else we loop through each enemy looking for the possible access
    for (Player p : gameState.getPlayers()) {
      if (!p.equals(gameAI) && (p.isAlive())) {
        moves = finder.findRoute(gameAI.getGridPos(), p.getGridPos());

        // when the path is find we return the path
        if (moves != null)
          return moves;
      }
    }

    // if no possible paths are find null is returned
    return null;
  }

  /**
   * Gets the moves to enemy excluding the AI. Other AIs are ignored.
   *
   * @return the moves to enemy ignoring other AIs, null is returned if route is blocked
   */
  protected LinkedList<AIActions> getMovesToEnemyExcludeAIs() {

    // find the route to the nearest enemy
    LinkedList<AIActions> moves = finder.findRoute(gameAI.getGridPos(),
        finder.getNearestEnemyExcludeAIs());

    // if the route is found to the nearest enemy, we return the route
    if (moves != null)
      return moves;

    // otherwise we loop until we find one or return null if none of them are possible
    List<Player> players = gameState.getPlayers().stream()
        .filter(p -> !(p instanceof GameAI) && p.isAlive()).collect(Collectors.toList());
    for (Player p : players) {
      moves = finder.findRoute(gameAI.getGridPos(), p.getGridPos());
      if (moves != null)
        return moves;
    }
    return null;
  }

  /**
   * Perform sequence of moves.
   *
   * @param moves
   *          the list of moves to be performed
   * @param inDanger
   *          the variable determining if the escape moves are passed in that case make moves
   *          without considering anything else
   */
  protected abstract void performMoves(LinkedList<AIActions> moves, boolean inDanger);

  /**
   * Perform planned moves. When none of the players are reachable
   *
   * @param moves
   *          the moves to be performed
   */
  protected abstract void performPlannedMoves(LinkedList<AIActions> moves);

  /**
   * 
   * Main method for controlling what moves to make.
   */
  protected abstract void move();
}
