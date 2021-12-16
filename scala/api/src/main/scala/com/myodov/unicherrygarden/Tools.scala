package com.myodov.unicherrygarden

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
}
