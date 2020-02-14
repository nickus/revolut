package com.revolut.api.exceptions

class WrongArgsException(argName: String) : RuntimeException("Wrong argument: $argName")