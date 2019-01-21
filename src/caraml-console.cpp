/*
 * Copyright (c) 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#include "node.h"
#include "v8.h"
#include "caraml-console.h"

using namespace v8;

void Init(Local<Object> target)
{
}

NODE_MODULE_CONTEXT_AWARE(caramlconsole,Init)

void register_caramlconsole()
{
    _register_caramlconsole();
}
