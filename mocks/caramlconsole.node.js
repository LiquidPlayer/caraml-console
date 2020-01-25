/*
 * Copyright (c) 2020 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-core for terms and conditions.
 */
console.warn('WARN: Using mock interface for caraml-console')

const events = require('events')

class Console extends events {
  constructor() {
    super()
    this._state = 'detached'
  }

  attach(core) {
    if (!core || typeof core !== 'object')
      throw new TypeError('attach: first argument must be a caraml object')
    if (this._state != 'detached')
      throw new Error('attach: must be in detached state')

    this._core = core
    this._state = 'attached'
    this.emit(this._state)
    return Promise.resolve()
  }

  state() {
    return this._state
  }

  detach() {
    this._state = 'detached'
    this.emit(this._state)
    return Promise.resolve()
  }

  write(out) {
  }
}

module.exports = {
  newConsole() {
    return new Console()
  }
}
