/*
 * Copyright 2012 Adam Murdoch
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.rubygrapefruit.platform.internal;

import net.rubygrapefruit.platform.NativeException;
import net.rubygrapefruit.platform.Terminal;

import java.io.IOException;

public abstract class AbstractTerminal implements Terminal {
    protected static byte[] NEW_LINE = System.getProperty("line.separator").getBytes();

    protected abstract void init();

    @Override
    public Terminal newLine() throws NativeException {
        write(NEW_LINE);
        return this;
    }

    public Terminal write(String text) throws NativeException {
        byte[] bytes = text.getBytes();
        write(bytes);
        return this;
    }

    protected void write(byte[] bytes) {
        try {
            getOutputStream().write(bytes);
        } catch (IOException e) {
            throw new NativeException("Could not write to output stream.", e);
        }
    }
}
