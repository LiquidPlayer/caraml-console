/*
 * Copyright (c) 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
/**
 @file
 @author Eric Lange<eric@flicket.tv>
 @description
 The caraml-console module provides an ANSI-compatible console surface for use with LiquidCore
 and caraml. It can be accessed using:
 <code><pre>
 const Console = require('@liquidcore/caraml-console')
 </pre></code>

 Example usage:
 <code><pre>
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
 </pre></code>
*/
const bindings = require('bindings')
const native = bindings('caramlconsole')
const events = require('events')


/**
 * A function to transform output string, e.g. to add coloring with ANSI tags.
 * @typedef {Function} Console#Transform
 * @param {String} input - Input string
 * @returns {String} Transformed string
*/

/**
 The {@link Console} class instance represents a console view that can be attached and detached to a caraml-core
 view.
*/
class Console extends events {
  /**
   Creates a new console that will redirect stderr and stdout through it. Multiple consoles will
   be chained, i.e. the output streams will pass through all console instances.
   @param {Object} [opts]
   @param {string} [opts.textColor] - An html-like description of text color, e.g. 'black' or '#ed5616'
   @param {string} [opts.backgroundColor] - An html-like description of background color
   @param {number} [opts.fontSize]  - A floating-point font point size
   @param {Console#Transform} [opts.transformStdout] - A function to transform output string being sent to stdout
   @param {Console#Transform} [opts.transformStderr] - A function to transform output strings being sent to stderr
   */
  constructor(opts) {
    super()
    opts = opts || {}
    if (!opts.transformStderr) opts.transformStderr =
      (out) => '\u001b[31m' + String(out)

    let c = native.newConsole(opts)
    let parent = undefined
    let pending = false
    let oldStdout = {}
    let oldStderr = {}

    const redirectStdout = transform =>
      redirect(process.stdout, oldStdout, transform)

    const redirectStderr = transform =>
      redirect(process.stderr, oldStderr, transform)

    redirectStderr(opts.transformStderr)
    redirectStdout(opts.transformStdout)

    c.on('detached', () => {
      parent = undefined
      if (oldStderr.rows !== undefined) {
        process.stderr.rows = oldStderr.rows
        process.stderr.columns = oldStderr.columns
        process.stdout.rows = oldStdout.rows
        process.stdout.columns = oldStdout.columns
      }
      this.emit('detached')
    })

    c.on('attached', () => {
      this.emit('attached')
    })

    c.on('error', (e) => {
      this.emit('error', e)
    })

    c.on('resize', (c,r) => {
      process.stdout.columns = c
      process.stdout.rows = r
      process.stderr.columns = c
      process.stderr.rows = r
    })

    /**
     Requests the console to be displayed (attached) on a caraml-core view. If no caramlview is provided,
     the default core view will be used.
     @name Console#display
     @function
     @param {Object} [caramlview] - A caraml-core view object obtained through
                                    <code>var core = require('@liquidcore/caraml-core')</code>
     @returns {Promise} Promise which is resolved when the view is attached, or rejected with an error string
     */
    this.display = (caramlview) => {
      return new Promise((resolve, reject) => {
        const attach = () => {
          parent = caramlview
          pending = false
          c.attach(caramlview).then(resolve).catch(reject)
        }
        const detach = () => {
          c.detach()
        }

        if (!pending) {
          if (caramlview === undefined) caramlview = require('@liquidcore/caraml-core')
          let state = c.state()
          if (state == 'init') {
            pending = true
            c.once('ready', attach)
          } else if (state == 'detaching') {
            pending = true
            c.once('detached', attach)
          } else if (state == 'attaching' || state == 'attached') {
            if (caramlview != parent) {
              pending = true
              if (state == 'attaching') {
                c.once('attached', detach)
              }
              c.once('detached', attach)
            } else {
              // do nothing because we are already attaching/attached to parent
              resolve()
            }
          } else {
            // state == 'detached'
            attach()
          }
        } else {
          reject('A display request is already pending')
        }
      })
    }

    /**
     Detaches a previously displayed console.
     @name Console#hide
     @function
     @returns {Promise} Resolved when the view is attached, or rejected with an error string
     */
    this.hide = () => {
      return new Promise((resolve,reject) => {
        let state = c.state()
        let onAttached = () => {
          c.detach().then(onDetached).catch(onError)
        }
        let onDetached = () => {
          c.removeListener('error', onError)
          resolve()
        }
        let onError = (e) => {
          c.removeListener('error', onError)
          c.removeListener('attached', onAttached)
          reject(e)
        }
        if (pending) {
          c.once('attached', onAttached)
          c.once('error', onError)
        } else if (parent !== undefined) {
          c.once('error', onError)
          onAttached()
        } else {
          reject('This console is not being displayed')
        }
      })
    }

    /**
     Returns the parent caraml-core view
     @name Console#getParent
     @function
     @returns {Object} The caraml-core view to which the console is attached or undefined if not attached
     */
    this.getParent = () => parent

    /**
     Returns one of 'init', 'detached', 'attaching', 'attached', 'detaching'
     @name Console#getState
     @function
     @returns {string} The current state of the console
     */
    this.getState = () => c.state()

    function redirect(stream, old, transform) {
      if (old.write === undefined) {
        old.write = stream.write
        old.clearScreenDown = stream.clearScreenDown
        old.moveCursor = stream.moveCursor
        old.transform = transform
        old.columns = stream.columns
        old.rows = stream.rows
        if (!old.transform) old.transform = x => x

        stream.write = (output) => {
          c.write(old.transform(output))
          if (old.write) old.write.call(stream, output)
        }

        stream.clearScreenDown = () => {
          c.write('\x1b[0J')
          if (old.clearScreenDown) old.clearScreenDown()
        }

        stream.moveCursor = (col,r) => {
          var out = '', col = col || 0, r = r || 0
          if (col>0) out += '\x1b['+col+'C'
          else if (col<0) out+='\x1b['+(-col)+'D'
          if (r>0) out += '\x1b['+r+'B'
          else if (r<0) out+='\x1b['+(-r)+'A'
          c.write(out)
          if (old.moveCursor) old.moveCursor(col,r)
        }
      }
    }
  }
}

/**
Emitted when the console view has been created and is ready to be attached to a caraml-core view.
@event Console#ready
*/
/**
Emitted when the console view has been attached to a caraml-core view.
@event Console#attached
*/
/**
Emitted when the console view has been detached from a caraml-core view.
@event Console#detached
*/
/**
Emitted on error
@event Console#error
@param {string} - A human-readable error string
*/

module.exports = Console