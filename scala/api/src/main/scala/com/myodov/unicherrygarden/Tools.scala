package com.myodov.unicherrygarden

import scala.Ordering.Implicits._
import scala.math.Numeric
import scala.math.Numeric.Implicits._


/** Custom [[Iterable]] implementation with special methods. */
class EnhancedIterable[T](i: Iterable[T]) {
  /** Check if each pair of adjacent items in a sequence satisfies predicate `p`.
   *
   * Note: true for single-item collection.
   */
  @inline def forAllPairs(p: (T, T) => Boolean): Boolean = Tools.forAllPairs(p)(i)
}

/** Miscellaneous useful tools. */
object Tools {

  object Implicits {
    implicit def iterableToIterable[T](i: Iterable[T]): EnhancedIterable[T] =
      new EnhancedIterable(i)
  }

  /** Converts a sequence of options of the same type to the option of the sequence.
   * The result option is assumed `Some` if all options in the sequence are non-`None`;
   * and assumed `None` if any option is `None`.
   */
  @inline def reduceOptionSeq[A](seq: Seq[Option[A]]): Option[Seq[A]] =
    seq.foldLeft(
      Some(Seq.empty[A]): Option[Seq[A]]
    )(
      (a: Option[Seq[A]], b: Option[A]) => (a, b) match {
        case (Some(seql), Some(item)) => Some(seql :+ item)
        case other => None
      }
    )

  /** Check if each pair of adjacent items in a sequence satisfies predicate `p`.
   *
   * Note: true for single-item collection.
   */
  @inline def forAllPairs[T](p: (T, T) => Boolean)(seq: Iterable[T]): Boolean =
    (seq.size == 1) || seq.sliding(2).map(_.toSeq).forall { case Seq(a, b) => p(a, b) }

  /** Check if a sequence of orderable items is strictly increasing on each step.
   * E.g. `5, 6, 17, 42` but not `5, 7, 6`.
   *
   * Note: true for single-item collection.
   */
  @inline def seqIsIncreasing[T: Ordering](seq: Iterable[T]): Boolean =
    forAllPairs((_: T) <= (_: T))(seq)

  /** Check if a sequence of orderable items is only incrementing on each step.
   * E.g. `5, 6, 7, 8` but not `5, 6, 8, 9, 10` and of course not `5, 7, 6`.
   *
   * Note: true for single-item collection.
   */
  @inline def seqIsIncrementing[T: Numeric](seq: Iterable[T]): Boolean =
    forAllPairs((_: T) + Numeric[T].one == (_: T))(seq)
}
