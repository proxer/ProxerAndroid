package me.proxer.app.exception

class PartialException(val innerError: Throwable, val partialData: Any?) : Exception()
