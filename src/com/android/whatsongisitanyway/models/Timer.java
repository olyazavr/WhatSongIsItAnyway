package com.android.whatsongisitanyway.models;

/**
 * ADT representing the timer
 * 
 */
public class Timer {
	private int time;
	private double startTime;

	private boolean run;
	private boolean pause;

	/**
	 * pauseTime is the time of the user clicking pause, pauseTimeElapsed is the
	 * current window of time in which pause is being pressed,
	 * prevPauseTimeElapsed is the sum over all the pause time intervals (if
	 * user has pressed pause multiple times, we want to add all of those times)
	 */
	private double pauseTime;
	private double pauseTimeElapsed;
	private double prevPauseTimeElapsed;

	/**
	 * Makes a Timer object, but *does not* start it. To start the Timer, call
	 * run()
	 * 
	 * @param time
	 *            the amount of time in seconds to run the timer for
	 */
	public Timer(int time) {
		this.time = time;
		startTime = 0;
		pauseTime = 0;
		prevPauseTimeElapsed = 0;
		pauseTimeElapsed = 0;
		run = false;
		pause = false;
	}

	/**
	 * Starts the timer inside the timerThread
	 * 
	 */
	public void run() {
		startTime = (System.currentTimeMillis() / 1000.0);
		run = true;
	}

	/**
	 * Gets the amount of time left in seconds
	 * 
	 * @return time left
	 */
	public int getTimeLeft() {
		if (run) {
			if (pause) {
				pauseTimeElapsed = (System.currentTimeMillis() / 1000.0)
						- pauseTime;
			}
			double currentTime = (System.currentTimeMillis() / 1000.0);
			double timeElapsed = currentTime - startTime;

			// time limit - time elapsed + time game was paused
			int timeLeft = (int) (time - timeElapsed + pauseTimeElapsed + prevPauseTimeElapsed);

			// return 0 if timeLeft < 0
			return timeLeft < 0 ? 0 : timeLeft;
		}
		return time;
	}

	/**
	 * Pause the timer
	 */
	public void pause() {
		if (run) {
			pause = true;
			pauseTime = System.currentTimeMillis() / 1000.0;
		}
	}

	/**
	 * Resume the timer from pause
	 */
	public void resume() {
		if (run && pause) {
			prevPauseTimeElapsed += pauseTimeElapsed;
			pauseTimeElapsed = 0;
			pause = false;
		}
	}

	/**
	 * Decrement the timer by a number of seconds (used for penalties)
	 * 
	 * @param seconds
	 *            seconds to decrement by
	 */
	public void decrement(int seconds) {
		if (run) {
			time -= seconds;
		}
	}

}