package info.staticfree.android.units;
/*
 * WindowInsetsHelper.java
 * Copyright (C) 2026  Steve Pomeroy <steve@staticfree.info>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Android 15 (API 35) and up always lay activities out edge to edge, and ignore
 * {@code adjustResize} for the soft keyboard. Rather than restyling the app,
 * keep the old look by padding the content view in by whatever the system bars,
 * display cutout and IME are covering.
 */
final class WindowInsetsHelper {

	private WindowInsetsHelper() {
	}

	/**
	 * Pads the activity's content view so nothing lands underneath the system
	 * bars, a display cutout or the soft keyboard.
	 *
	 * @param activity the activity whose content view should be inset
	 */
	static void padForSystemBars(Activity activity) {
		final View content = activity.findViewById(android.R.id.content);

		ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
			final Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
					| WindowInsetsCompat.Type.displayCutout()
					| WindowInsetsCompat.Type.ime());

			v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

			return WindowInsetsCompat.CONSUMED;
		});
	}
}
