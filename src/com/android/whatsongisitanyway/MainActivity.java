package com.android.whatsongisitanyway;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.android.whatsongisitanyway.models.Game;
import com.android.whatsongisitanyway.models.Music;

public class MainActivity extends Activity implements
		LoaderManager.LoaderCallbacks<Cursor> {
	private Game game;
	private MediaPlayer mediaPlayer = null;
	private Music currentSong = null;

	private boolean running = false;
	private boolean paused = false;

	// The callbacks through which we will interact with the LoaderManager.
	private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;
	List<String> songs = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// add enter listener
		TextView songBox = (TextView) findViewById(R.id.songTextbox);
		songBox.setOnEditorActionListener(submitListener);

		// stuff for loading songs
		mCallbacks = this;
		LoaderManager lm = getLoaderManager();
		lm.initLoader(1, null, mCallbacks);

		// Log.d("a song ", "SON");
		// MusicRetriever mr = new MusicRetriever(this);
		//
		// List<String> songs = mr.getMusic();
		// for (String song : songs) {
		// Log.d("a song ", song);
		// }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// Create a new CursorLoader with the following query parameters.
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

		String[] projection = { MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.DISPLAY_NAME,
				MediaStore.Audio.Media.DURATION,
				MediaStore.Audio.Media.ALBUM_ID };

		return new CursorLoader(MainActivity.this,
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
				selection, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		while (cursor.moveToNext()) {
			songs.add(cursor.getString(0));
			songs.add(cursor.getString(1));
			songs.add(cursor.getString(2));
			songs.add(cursor.getString(3));
			songs.add(cursor.getString(4));
			System.out.println(cursor.getString(0));
			System.out.println(cursor.getString(1));
			System.out.println(cursor.getString(2));
			System.out.println(cursor.getString(3));
			System.out.println(cursor.getString(4));
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// For whatever reason, the Loader's data is now unavailable.
	}

	/**
	 * Either starts up media player if it is null, or skips to the next song if
	 * a song is already playing
	 * 
	 * @param view
	 */
	public void skip(View view) {
		// do nothing if paused
		if (paused) {
			return;
		}
		if (running) {
			// penalty for skipping
			game.skipPenalty();
			// multiplier and streak are lost
			updateUILabel(R.id.streak, "Streak: 0");
			updateUILabel(R.id.multiplier, "Multiplier: 1");
		} else {
			running = true;
			game = new Game(getResources());
			initTimerThread();
		}

		goToNextSong();

	}

	/**
	 * Pauses/resumes song play
	 * 
	 * @param view
	 */
	public void pause(View view) {
		if (running) {
			if (!paused) {
				paused = true;
				mediaPlayer.pause();
				game.pause();
			} else {
				paused = false;
				mediaPlayer.start();
				game.resume();
			}
		}
	}

	/**
	 * Submits the guess to the game, updates the score
	 * 
	 * @param view
	 */
	public void submit(View view) {
		TextView songBox = (TextView) findViewById(R.id.songTextbox);

		// if the score hasn't changed, it returns 0
		int score = game.guess(songBox.getText().toString());
		updateUILabel(R.id.songTextbox, "");

		// if they got it right, update score and skip songs
		if (score > 0) {
			updateUILabel(R.id.score, "Score: " + score);
			updateUILabel(R.id.streak, "Streak: " + game.getStreak());
			updateUILabel(R.id.multiplier,
					"Multiplier: " + game.getMultiplier());
			goToNextSong();
		}
	}

	/**
	 * Set the music player to go to the next song, if there are any left.
	 * Release the player if we're done.
	 */
	public void goToNextSong() {
		currentSong = game.getNextSong();
		TextView textView = (TextView) findViewById(R.id.title);

		// if we still have music to play
		if (currentSong != null) {
			try {
				// set the new title
				updateUILabel(R.id.title, currentSong.getID() + "");

				// stop old music player
				if (mediaPlayer != null) {
					mediaPlayer.stop();
				}

				// create new music player
				mediaPlayer = MediaPlayer.create(textView.getContext(),
						currentSong.getID());
				// go to a different start

				// make sure rest of songs play
				mediaPlayer
						.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
							@Override
							public void onCompletion(MediaPlayer mediaPlayer) {
								goToNextSong();
							}
						});

				// actually start!
				mediaPlayer.start();
				// skip to a random place
				mediaPlayer.seekTo(currentSong.getRandomStart());

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// we're done!
			// TODO: do something to alert user here...
			if (mediaPlayer != null) {
				mediaPlayer.release();
				mediaPlayer = null;
			}
			running = false;
			updateUILabel(R.id.title, "What Song Is It Anyway?");
		}
	}

	/**
	 * Initialize the timer loop thread
	 * 
	 */
	private void initTimerThread() {
		Thread timerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				game.start();
				initSongTimerThread();

				// while we have time left
				while (running && game.timeLeft() > 0) {

					updateUILabel(R.id.timer, game.timeLeftString());

					// to avoid updating too often, sleep for .2 secs
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

				// TODO: we're done, now what?
				running = false;
				updateUILabel(R.id.timer, "0:00");

				if (mediaPlayer != null) {
					mediaPlayer.pause();
				}
			}
		});

		timerThread.start();
	}

	/**
	 * Initialize the song timer loop thread
	 * 
	 */
	private void initSongTimerThread() {
		Thread songTimerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// while we have songs left
				while (currentSong != null) {
					// once the timer runs out, skip
					if (currentSong.timeLeft() == 0) {
						goToNextSong();
					}
				}
			}
		});

		songTimerThread.start();
	}

	/**
	 * Updates a TextView to some text on the UI thread
	 * 
	 * @param id
	 *            the id of the TextView to update
	 * @param text
	 *            the text to set the TextView to
	 */
	private void updateUILabel(final int id, final String text) {

		// update UI on its own thread
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView textView = (TextView) findViewById(id);
				textView.setText(text);
			}
		});
	}

	/**
	 * This is the on enter listener- it submits the entered text
	 */
	TextView.OnEditorActionListener submitListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_DOWN) {
				submit(view);
			}
			return true;
		}
	};

}
