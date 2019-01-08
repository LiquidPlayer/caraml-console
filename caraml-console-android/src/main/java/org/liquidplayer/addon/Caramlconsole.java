/*
 * Copyright (c) 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
package org.liquidplayer.addon;

import android.content.Context;

import org.liquidplayer.caraml.BuildConfig;
import org.liquidplayer.caraml.console.ConsoleJS;
import org.liquidplayer.javascript.JSFunction;
import org.liquidplayer.javascript.JSObject;
import org.liquidplayer.javascript.JSValue;
import org.liquidplayer.service.AddOn;

@SuppressWarnings("unused")
public class Caramlconsole implements AddOn {
    public Caramlconsole(Context androidContext) {
        this.androidContext = androidContext;
    }

    @Override
    public void register(String module) {
        if (BuildConfig.DEBUG && !module.equals("caramlconsole")) { throw new AssertionError(); }
        System.loadLibrary("caraml-console.node");

        register();
    }

    @Override
    public void require(final JSValue binding) {
        if (BuildConfig.DEBUG && (binding == null || !binding.isObject())) {
            throw new AssertionError();
        }

        JSObject bindingObject = binding.toObject();
        bindingObject.property("newConsole",
        new JSFunction(binding.getContext(), "newConsole") {
            @SuppressWarnings("unused") public
            JSObject newConsole(JSValue opts) {
                return new ConsoleJS(androidContext, binding.getContext(), opts);
            }
        });
    }

    private final Context androidContext;

    native static void register();
}
