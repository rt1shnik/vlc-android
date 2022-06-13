package org.videolan.vlc.gui

import android.content.res.TypedArray
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Job
import org.videolan.vlc.R
import org.videolan.vlc.gui.view.SwipeRefreshLayout

abstract class BaseFragment : Fragment(), ActionMode.Callback {
    var actionMode: ActionMode? = null
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    open val hasTabs = false

    private var refreshJob : Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    open val subTitle: String?
        get() = null

    val menu: Menu?
        get() = null

    abstract fun getTitle(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<SwipeRefreshLayout>(R.id.swipeLayout)?.let {
            swipeRefreshLayout = it
            val a: TypedArray = requireActivity().obtainStyledAttributes(TypedValue().data, intArrayOf(R.attr.colorPrimary))
            val color = a.getColor(0, 0)
            a.recycle()
            it.setColorSchemeColors(color)
        }
    }

    override fun onStart() {
        super.onStart()
        updateActionBar()
    }

    private fun updateActionBar() {
        val activity = activity as? AppCompatActivity ?: return
        activity.supportActionBar?.let {
            it.subtitle = subTitle
            activity.invalidateOptionsMenu()
        }
    }

    fun stopActionMode() {
        actionMode?.let {
            it.finish()
        }
    }

    fun invalidateActionMode() {
        actionMode?.invalidate()
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
}