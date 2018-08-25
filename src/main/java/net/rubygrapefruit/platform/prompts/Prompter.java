package net.rubygrapefruit.platform.prompts;

import net.rubygrapefruit.platform.TerminalInput;
import net.rubygrapefruit.platform.TerminalInputListener;
import net.rubygrapefruit.platform.TerminalOutput;
import net.rubygrapefruit.platform.Terminals;

import java.util.List;

/**
 * Displays prompts on the terminal to ask the user various kinds of questions.
 */
public class Prompter {
    private static final TerminalOutput.Color SELECTION_COLOR = TerminalOutput.Color.Cyan;
    private static final TerminalOutput.Color DEFAULT_VALUE_COLOR = TerminalOutput.Color.White;
    private static final TerminalOutput.Color INFO_COLOR = TerminalOutput.Color.White;
    private final TerminalOutput output;
    private final TerminalInput input;
    private final boolean interactive;

    public Prompter(Terminals terminals) {
        interactive = terminals.isTerminalInput() && terminals.isTerminal(Terminals.Output.Stdout);
        output = terminals.getTerminal(Terminals.Output.Stdout);
        input = terminals.getTerminalInput();
    }

    /**
     * Returns true if this prompter can ask the user questions.
     */
    public boolean isInteractive() {
        return interactive;
    }

    /**
     * Asks the user to select an option from a list.
     *
     * @return The index of the selected option or null on end of input. Returns the default option when not interactive.
     */
    public Integer select(String prompt, List<String> options, int defaultOption) {
        if (interactive) {
            return selectInteractive(prompt, options, defaultOption);
        } else {
            return defaultOption;
        }
    }

    /**
     * Asks the user to enter some text.
     *
     * @return The text or null on end of input. Returns the default value when not interactive.
     */
    public String enterText(String prompt, String defaultValue) {
        if (interactive) {
            return enterTextInteractive(prompt, defaultValue);
        } else {
            return defaultValue;
        }
    }

    /**
     * Asks the user a yes/no question.
     *
     * @return The selected value or null on end of input. Returns the default value when not interactive.
     */
    public Boolean askYesNo(String prompt, boolean defaultValue) {
        if (interactive) {
            return yesNoInteractive(prompt, defaultValue);
        } else {
            return defaultValue;
        }
    }

    private Boolean yesNoInteractive(String prompt, boolean defaultValue) {
        YesNoView view = new YesNoView(output, prompt, defaultValue);
        view.render();
        input.rawMode();
        YesNoListener listener = new YesNoListener(view);
        while (!listener.finished) {
            input.read(listener);
        }
        input.reset();
        view.close(listener.selected);
        return listener.selected;
    }

    private Integer selectInteractive(String prompt, List<String> options, final int defaultOption) {
        SelectView view = new SelectView(output, prompt, options, defaultOption);
        view.render();
        input.rawMode();
        SelectionListener listener = new SelectionListener(view);
        while (listener.selected == -1) {
            input.read(listener);
        }
        input.reset();
        view.close(listener.selected);
        return listener.selected < 0 ? null : listener.selected;
    }

    private String enterTextInteractive(String prompt, String defaultValue) {
        TextView view = new TextView(output, prompt, defaultValue);
        view.render();
        input.rawMode();
        TextEntryListener listener = new TextEntryListener(view);
        while (!listener.finished) {
            input.read(listener);
        }
        input.reset();
        view.close(listener.entered);
        return listener.entered;
    }

    private static class YesNoView {
        private final TerminalOutput output;
        private final String prompt;
        private final boolean defaultValue;

        YesNoView(TerminalOutput output, String prompt, boolean defaultValue) {
            this.output = output;
            this.prompt = prompt;
            this.defaultValue = defaultValue;
        }

        public void render() {
            output.newline();
            output.hideCursor();
            output.bold().write(prompt).normal().write(" [yn]: ");
            output.foreground(DEFAULT_VALUE_COLOR).write(defaultValue ? "y" : "n").defaultForeground().cursorLeft(1);
            output.showCursor();
        }

        public void close(Boolean selected) {
            output.cursorStartOfLine();
            output.clearToEndOfLine();
            output.write(prompt).write(": ");
            if (selected != null) {
                output.foreground(SELECTION_COLOR);
                output.write(selected ? "yes" : "no");
                output.reset();
            } else {
                output.write("<none>");
            }
            output.newline();
        }
    }

    private static class YesNoListener implements TerminalInputListener {
        final YesNoView view;
        Boolean selected;
        boolean finished;

        YesNoListener(YesNoView view) {
            this.view = view;
        }

        @Override
        public void character(char ch) {
            if (ch == 'y') {
                selected = true;
                finished = true;
            } else if (ch == 'n') {
                selected = false;
                finished = true;
            }
        }

        @Override
        public void controlKey(Key key) {
            if (key == Key.Enter) {
                selected = view.defaultValue;
                finished = true;
            }
        }

        @Override
        public void endInput() {
            finished = true;
        }
    }

    private static class SelectView {
        final TerminalOutput output;
        final String prompt;
        final List<String> options;
        int selected;

        SelectView(TerminalOutput output, String prompt, List<String> options, int defaultOption) {
            this.output = output;
            this.prompt = prompt;
            this.options = options;
            this.selected = defaultOption;
        }

        void render() {
            output.newline();
            output.hideCursor();
            output.bold().write(prompt).write(":").normal().newline();
            for (int i = 0; i < options.size(); i++) {
                renderItem(i);
            }
            output.foreground(INFO_COLOR)
                    .write("Use the arrow keys to select an option and press enter")
                    .defaultForeground()
                    .cursorStartOfLine();
        }

