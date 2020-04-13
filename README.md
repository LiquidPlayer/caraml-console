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

To integrate, clients must instantiate a `CaramlView` as described in the [`caraml-core`](https://github.com/LiquidPlayer/caraml-core) project.

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

<a name="Console"></a>

## Console
The [Console](#Console) class instance represents a console view that can be attached and detached to a caraml-core
 view.

**Kind**: global class  

* [Console](#Console)
    * [new Console([opts])](#new_Console_new)
    * [.display([caramlview])](#Console+display) ⇒ <code>Promise</code>
    * [.hide()](#Console+hide) ⇒ <code>Promise</code>
    * [.getParent()](#Console+getParent) ⇒ <code>Object</code>
    * [.getState()](#Console+getState) ⇒ <code>string</code>
    * ["ready"](#Console+event_ready)
    * ["attached"](#Console+event_attached)
    * ["detached"](#Console+event_detached)
    * ["error"](#Console+event_error)
    * [.Transform](#Console+Transform) ⇒ <code>String</code>

<a name="new_Console_new"></a>

### new Console([opts])
Creates a new console that will redirect stderr and stdout through it. Multiple consoles will
   be chained, i.e. the output streams will pass through all console instances.


| Param | Type | Description |
| --- | --- | --- |
| [opts] | <code>Object</code> |  |
| [opts.textColor] | <code>string</code> | An html-like description of text color, e.g. 'black' or '#ed5616' |
| [opts.backgroundColor] | <code>string</code> | An html-like description of background color |
| [opts.fontSize] | <code>number</code> | A floating-point font point size |
| [opts.transformStdout] | [<code>Transform</code>](#Console+Transform) | A function to transform output string being sent to stdout |
| [opts.transformStderr] | [<code>Transform</code>](#Console+Transform) | A function to transform output strings being sent to stderr |

<a name="Console+display"></a>

### console.display([caramlview]) ⇒ <code>Promise</code>
Requests the console to be displayed (attached) on a caraml-core view. If no caramlview is provided,
     the default core view will be used.

**Kind**: instance method of [<code>Console</code>](#Console)  
**Returns**: <code>Promise</code> - Promise which is resolved when the view is attached, or rejected with an error string  

| Param | Type | Description |
| --- | --- | --- |
| [caramlview] | <code>Object</code> | A caraml-core view object obtained through                                     <code>var core = require('@liquidcore/caraml-core')</code> |

<a name="Console+hide"></a>

### console.hide() ⇒ <code>Promise</code>
Detaches a previously displayed console.

**Kind**: instance method of [<code>Console</code>](#Console)  
**Returns**: <code>Promise</code> - Resolved when the view is attached, or rejected with an error string  
<a name="Console+getParent"></a>

### console.getParent() ⇒ <code>Object</code>
Returns the parent caraml-core view

**Kind**: instance method of [<code>Console</code>](#Console)  
**Returns**: <code>Object</code> - The caraml-core view to which the console is attached or undefined if not attached  
<a name="Console+getState"></a>

### console.getState() ⇒ <code>string</code>
Returns one of 'init', 'detached', 'attaching', 'attached', 'detaching'

**Kind**: instance method of [<code>Console</code>](#Console)  
**Returns**: <code>string</code> - The current state of the console  
<a name="Console+event_ready"></a>

### "ready"
Emitted when the console view has been created and is ready to be attached to a caraml-core view.

**Kind**: event emitted by [<code>Console</code>](#Console)  
<a name="Console+event_attached"></a>

### "attached"
Emitted when the console view has been attached to a caraml-core view.

**Kind**: event emitted by [<code>Console</code>](#Console)  
<a name="Console+event_detached"></a>

### "detached"
Emitted when the console view has been detached from a caraml-core view.

**Kind**: event emitted by [<code>Console</code>](#Console)  
<a name="Console+event_error"></a>

### "error"
Emitted on error

**Kind**: event emitted by [<code>Console</code>](#Console)  

| Type | Description |
| --- | --- |
| <code>string</code> | A human-readable error string |

<a name="Console+Transform"></a>

### console.Transform ⇒ <code>String</code>
A function to transform output string, e.g. to add coloring with ANSI tags.

**Kind**: instance typedef of [<code>Console</code>](#Console)  
**Returns**: <code>String</code> - Transformed string  

| Param | Type | Description |
| --- | --- | --- |
| input | <code>String</code> | Input string |

