package com.myodov.unicherrygarden

import scala.Ordering.Implicits._
import scala.math.Numeric
import scala.math.Numeric.Implicits._

/** Miscellaneous useful tools. */
object Tools {
  /** Converts a sequence of options of the same type to the option of the sequence.
   * The result option is assumed `Some` if all options in the sequence are non-`None`;
   * and assumed `None` if any option is `None`.
   */
  def reduceOptionSeq[A](seq: Seq[Option[A]]): Option[Seq[A]] =
    seq.foldLeft(
      Some(Seq.empty[A]): Option[Seq[A]]
    )(
      (a: Option[Seq[A]], b: Option[A]) => (a, b) match {
        case (Some(seql), Some(item)) => Some(seql :+ item)
        case other => None
      }
    )

  /** Check if a sequence of orderable items is strictly increasing on each step.
   * E.g. `5, 6, 17, 42` but not `5, 7, 6`.
   *
   * Note: true for single-item collection.
   */
  def seqIsIncreasing[T: Ordering](seq: Iterable[T]): Boolean =
    (seq.size == 1) || seq.sliding(2).forall { case Seq(a, b) =>
      (a: T) <= (b: T)
    }

  /** Check if a sequence of orderable items is only incrementing on each step.
   * E.g. `5, 6, 7, 8` but not `5, 6, 8, 9, 10` and of course not `5, 7, 6`.
   *
   * Note: true for single-item collection.
   */
  def seqIsIncrementing[T: Numeric](seq: Iterable[T]): Boolean =
    (seq.size == 1) || seq.sliding(2).forall { case Seq(a, b) =>
      b == (a: T) + Numeric[T].one
    }
}
