package me.proxer.app.util.listener

import android.text.Editable
import android.text.TextWatcher

/**
 * @author Ruben Gees
 */
interface TextWatcherWrapper : TextWatcher {
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable) {}
}
