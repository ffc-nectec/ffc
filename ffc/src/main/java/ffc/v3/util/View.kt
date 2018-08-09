/*
 * Copyright (c) 2018 NECTEC
 *   National Electronics and Computer Technology Center, Thailand
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ffc.v3.util

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.EditText

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun EditText.notNullOrBlank() = !text.isNullOrBlank()

fun setStatusBarColor (context: Context, color: Int): Int = ContextCompat.getColor(context, color)
/* Use:
    if (Build.VERSION.SDK_INT >= 21)
        window.statusBarColor = setStatusBarColor(this, R.color.overlay_black)
 */
