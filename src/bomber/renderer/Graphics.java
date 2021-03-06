package bomber.renderer;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import bomber.game.Constants;
import bomber.renderer.interfaces.GameInterface;

/**
 * Class that holds the Graphic Thread with all its feature
 * 
 * @author Alexandru Blinda
 * 
 */
public class Graphics implements Runnable {

	private final Thread gameLoopThread;
	private final Screen screen;
	private final Timer timer;
	private final GameInterface gameLogic;

	/**
	 * Create a Graphics object with the given parameters.
	 * 
	 * @param screenTitle
	 *            The title of the screen
	 * @param screenWidth
	 *            The width of the screen
	 * @param screenHeight
	 *            The height of the screen
	 * @param vSync
	 *            Boolean to use vSync
	 * @param gameLogic
	 *            The game logic for the graphics
	 * @param fullScreen
	 *            The game runs in full screen or window#
	 * @param wasd
	 *            Tells the graphics to use either wasd or arrow keys
	 * @throws Exception
	 */
	public Graphics(String screenTitle, int screenWidth, int screenHeight, boolean vSync, GameInterface gameLogic,
			boolean fullScreen) throws Exception {

		gameLoopThread = new Thread(this, "_THREAD_GAME_LOOP");
		this.screen = new Screen(screenTitle, screenWidth, screenHeight, vSync, fullScreen);
		this.gameLogic = gameLogic;
		timer = new Timer();
	}

	/**
	 * Start the graphics
	 */
	public void start() {

		// Start the thread for the game engine loop
		gameLoopThread.start();
	}

	/**
	 * Run the graphics
	 */
	@Override
	public void run() {

		try {

			// Try to initialize the game and loop
			init();
			gameLoop();
		} catch (Exception ex) {

			// Catch an exception
			System.err.println("ERROR1!" + ex.getMessage());
			ex.printStackTrace();
		} finally {

			dispose();
		}

	}

	/**
	 * Initialise the graphics
	 * 
	 * @throws Exception
	 */
	private void init() throws Exception {

		this.screen.init();
		this.timer.init();
		this.gameLogic.init(screen);
	}

	/**
	 * Update the game logic at the given interval
	 * 
	 * @param interval
	 *            The interval at everything is updated
	 */
	private void update(float interval) {

		// Game logic gets updated
		gameLogic.update(interval);
	}

	/**
	 * Render everything on the screen
	 */
	private void render() {

		// Render the renderer
		gameLogic.render(screen);
		screen.update();
	}

	/**
	 * The game loop responsible with calculating UPS and FPS
	 */
	private void gameLoop() {

		float deltaTime = 0f;
		float accumulator = 0f;
		float interval = 1f / Constants.TARGET_UPS;

		boolean gameRunning = true;

		// The main loop of the game
		while (gameRunning && (!screen.screenShouldClose())) {
			deltaTime = timer.getDeltaTime();
			accumulator = accumulator + deltaTime;
			input();
			// Update game and timer UPS if enough time passed
			while (accumulator >= interval) {

				update(interval);
				timer.updateUPS();
				accumulator = accumulator - interval;
			}

			// Render game and update timer fps
			render();
			timer.updateFPS();

			// Update the timer so we get accurat FPS and UPS
			timer.update();

			// If the vSync is off, use our sync method
			if (!screen.isVsyncOn()) {

				sync();
			}
		}
	}

	/**
	 * Synchronise everything if vSync is not on
	 */
	private void sync() {

		float loopInterval = 1f / Constants.TARGET_FPS;
		double endTime = timer.getLastLoopTime() + loopInterval;

		while (timer.getTime() < endTime) {

			try {

				Thread.sleep(1);
			} catch (InterruptedException ie) {

				System.err.println("ERROR2!");
				ie.printStackTrace();
			}
		}

	}

	/**
	 * Listen for inputs from the game logic
	 */
	public void input() {

		gameLogic.input(screen);
	}

	/**
	 * Dispose the graphics and game logic
	 */
	public void dispose() {

		gameLogic.dispose();
		glfwDestroyWindow(this.screen.getScreenID());
	}

	/**
	 * Get the screen of the graphics
	 * 
	 * @return The screen
	 */
	public Screen getScreen() {

		return this.screen;
	}
}
