/*
 * Copyright (c) 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#include <jni.h>
#include <node.h>
#include "v8.h"

using namespace v8;

void Init(Local<Object> target)
{
}

NODE_MODULE_CONTEXT_AWARE(caramlconsole,Init)

extern "C" void JNICALL Java_org_liquidplayer_addon_Caramlconsole_register(JNIEnv* env, jobject thiz)
{
    _register_caramlconsole();
}
