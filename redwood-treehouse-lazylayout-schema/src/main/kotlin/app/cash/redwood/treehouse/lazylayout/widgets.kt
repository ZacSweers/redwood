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
package app.cash.redwood.treehouse.lazylayout

import app.cash.redwood.schema.Children
import app.cash.redwood.schema.Default
import app.cash.redwood.schema.Property
import app.cash.redwood.schema.Widget

@Widget(1)
public data class LazyList(
  @Property(1) val isVertical: Boolean,
  @Property(2) val onPositionDisplayed: (position: Int) -> Unit,
  @Property(3) @Default("0") val itemsBefore: Int = 0,
  @Property(4) @Default("0") val itemsAfter: Int = 0,
  @Children(1) val items: () -> Unit,
)
