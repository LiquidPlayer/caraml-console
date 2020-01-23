caraml
------

[![Download](https://img.shields.io/npm/dt/@liquidcore/caraml-console.svg)](https://www.npmjs.com/package/@liquidcore/caraml-console)

[![NPM](https://nodei.co/npm/@liquidcore/caraml-console.png)](https://nodei.co/npm/@liquidcore/caraml-console)

caraml is a native mobile UI markup language designed for running native micro-apps on Android and iOS
from node.js instances.  It is built on top of [LiquidCore](https://github.com/LiquidPlayer/LiquidCore), a
library which provides node-based virtual machines on mobile devices.

caraml is very much a work in progress.

caraml-console
-----------

caraml-console is a UI surface for use with caraml. It provides an ANSI-compatible Node.js console
that can interact with a LiquidCore micro service. This surface is most useful for debugging.

To integrate, clients must instantiate a `CaramlView` as described in the [`caraml-core`](https://github.com/LiquidPlayer/caraml-core`) project.

### Step 1: Install LiquidCore

Follow the [directions for installing LiquidCore from `npm`](https://github.com/LiquidPlayer/LiquidCore/blob/master/README.md#installation).

### Step 2: Install caraml-console

```bash
$ npm install @liquidcore/caraml-console
$ npm install
```

The second `npm install` triggers a post-install script in your project that will automatically set up `caraml-console` into your build.


JavaScript API
--------------

## caraml Console

The `caraml-console` module provides an ANSI-compatible console surface for use with LiquidCore and caraml. 
It can be accessed using:

```javascript
const Console = require('@liquidcore/caraml-console')
```

Example usage:

```javascript
const core = require('@liquidcore/caraml-core')
const Console = require('@liquidcore/caraml-console')

// Stop process from exiting
setInterval(()=>{},1000)

let cons = new Console({
  fontSize : 11,
  backgroundColor: 'black',
  textColor: '#32cd32'
})

cons.display(core)
.then(()=>{
  console.error('Welcome to caraml-console')
  console.log('Enter javascript commands and play around.')
  console.log()
})
.catch(e => {
  console.error(e)
})
```

### Class Console

The Console class can be constructed with `new`, for example:

```javascript
var cons = new Console()
```

The Console class instance represents a console view that can be attached and detached to a caraml-core
view.

#### Event: 'ready'

Emitted when the console view has been created and is ready to be attached to a caraml-core view.

#### Event: 'attached'

Emitted when the console view has been attached to a caraml-core view.

#### Event: 'detached'

Emitted when the console view has been detached from a caraml-core view.

#### Event: 'error'

* `error <string>` A human-readable error string

#### new Console([opts])

* `opts <object>` An optional object containing configuration parameters:
  * `textColor <string>` An html-like description of text color, e.g. `'black'` or `'#ed5616'`
  * `backgroundColor <string>` An html-like description of background color
  * `fontSize <number>` A floating-point font point size
  * `transformStdout(output <string>) <function>` A function to transform `output` string being sent to `stdout`, e.g. to add coloring with ANSI tags.  Must return type `<string>`
  * `transformStderr(output <string>) <function>` A function to transform `output` strings being sent to `stderr`

Creates a new console that will redirect `stderr` and `stdout` through it.  Multiple consoles will be chained, i.e.
the output streams will pass through all console instances.

#### cons.display([core-object])

* `core-object <object>` A caraml-core view object obtained through `var core = require('@liquidcore/caraml-core')`
* Returns `<promise>` which is resolved when the view is attached, or rejected with an error string

Requests the console to be displayed (attached) on a caraml-core view.  If no `core-object` is provided, the default
core view will be used.

#### cons.hide()

* Returns `<promise>` which is resolved when the view is detached, or rejected with an error string

Detaches a previously `display`ed console.

#### cons.getParent()

* Returns `<object>`, the caraml-core view to which the console is attached or `undefined` if not attached

#### cons.getState()

* Returns `<string>`, the current state of the console, one of:
  * `'init'`
  * `'detached'`
  * `'attaching'`
  * `'attached'`
  * `'detaching'`
