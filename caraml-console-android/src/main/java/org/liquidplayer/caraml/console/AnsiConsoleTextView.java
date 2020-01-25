/*
 * Copyright (c) 2016 - 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
package org.liquidplayer.caraml.console;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Keep;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.text.HtmlCompat;

import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

class AnsiConsoleTextView extends AppCompatTextView {
    public AnsiConsoleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        stream = new ConsoleOutputStream(new ByteArrayOutputStream());
    }

    private final static String UTF8 = "UTF-8";

    public void print(final String string) {
        stream.print(string, false);
    }
    public void println(final String string) {
        stream.print(string, true);
    }

    interface Listener {
        void onDisplayUpdated();
    }

    private ArrayList<Listener> listeners = new ArrayList<>();

    public void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    @SuppressWarnings("unused") @Keep
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private final ConsoleOutputStream stream;

    public void setDisplayText(CharSequence text) {
        if (stream != null) {
            stream.displayText = text;
            setText(text);
            stream.index = stream.displayText.length();
        }
    }

    private class ConsoleOutputStream extends HtmlAnsiOutputStream {

        ConsoleOutputStream(ByteArrayOutputStream byteArrayOutputStream) {
            super(byteArrayOutputStream);
            os = byteArrayOutputStream;
            displayText = getText();
            index = displayText.length();
            consoleThread = new Thread(consoleThreadRunnable);
            consoleThread.start();
        }

        private ByteArrayOutputStream os;

        private int index;
        private CharSequence displayText;

        private Boolean refreshQueued = false;
        private final Object lock = new Object();

        private Runnable updateText = new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    setText(displayText);
                    refreshQueued = false;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        for (Listener listener : listeners) {
                            listener.onDisplayUpdated();
                        }
                    }
                });
            }
        };

        private final Object consoleThreadLock = new Object();
        private final Semaphore consoleSemaphore = new Semaphore(0);
        private final ArrayList<String> consoleStrings = new ArrayList<>();

        private final Thread consoleThread;
        /** @noinspection FieldCanBeLocal */
        private final Runnable consoleThreadRunnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int strings;
                        String out = "";
                        consoleSemaphore.acquire();
                        synchronized (consoleThreadLock) {
                            strings = consoleStrings.size();
                            for (String string : consoleStrings) {
                                out = out.concat(string);
                            }
                            consoleStrings.clear();
                        }
                        try {
                            write(out.getBytes(UTF8));
                            flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (strings > 1) {
                            consoleSemaphore.acquire(strings - 1);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };

        @Override
        public void close() throws IOException {
            super.close();
            consoleThread.interrupt();
        }

        void print(String string, final boolean addCr) {
            if (addCr) string = string.concat("\n");
            synchronized (consoleThreadLock) {
                consoleStrings.add(string);
                consoleSemaphore.release();
            }
        }

        @Override
        public void flush() throws IOException {
            synchronized (lock) {
                CharSequence text_;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    text_ = HtmlCompat.fromHtml(new String(os.toByteArray(), UTF8),
                            HtmlCompat.FROM_HTML_MODE_LEGACY);
                } else {
                    text_ = HtmlCompat.fromHtml(new String(os.toByteArray(), UTF8),
                            HtmlCompat.FROM_HTML_MODE_LEGACY);
                }
                os.reset();

                if (index == displayText.length()) {
                    displayText = TextUtils.concat(displayText, text_);
                } else if (index < displayText.length()) {
                    if (index + text_.length() >= displayText.length()) {
                        displayText = TextUtils.concat(displayText.subSequence(0, index), text_);
                    } else {
                        CharSequence first = displayText.subSequence(0, index);
                        CharSequence last =
                                displayText.subSequence(index+text_.length(), displayText.length());
                        displayText = TextUtils.concat(first, text_, last);
                    }
                } else {
                    android.util.Log.e("Console flush","index is greater than total buffer length");
                    displayText = TextUtils.concat(displayText, text_);
                    index = displayText.length();
                }
                index += text_.length();

                if (!refreshQueued) {
                    refreshQueued = true;
                    new Handler(Looper.getMainLooper()).post(updateText);
                }
            }
        }

        @Override
        protected void processEraseScreen(int eraseOption) throws IOException {
            synchronized (lock) {
                switch(eraseOption) {
                    case ERASE_SCREEN_TO_END:
                        displayText = displayText.subSequence(0,
                                Math.min(index,displayText.length()));
                        break;
                    case ERASE_SCREEN_TO_BEGINING:
                    case ERASE_SCREEN:
                        // FIXME
                        displayText = "";
                        index = 0;
                        break;
                }
            }
            flush();
        }

        @Override
        protected void processCursorTo(int row, int col) throws IOException {
            synchronized (lock) {
                // Move to row 'row'
                index = 0;
                int count = 1;
                String text = displayText.toString();
                while (count < row && index < text.length()) {
                    int next = text.indexOf('\n', index);
                    if (next < 0) break;
                    count++;
                    index = next + 1;
                }

                // Move to column 'col'
                for (count = 1;
                     count < col && index < text.length() && text.charAt(index) != '\n';
                     count++)
                    index++;
            }
            flush();
        }

        private int[] currentCursorPos() {
            int[] pos = {1,1};
            String text = displayText.toString();

            for (int i=0; i<index; i++) {
                if (text.charAt(i) == '\n') {
                    pos[0] ++;
                    pos[1] = 0;
                } else {
                    pos[1] ++;
                }
            }

            return pos;
        }

        @Override
        protected void processCursorToColumn(int x) throws IOException {
            int[] pos = currentCursorPos();
            processCursorTo(pos[0], Math.max(1,x));
        }

        @Override
        protected void processCursorUpLine(int count) throws IOException {
            int[] pos = currentCursorPos();
            processCursorTo(Math.max(pos[0] - count,1), 1);
        }

        @Override
        protected void processCursorDownLine(int count) throws IOException {
            int[] pos = currentCursorPos();
            processCursorTo(pos[0] + count, 1);
        }

        @Override
        protected void processCursorLeft(int count) throws IOException {
            int[] pos = currentCursorPos();
            processCursorTo(pos[0], Math.max(pos[1] - count,1));
        }

        @Override
        protected void processCursorRight(int count) throws IOException {
            int[] pos = currentCursorPos();
            processCursorTo(pos[0], pos[1] + count);
        }

        @Override
        protected void processCursorDown(int count) throws IOException {
            int[] pos = currentCursorPos();
            processCursorTo(pos[0] + count, pos[1]);
        }

        @Override
        protected void processCursorUp(int count) throws IOException {
            int[] pos = currentCursorPos();
            processCursorTo(Math.max(pos[0] - count,1), pos[1]);
        }

        private final ArrayList<Integer[]> cursorPositionStack = new ArrayList<>();

        @Override
        protected void processRestoreCursorPosition() throws IOException {
            if (cursorPositionStack.size() > 0) {
                Integer [] pos = cursorPositionStack.get(cursorPositionStack.size()-1);
                cursorPositionStack.remove(cursorPositionStack.size()-1);

                processCursorTo(pos[0], pos[1]);
            }
        }

        @Override
        protected void processSaveCursorPosition() {
            int [] p = currentCursorPos();
            Integer [] pp = {p[0],p[1]};
            cursorPositionStack.add(pp);
        }

        @Override
        protected void processScrollDown(int optionInt) {
            android.util.Log.d("ConsoleTextView", "processScrollDown");
        }

        @Override
        protected void processScrollUp(int optionInt) {
            android.util.Log.d("ConsoleTextView", "processScrollUp");
        }

        @Override
        protected void processEraseLine(int eraseOption) throws IOException {
            synchronized (lock) {
                switch(eraseOption) {
                    case ERASE_LINE_TO_END: {
                        String dt = displayText.toString();
                        int next = dt.indexOf('\n', index);
                        if (next >= 0) {
                            displayText = TextUtils.concat(displayText.subSequence(0, index)
                                    ,displayText.subSequence(next,dt.length()));
                        } else {
                            displayText = displayText.subSequence(0, index);
                        }
                        break;
                    }
                    case ERASE_LINE_TO_BEGINING: {
                        String dt = displayText.toString();
                        int currIndex = index;
                        int[] currPos = currentCursorPos();
                        processCursorTo(currPos[0], 1);
                        displayText = TextUtils.concat(displayText.subSequence(0, index)
                                , displayText.subSequence(currIndex, dt.length()));
                        break;
                    }
                    case ERASE_LINE: {
                        int[] currPos = currentCursorPos();
                        processCursorTo(currPos[0], 1);
                        processEraseLine(ERASE_LINE_TO_END);
                        break;
                    }
                }
            }
            flush();
        }

    }
}
