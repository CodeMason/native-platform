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
import net.rubygrapefruit.platform.TerminalSize;
import net.rubygrapefruit.platform.Terminals;
import net.rubygrapefruit.platform.internal.jni.PosixTerminalFunctions;
import net.rubygrapefruit.platform.internal.jni.TerminfoFunctions;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class TerminfoTerminal extends AbstractTerminal {
    private final Terminals.Output output;
    private final TerminalCapabilities capabilities = new TerminalCapabilities();
    private final OutputStream outputStream;
    private final Object lock = new Object();
    private Map<Color, byte[]> foregroundColors = new HashMap<Color, byte[]>();
    private byte[] bold;
    private byte[] reset;
    private byte[] up;
    private byte[] down;
    private byte[] left;
    private byte[] right;
    private byte[] startLine;
    private byte[] clearEOL;
    private Color foreground;

    public TerminfoTerminal(Terminals.Output output) {
        this.output = output;
        this.outputStream = new FileOutputStream(output == Terminals.Output.Stdout ? FileDescriptor.out : FileDescriptor.err);
    }

    @Override
    public String toString() {
        return String.format("Curses terminal %s on %s", capabilities.terminalName, getOutputDisplay());
    }

    private String getOutputDisplay() {
        return output.toString().toLowerCase();
    }

    @Override
    protected void init() {
        synchronized (lock) {
            FunctionResult result = new FunctionResult();
            TerminfoFunctions.initTerminal(output.ordinal(), capabilities, result);
            if (result.isFailed()) {
                throw new NativeException(String.format("Could not open terminal for %s: %s", getOutputDisplay(), result.getMessage()));
            }
            reset();
        }
    }

    public TerminalSize getTerminalSize() {
        synchronized (lock) {
            MutableTerminalSize terminalSize = new MutableTerminalSize();
            FunctionResult result = new FunctionResult();
            PosixTerminalFunctions.getTerminalSize(output.ordinal(), terminalSize, result);
            if (result.isFailed()) {
                throw new NativeException(String.format("Could not get terminal size for %s: %s", getOutputDisplay(), result.getMessage()));
            }
            return terminalSize;
        }
    }

    public boolean supportsColor() {
        return capabilities.colors;
    }

    public boolean supportsCursorMotion() {
        return capabilities.cursorMotion;
    }

    public boolean supportsTextAttributes() {
        return capabilities.textAttributes;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public Terminal foreground(Color color) {
        if (!capabilities.colors) {
            return this;
        }

        synchronized (lock) {
            byte[] sequence = foregroundColors.get(color);
            if (sequence == null) {
                FunctionResult result = new FunctionResult();
                sequence = TerminfoFunctions.foreground(color.ordinal(), result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not switch foreground color for %s: %s", getOutputDisplay(),
                            result.getMessage()));
                }
                foregroundColors.put(color, sequence);
            }
            write(sequence);
            foreground = color;
        }
        return this;
    }

    public Terminal bold() {
        if (!capabilities.textAttributes) {
            return this;
        }

        synchronized (lock) {
            if (bold == null) {
                FunctionResult result = new FunctionResult();
                bold = TerminfoFunctions.bold(result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not switch to bold mode for %s: %s", getOutputDisplay(),
                            result.getMessage()));
                }
            }
            write(bold);
        }
        return this;
    }

    public Terminal normal() {
        synchronized (lock) {
            reset();
            if (foreground != null) {
                foreground(foreground);
            }
        }
        return this;
    }

    public Terminal reset() {
        synchronized (lock) {
            if (reset == null) {
                FunctionResult result = new FunctionResult();
                reset = TerminfoFunctions.reset(result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not reset terminal for %s: %s", getOutputDisplay(), result.getMessage()));
                }
                if (reset == null) {
                    reset = new byte[0];
                }
            }
            write(reset);
        }
        return this;
    }

    public Terminal cursorDown(int count) {
        synchronized (lock) {
            if (down == null) {
                FunctionResult result = new FunctionResult();
                down = TerminfoFunctions.down(result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not move cursor down for %s: %s", getOutputDisplay(), result.getMessage()));
                }
            }
            for (int i = 0; i < count; i++) {
                write(down);
            }
        }
        return this;
    }

    public Terminal cursorUp(int count) {
        synchronized (lock) {
            if (up == null) {
                FunctionResult result = new FunctionResult();
                up = TerminfoFunctions.up(result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not move cursor up for %s: %s", getOutputDisplay(), result.getMessage()));
                }
            }
            for (int i = 0; i < count; i++) {
                write(up);
            }
        }
        return this;
    }

    public Terminal cursorLeft(int count) {
        synchronized (lock) {
            if (left == null) {
                FunctionResult result = new FunctionResult();
                left = TerminfoFunctions.left(result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not move cursor left for %s: %s", getOutputDisplay(), result.getMessage()));
                }
            }
            for (int i = 0; i < count; i++) {
                write(left);
            }
        }
        return this;
    }

    public Terminal cursorRight(int count) {
        synchronized (lock) {
            if (right == null) {
                FunctionResult result = new FunctionResult();
                right = TerminfoFunctions.right(result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not move cursor right for %s: %s", getOutputDisplay(), result.getMessage()));
                }
            }
            for (int i = 0; i < count; i++) {
                write(right);
            }
        }
        return this;
    }

    public Terminal cursorStartOfLine() throws NativeException {
        synchronized (lock) {
            if (startLine == null) {
                FunctionResult result = new FunctionResult();
                startLine = TerminfoFunctions.startLine(result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not move cursor to start of line for %s: %s", getOutputDisplay(), result.getMessage()));
                }
            }
            write(startLine);
        }
        return this;
    }

    public Terminal clearToEndOfLine() throws NativeException {
        synchronized (lock) {
            if (clearEOL == null) {
                FunctionResult result = new FunctionResult();
                clearEOL = TerminfoFunctions.clearToEndOfLine(result);
                if (result.isFailed()) {
                    throw new NativeException(String.format("Could not clear to end of line for %s: %s", getOutputDisplay(), result.getMessage()));
                }
            }
            write(clearEOL);
        }
        return this;
    }
}
