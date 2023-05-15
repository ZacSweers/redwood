/*
 * Copyright (C) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.redwood.treehouse.lazylayout.view

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.redwood.LayoutModifier
import app.cash.redwood.treehouse.lazylayout.widget.LazyList
import app.cash.redwood.widget.MutableListChildren
import app.cash.redwood.widget.Widget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

internal class Items<VH : RecyclerView.ViewHolder>(
  private val adapter: RecyclerView.Adapter<VH>,
) : Widget.Children<View> {
  val size: Int
    get() = widgets.size

  internal var itemsBefore = 0
  internal var itemsAfter = 0

  private val _widgets = MutableListChildren<View>()
  val widgets: List<Widget<View>> get() = _widgets

  override fun insert(index: Int, widget: Widget<View>) {
    _widgets.insert(index, widget)
    adapter.notifyItemInserted(itemsBefore + index)
  }

  override fun move(fromIndex: Int, toIndex: Int, count: Int) {
    _widgets.move(fromIndex, toIndex, count)
    check(count == 1)
    // TODO Support arbitrary count.
    adapter.notifyItemMoved(itemsBefore + fromIndex, itemsBefore + toIndex)
  }

  override fun remove(index: Int, count: Int) {
    _widgets.remove(index, count)
    adapter.notifyItemRangeRemoved(itemsBefore + index, count)
  }

  override fun onLayoutModifierUpdated() {
  }
}

internal class ViewLazyList(
  override val value: RecyclerView,
) : LazyList<View> {
  private val scope = MainScope()

  override var layoutModifiers: LayoutModifier = LayoutModifier

  private val linearLayoutManager = LinearLayoutManager(value.context)
  private val adapter = LazyContentItemListAdapter()
  private var onViewportChanged: ((IntArray) -> Unit)? = null
  private var viewport = 0 until 0

  override val items = Items(adapter)

  init {
    adapter.items = items
    value.apply {
      layoutManager = linearLayoutManager
      layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }
    value.adapter = adapter
    value.addOnAttachStateChangeListener(
      object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {}

        override fun onViewDetachedFromWindow(view: View) {
          view.removeOnAttachStateChangeListener(this)
          scope.cancel()
        }
      },
    )
    value.addOnScrollListener(
      object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          updateViewport()
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
          updateViewport()
        }
      },
    )
  }

  override fun isVertical(isVertical: Boolean) {
    linearLayoutManager.orientation = if (isVertical) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
  }

  override fun onViewportChanged(onViewportChanged: (IntArray) -> Unit) {
    this.onViewportChanged = onViewportChanged
  }

  private fun updateViewport() {
    val newViewport = linearLayoutManager.findFirstVisibleItemPosition()..linearLayoutManager.findLastVisibleItemPosition()
    if (newViewport != viewport) {
      this@ViewLazyList.viewport = newViewport
      onViewportChanged?.invoke(intArrayOf(newViewport.first, newViewport.last + 1))
    }
  }

  override fun itemsBefore(itemsBefore: Int) {
    val delta = itemsBefore - items.itemsBefore
    items.itemsBefore = itemsBefore

    if (delta > 0) {
      adapter.notifyItemRangeInserted(itemsBefore - delta, delta)
    } else {
      adapter.notifyItemRangeRemoved(itemsBefore, -delta)
    }
  }

  override fun itemsAfter(itemsAfter: Int) {
    val delta = itemsAfter - items.itemsAfter
    items.itemsAfter = itemsAfter

    val positionStart = items.itemsBefore + items.size
    if (delta > 0) {
      adapter.notifyItemRangeInserted(positionStart, delta)
    } else {
      adapter.notifyItemRangeRemoved(positionStart, -delta)
    }
  }

  private class LazyContentItemListAdapter : RecyclerView.Adapter<ViewHolder>() {
    lateinit var items: Items<ViewHolder>

    override fun getItemCount(): Int = items.itemsBefore + items.widgets.size + items.itemsAfter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
      FrameLayout(parent.context).apply {
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
      },
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val index = position - items.itemsBefore
      val view = if (index !in items.widgets.indices) {
        TextView(holder.itemView.context).apply {
          text = "Placeholder"
        }
      } else {
        items.widgets[index].value
      }
      holder.container.removeAllViews()
      (view.parent as? FrameLayout)?.removeAllViews()
      holder.container.addView(view)
    }
  }

  class ViewHolder(val container: FrameLayout) : RecyclerView.ViewHolder(container)
}
