package cc.rbbl.exceptions

import java.lang.Exception

class NoGenreFoundException(val itemName: String) : Exception()