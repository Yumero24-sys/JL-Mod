/*
 * Diagnostic helper for physical-keyboard hold problems (BlackBerry 10 Android runtime).
 * TEST BUILD ONLY: shows the last few raw key events in an on-screen toast.
 */

package javax.microedition.lcdui.keyboard;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import javax.microedition.util.ContextHolder;

public final class KeyDebug {
	/** Show raw key events on screen (and in logcat, tag "KeyDebug"). */
	public static final boolean ENABLED = true;
	/** Generate keyRepeated() ourselves while a key is held and the system sends no repeats. */
	public static final boolean SYNTH_REPEAT = true;
    	/** Stop the game view from declaring itself a text editor (BB10 then sends raw key down/up). */
	public static final boolean NO_TEXT_EDITOR = true;
	/** Delay before the first synthetic repeat, ms. */
	public static final long SYNTH_DELAY = 450;
	/** Interval between synthetic repeats, ms. */
	public static final long SYNTH_INTERVAL = 100;
	/** Safety cap so a lost ACTION_UP can never repeat forever, ms. */
	public static final long SYNTH_MAX = 20000;

	private static final String TAG = "KeyDebug";
	private static final int LINES = 7;

	private static final Handler HANDLER = new Handler(Looper.getMainLooper());
	private static final String[] BUF = new String[LINES];
	private static int count;
	private static long lastArrival;
	private static Toast toast;

	private KeyDebug() {
	}

	/** Logs a raw Android key event exactly as it arrives, before any mapping. */
	public static void log(KeyEvent e) {
		String action;
		switch (e.getAction()) {
			case KeyEvent.ACTION_DOWN:
				action = "DOWN";
				break;
			case KeyEvent.ACTION_UP:
				action = "UP";
				break;
			case KeyEvent.ACTION_MULTIPLE:
				action = "MULT";
				break;
			default:
				action = "A" + e.getAction();
		}
		logText(action + " kc=" + e.getKeyCode() + " rc=" + e.getRepeatCount()
				+ " fl=0x" + Integer.toHexString(e.getFlags())
				+ " src=0x" + Integer.toHexString(e.getSource()));
	}

	public static void logText(String text) {
		long now = SystemClock.uptimeMillis();
		final String line;
		synchronized (KeyDebug.class) {
			long dt = lastArrival == 0 ? 0 : now - lastArrival;
			lastArrival = now;
			line = "+" + dt + "ms " + text;
			BUF[count % LINES] = line;
			count++;
		}
		Log.d(TAG, line);
		HANDLER.post(KeyDebug::show);
	}

	private static void show() {
		StringBuilder sb = new StringBuilder();
		synchronized (KeyDebug.class) {
			int start = Math.max(0, count - LINES);
			for (int i = start; i < count; i++) {
				sb.append(i - start == 0 ? "" : "\n").append(BUF[i % LINES]);
			}
		}
		try {
			if (toast == null) {
				toast = Toast.makeText(ContextHolder.getAppContext(), "", Toast.LENGTH_SHORT);
			}
			toast.setText(sb.toString());
			toast.show();
		} catch (RuntimeException e) {
			Log.w(TAG, "toast failed", e);
		}
	}
}