        private void renderItem(int i) {
            if (i == selected) {
                output.foreground(SELECTION_COLOR);
                output.write("> ");
            } else {
                output.write("  ");
            }
            output.write(String.valueOf((i + 1))).write(") ").write(options.get(i));
            output.defaultForeground();
            output.newline();
        }

        void selectPrevious() {
            if (selected == 0) {
                return;
            }
            selected--;
            int rowsToMoveUp = options.size() - selected;
            output.cursorUp(rowsToMoveUp);
            renderItem(selected);
            renderItem(selected + 1);
            output.cursorDown(rowsToMoveUp - 2);
        }

        void selectNext() {
            if (selected == options.size() - 1) {
                return;
            }
            selected++;
            int rowsToModeUp = options.size() - selected + 1;
            output.cursorUp(rowsToModeUp);
            renderItem(selected - 1);
            renderItem(selected);
            output.cursorDown(rowsToModeUp - 2);
        }

        void close(int selected) {
            output.clearToEndOfLine();
            for (int i = 0; i < options.size(); i++) {
                output.cursorUp(1).clearToEndOfLine();
            }
            output.cursorUp(1);
            output.write(prompt)
                    .write(": ");
            if (selected >= 0) {
                output.foreground(SELECTION_COLOR)
                        .write(options.get(selected))
                        .reset();
            } else {
                output.write("<none>");
            }
            output.showCursor();
            output.newline();
        }
    }

    private static class SelectionListener implements TerminalInputListener {
        private final SelectView view;
        int selected;

        SelectionListener(SelectView view) {
            this.view = view;
            selected = -1;
        }

        @Override
        public void character(char ch) {
            if (Character.isDigit(ch)) {
                int index = ch - '0' - 1;
                if (index >= 0 && index < view.options.size()) {
                    this.selected = index;
                }
            }
        }

        @Override
        public void controlKey(Key key) {
            if (key == Key.Enter) {
                selected = view.selected;
            } else if (key == Key.UpArrow) {
                view.selectPrevious();
            } else if (key == Key.DownArrow) {
                view.selectNext();
            }
        }

        @Override
        public void endInput() {
            selected = -2;
        }
    }

    private static class TextView {
        private final TerminalOutput output;
        private final String prompt;
        private final String defaultValue;
        private StringBuilder value = new StringBuilder();
        private int insertPos = 0;
        private int cursor = 0;

        TextView(TerminalOutput output, String prompt, String defaultValue) {
            this.output = output;
            this.prompt = prompt;
            this.defaultValue = defaultValue;
        }

        public void render() {
            output.newline();
            output.hideCursor();
            output.bold().write(prompt).write(": ").normal();
            output.foreground(DEFAULT_VALUE_COLOR);
            output.write(defaultValue);
            output.cursorLeft(defaultValue.length());
            output.reset();
        }

        void update() {
            output.hideCursor();
            output.cursorLeft(cursor);
            output.clearToEndOfLine();
            if (value.length() == 0) {
                output.foreground(DEFAULT_VALUE_COLOR);
                output.write(defaultValue);
                output.cursorLeft(defaultValue.length() - insertPos);
            } else {
                output.foreground(SELECTION_COLOR);
                output.write(value.toString());
                output.cursorLeft(value.length() - insertPos);
            }
            output.reset();
            cursor = insertPos;
        }

        public void insert(char ch) {
            value.insert(insertPos, ch);
            insertPos++;
            update();
        }

        public void eraseBack() {
            if (insertPos == 0) {
                return;
            }
            value.deleteCharAt(insertPos - 1);
            insertPos--;
            update();
        }

        public void eraseForward() {
            if (insertPos == value.length()) {
                return;
            }
            value.deleteCharAt(insertPos);
            update();
        }

        public void cursorStart() {
            insertPos = 0;
            output.cursorLeft(cursor);
            cursor = 0;
        }

        public void cursorEnd() {
            insertPos = value.length();
            output.cursorRight(insertPos - cursor);
            cursor = insertPos;
        }

        public void cursorLeft() {
            if (insertPos == 0) {
                return;
            }
            insertPos--;
            cursor--;
            output.cursorLeft(1);
        }

        public void cursorRight() {
            if (insertPos == value.length()) {
                return;
            }
            insertPos++;
            cursor++;
            output.cursorRight(1);
        }

        public void close(String entered) {
            output.cursorStartOfLine();
            output.clearToEndOfLine();
            output.write(prompt).write(": ");
            if (entered != null) {
                output.foreground(SELECTION_COLOR);
                output.write(entered);
                output.reset();
            } else {
                output.write("<none>");
            }
            output.newline();
        }
    }

    private static class TextEntryListener implements TerminalInputListener {
        final TextView view;
        String entered;
        boolean finished;

        TextEntryListener(TextView view) {
            this.view = view;
        }

        @Override
        public void character(char ch) {
            view.insert(ch);
        }

        @Override
        public void controlKey(Key key) {
            if (key == Key.Enter) {
                if (view.value.length() == 0) {
                    entered = view.defaultValue;
                } else {
                    entered = view.value.toString();
                }
                finished = true;
            } else if (key == Key.EraseBack) {
                view.eraseBack();
            } else if (key == Key.EraseForward) {
                view.eraseForward();
            } else if (key == Key.LeftArrow) {
                view.cursorLeft();
            } else if (key == Key.RightArrow) {
                view.cursorRight();
            } else if (key == Key.Home) {
                view.cursorStart();
            } else if (key == Key.End) {
                view.cursorEnd();
            }
        }

        @Override
        public void endInput() {
            finished = true;
        }
    }
}
