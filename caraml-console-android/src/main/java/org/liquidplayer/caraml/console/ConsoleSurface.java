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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

/**
 * A ConsoleSurface is a node.js ANSI text console.  ConsoleSurface operates by manipulating
 * the 'process' object in node.  It captures output written to stdout and stderr as well as
 * traps and displays any JavaScript exceptions.
 *
 * So long as the underlying MicroService is still running, ConsoleSurface can inject javascript
 * into a running process through a command line.
 *
 * ConsoleSurface is intended to be used mostly for debugging.
 */
public class ConsoleSurface extends ConsoleView {

    public ConsoleSurface(Context context) {
        this(context, null);
    }

    public ConsoleSurface(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConsoleSurface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSaveEnabled(true);
    }

    private ConsoleJS session = null;

    @Override
    public void reset() {
        detach();
        super.reset();
    }

    @Override
    protected void resize(int columns, int rows) {
        if (session != null) {
            session.resize(columns,rows);
        }
    }

    void setSession(ConsoleJS session) {
        uuid = session.getSessionUUID();
        this.session = session;
    }

    void detach() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                inputBox.setEnabled(false);
                inputBox.setClickable(false);
                inputBox.setAlpha(0.5f);
                setButtonEnabled(downHistory,false);
                setButtonEnabled(upHistory,false);
            }
        });
    }

    @Override
    protected void processCommand(final String cmd) {
        if (session != null) {
            session.processCommand(cmd);
        }
    }

    private String temp = "";
    void print(String str) {
        if (consoleTextView == null) {
            temp = temp + "\u001b[m" + str;
        } else {
            if (temp.length() > 0) {
                consoleTextView.print(temp);
                temp = "";
            }
            consoleTextView.print(str);
        }
    }
    void println(String str) {
        print(str + "\n");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        session.setCurrentView(this);

        inputBox.setTextSize(session.getFontSize());
        inputBox.setTextColor(session.getTextColor());
        setBackgroundColor(session.getBackgroundColor());
        consoleTextView.setTextSize(session.getFontSize());
        consoleTextView.setTextColor(session.getTextColor());

        if (temp.length() > 0) {
            consoleTextView.print(temp);
            temp = "";
        }
    }

    /* -- parcelable privates -- */
    private String uuid;

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.uuid = uuid;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        uuid = ss.uuid;
        session = ConsoleJS.getSessionFromUUID(uuid);
    }

    static class SavedState extends BaseSavedState {
        String uuid;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            uuid = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(uuid);
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
