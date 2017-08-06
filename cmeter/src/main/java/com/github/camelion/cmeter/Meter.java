/*
 * Copyright 2017 JTS-Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.camelion.cmeter;

/**
 * All implementation should correctly implement {@link #equals} and {@link #hashCode()} methods
 * because we use some approaches based on top of this checks (e.g. reducing duplicates meters)
 *
 * @author Camelion
 * @since 25.07.17
 */
public interface Meter {
    Measurement measure();
}
