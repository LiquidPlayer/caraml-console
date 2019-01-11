/*
 * Copyright (c) 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
const bindings = require('bindings')
const native = bindings('caramlconsole')
const events = require('events')

class Console extends events {
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

    c.resize = (c,r) => {
      process.stdout.columns = c
      process.stdout.rows = r
      process.stderr.columns = c
      process.stderr.rows = r
    }

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
          if (caramlview === undefined) caramlview = require('caraml-core')
          let state = c.state()
          console.log(state)
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

    this.getParent = () => parent

    this.getState = () => c.state()

    function redirect(stream, old, transform) {
      old.transform = old.transform
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
          if (old.write) old.write(output)
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

module.exports = Console