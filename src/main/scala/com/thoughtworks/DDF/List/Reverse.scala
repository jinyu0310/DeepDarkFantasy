package com.thoughtworks.DDF.List

trait Reverse[Info[_], Repr[_]] extends ListMin[Info, Repr] {
  def reverse[A](implicit ai: Info[A]): Repr[scala.List[A] => scala.List[A]]

  final def reverse_[A]: Repr[scala.List[A]] => Repr[scala.List[A]] = f => app(reverse(listElmInfo(reprInfo(f))))(f)
}
