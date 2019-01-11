/*
 * Copyright (c) 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
package org.liquidplayer.caraml.console;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import org.liquidplayer.caraml.CaramlJS;
import org.liquidplayer.caraml.CaramlSurface;
import org.liquidplayer.javascript.JSContext;
import org.liquidplayer.javascript.JSException;
import org.liquidplayer.javascript.JSFunction;
import org.liquidplayer.javascript.JSObject;
import org.liquidplayer.javascript.JSValue;

import java.util.HashMap;
import java.util.UUID;

public class ConsoleJS extends JSObject implements CaramlSurface, JSContext.IJSExceptionHandler {
    public ConsoleJS(final Context androidContext, final JSContext context, final JSValue opts) {
        super(context);
        currentState = State.Init;
        JSFunction createEmitter = new JSFunction(context, "createEmitter",
                "let e=new events(); Object.assign(thiz,e); " +
                "thiz.__proto__=e.__proto__", "thiz");
        createEmitter.call(null, this);
        emit = property("emit").toFunction();
        attachPromise = null;
        detachPromise = null;

        uuid = UUID.randomUUID().toString();
        sessionMap.put(uuid, this);

        // defaults
        this.backgroundColor = Color.BLACK;
        this.textColor = Color.GREEN;
        this.fontSize = 12.0f;

        if (opts != null && opts.isObject()) {
            JSObject options = opts.toObject();
            JSValue backgroundColor = options.property("backgroundColor");
            if (backgroundColor.isString())
                this.backgroundColor = Color.parseColor(backgroundColor.toString());
            JSValue textColor = options.property("textColor");
            if (textColor.isString())
                this.textColor = Color.parseColor(textColor.toString());
            JSValue fontSize = options.property("fontSize");
            if (fontSize.isNumber()) {
                this.fontSize = fontSize.toNumber().floatValue();
            }
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                currentView = new ConsoleSurface(androidContext);
                currentView.setSession(ConsoleJS.this);
                currentState = State.Detached;
                context.getGroup().schedule(new Runnable() {
                    @Override
                    public void run() {
                        emit.call(ConsoleJS.this, "ready");
                    }
                });
            }
        });
    }

    /*--
    /* JavaScript API
    /*--*/

    @jsexport @SuppressWarnings("unused")
    JSObject attach(JSValue value) {
        attachPromise = getContext().evaluateScript(createPromiseObject).toObject();
        try {
            if (value == null)
                throw new RuntimeException("attach: first argument must be a caraml object");
            if (currentState != State.Detached)
                throw new RuntimeException("attach: must be in detached state");
            caramlJS = CaramlJS.from(value);
            currentState = State.Attaching;
            caramlJS.attach(this);
        } catch (RuntimeException e) {
            onError(e);
        }

        return attachPromise.property("promise").toObject();
    }

    @jsexport @SuppressWarnings("unused")
    String state() {
        switch (currentState) {
            case Init:      return "init";
            case Attaching: return "attaching";
            case Attached:  return "attached";
            case Detaching: return "detaching";
            case Detached:  return "detached";
        }
        if (BuildConfig.DEBUG) throw new AssertionError();
        return "error";
    }

    @jsexport @SuppressWarnings("unused")
    JSObject detach() {
        detachPromise = getContext().evaluateScript(createPromiseObject).toObject();
        JSObject promise = detachPromise.property("promise").toObject();
        if (currentState == State.Detached) {
            emit.call(this, "detached");
            detachPromise.property("resolve").toFunction().call(null);
            detachPromise = null;
        } else if (currentState == State.Attached) {
            if (BuildConfig.DEBUG && caramlJS == null) throw new AssertionError();
            currentState = State.Detaching;
            caramlJS.detach();
        } else {
            detachPromise.property("reject").toFunction().
                    call(null, "Attach/detach pending");
            detachPromise = null;
        }
        return promise;
    }

    @jsexport @SuppressWarnings("unused")
    void write(String out) {
        if (currentView != null && out != null) currentView.print(out);
    }

    /*--
    /* CaramlSurface implementation
    /*--*/

    @Override
    public View getView() {
        return currentView;
    }

    @Override
    public void onAttached(boolean fromRestore) {
        currentState = State.Attached;
        getContext().setExceptionHandler(this);
        console_log = getContext().property("console").toObject().property("log").toFunction();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (currentView != null) {
                    currentView.inputBox.setEnabled(true);
                }
            }
        });

        if (!fromRestore) {
            if (attachPromise != null) {
                attachPromise.property("resolve").toFunction().call(null);
            }
            emit.call(this, "attached");
            attachPromise = null;
        }
    }

    @Override
    public void onDetached() {
        currentState = State.Detached;

        console_log = null;
        if (uuid != null) {
            sessionMap.remove(uuid);
        }

        if (currentView != null) currentView.detach();
        caramlJS = null;

        emit.call(this, "detached");
        if (detachPromise != null) {
            detachPromise.property("resolve").toFunction().call(null);
        }
        detachPromise = null;
    }

    @Override
    public void onError(Exception e) {
        if (currentState != State.Init) {
            currentState = State.Detached;
        }
        JSObject promise;
        if (attachPromise != null) promise = attachPromise;
        else promise = detachPromise;

        if (promise != null) {
            promise.property("reject").toFunction().call(null, e.getMessage());
        }
        emit.call(this, "error", e.getMessage());
        attachPromise = detachPromise = null;
    }

    /*--
    /* Console session
    /*--*/

    static ConsoleJS getSessionFromUUID(String id) {
        return sessionMap.get(id);
    }

    String getSessionUUID() { return uuid; }

    void setCurrentView(ConsoleSurface view) {
        currentView = view;
    }

    int getBackgroundColor() {
        return backgroundColor;
    }

    int getTextColor() {
        return textColor;
    }

    float getFontSize() {
        return fontSize;
    }

    void removeCurrentView(ConsoleSurface view) {
        if (currentView == view) currentView = null;
    }

    void processCommand(final String cmd) {
        processedException = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSValue output = getContext().evaluateScript(cmd);
                if (!processedException && console_log != null) {
                    console_log.call(null, output);
                }
            }
        }).start();
    }

    void resize(final int columns, final int rows) {
        getContext().getGroup().schedule(new Runnable() {
            @Override
            public void run() {
                JSValue resize = property("resize");
                if (resize.isObject() && resize.toObject().isFunction()) {
                    resize.toFunction().call(null, columns, rows);
                }
            }
        });
    }

    /*--
    /* IJSExceptionHandler implementation
    /*--*/

    @Override
    public void handle(final JSException e) {
        processedException = true;
        if (currentView != null) {
            currentView.println("\u001b[31m" + e.stack());
        }
    }

    /*--
    /* private statics
    /*--*/

    private static final String createPromiseObject =
        "(()=>{" +
        "  var po = {}; var clock = true;" +
        "  var timer = setInterval(()=>{if(!clock) clearTimeout(timer);}, 100); "+
        "  po.promise = new Promise((resolve,reject)=>{po.resolve=resolve;po.reject=reject});" +
        "  po.promise.then(()=>{clock=false}).catch(()=>{clock=false});" +
        "  return po;" +
        "})();";
    private static HashMap<String,ConsoleJS> sessionMap = new HashMap<>();

    /*--
    /* session privates
    /*--*/

    private final JSFunction emit;
    private final String uuid;
    private JSObject attachPromise;
    private JSObject detachPromise;
    private ConsoleSurface currentView;
    private JSFunction console_log = null;
    private boolean processedException = false;
    private CaramlJS caramlJS;
    private int backgroundColor;
    private int textColor;
    private float fontSize;

    private enum State {
        Init,
        Attaching,
        Attached,
        Detaching,
        Detached,
    }
    private State currentState;
}
