package internal.infra.utils

import scala.math.Numeric

def clampCount[T](value: T)(using numeric: Numeric[T]): T = if numeric.lt(value, numeric.zero) then numeric.zero else value
