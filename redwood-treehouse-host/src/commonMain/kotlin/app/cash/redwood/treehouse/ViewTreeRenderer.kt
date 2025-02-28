/*
 * Copyright (C) 2023 Square, Inc.
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
package app.cash.redwood.treehouse

import app.cash.redwood.protocol.EventSink
import app.cash.redwood.protocol.ViewTree
import app.cash.redwood.protocol.widget.ProtocolBridge
import app.cash.redwood.protocol.widget.ProtocolMismatchHandler
import app.cash.redwood.protocol.widget.ProtocolNode
import app.cash.redwood.widget.Widget
import kotlinx.serialization.json.Json

/**
 * Renders a ViewTree snapshot into a target view by creating all of the view tree's widgets and
 * assigning all of its properties.
 *
 * The rendered widgets are not interactive.
 */
public class ViewTreeRenderer<W : Any>(
  private val json: Json,
) {
  private val refuseAllEvents = EventSink { event ->
    throw IllegalStateException("unexpected event: $event")
  }

  @Suppress("UNCHECKED_CAST")
  public fun render(
    view: TreehouseView,
    viewTree: ViewTree,
  ) {
    view.reset()
    val bridge = ProtocolBridge(
      container = view.children as Widget.Children<W>,
      factory = view.widgetSystem.widgetFactory(
        json,
        ProtocolMismatchHandler.Throwing,
      ) as ProtocolNode.Factory<W>,
      eventSink = refuseAllEvents,
    )
    bridge.sendChanges(viewTree.changes)
  }
}
