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
  private val _widgets = MutableListChildren<View>()
  val widgets: List<Widget<View>> get() = _widgets

  var guestItemsBefore = 0
  var guestItemsAfter = 0

  var hostItemsBefore = 0
  var hostItemsAfter = 0

  override fun insert(index: Int, widget: Widget<View>) {
    _widgets.insert(index, widget)

    if (index == 0 && hostItemsBefore > guestItemsBefore) {
      hostItemsBefore-- // An item went from virtual to real.
    } else if (index == _widgets.size - 1 && hostItemsAfter > guestItemsAfter) {
      hostItemsAfter-- // An item went from virtual to real.
    } else {
      adapter.notifyItemInserted(index)
    }
  }

  override fun move(fromIndex: Int, toIndex: Int, count: Int) {
    _widgets.move(fromIndex, toIndex, count)
    check(count == 1)
    adapter.notifyItemMoved(fromIndex, toIndex) // TODO Support arbitrary count.
  }

  override fun remove(index: Int, count: Int) {
    for ( i in 0 until count) { remove(index) }
  }

  private fun remove(index: Int) {
    _widgets.removeAt(index)

    if (index == 0 && hostItemsBefore < guestItemsBefore) {
      hostItemsBefore++ // An item went from real to virtual.
    } else if (index == _widgets.size && hostItemsAfter < guestItemsAfter) {
      hostItemsAfter++ // An item went from real to virtual.
    } else {
      adapter.notifyItemRangeRemoved(index, 1)
    }
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
  }

  override fun isVertical(isVertical: Boolean) {
    linearLayoutManager.orientation = if (isVertical) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
  }

  override fun onPositionDisplayed(onPositionDisplayed: (Int) -> Unit) {
    adapter.onPositionDisplayed = onPositionDisplayed
  }

  override fun itemsBefore(itemsBefore: Int) {
    items.guestItemsBefore = itemsBefore
  }

  override fun itemsAfter(itemsAfter: Int) {
    items.guestItemsAfter = itemsAfter
  }

  private class LazyContentItemListAdapter : RecyclerView.Adapter<ViewHolder>() {
    var onPositionDisplayed: ((Int) -> Unit)? = null

    lateinit var items: Items<ViewHolder>

    override fun getItemCount(): Int = items.widgets.size + items.guestItemsBefore + items.guestItemsAfter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
      FrameLayout(parent.context).apply {
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
      },
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      onPositionDisplayed!!.invoke(position)
      val index = position - items.guestItemsBefore
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
