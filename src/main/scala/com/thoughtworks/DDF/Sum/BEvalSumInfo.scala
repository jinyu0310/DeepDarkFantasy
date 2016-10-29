package com.thoughtworks.DDF.Sum

import com.thoughtworks.DDF.Arrow.BEvalArrowInfo
import com.thoughtworks.DDF.{BEval, BEvalCase, CommutativeMonoid, LossCase, LossInfo}

import scalaz.Leibniz.witness

trait BEvalSumInfo extends SumInfo[LossInfo, BEval] with BEvalArrowInfo {
  trait SumLCRet[A, B] {
    def Left: LossInfo[A]

    def Right: LossInfo[B]
  }

  case class SumBEC[A, B]() extends BEvalCase[Either[A, B]] {
    override type ret = Either[BEval[A], BEval[B]]
  }

  case class SumLC[A, B]() extends LossCase[Either[A, B]] {
    override type ret = SumLCRet[A, B]
  }

  override implicit def sumInfo[A, B](implicit ai: LossInfo[A], bi: LossInfo[B]): LossInfo.Aux[Either[A, B], (ai.loss, bi.loss)] =
    new LossInfo[Either[A, B]] {

      override def convert: Either[A, B] => BEval[Either[A, B]] = {
        case Left(x) => sumEval(scala.Left(ai.convert(x)))
        case Right(x) => sumEval(scala.Right(bi.convert(x)))
      }

      override val lc: LossCase.Aux[Either[A, B], SumLCRet[A, B]] = SumLC()

      override def lca: lc.ret = new SumLCRet[A, B] {
        override def Left: LossInfo[A] = ai

        override def Right: LossInfo[B] = bi
      }

      override type ret = (ai.loss, bi.loss)

      override def m: CommutativeMonoid[(ai.loss, bi.loss)] = new CommutativeMonoid[(ai.loss, bi.loss)] {
        override def zero: (ai.loss, bi.loss) = (ai.m.zero, bi.m.zero)

        override def append(f1: (ai.loss, bi.loss), f2: => (ai.loss, bi.loss)): (ai.loss, bi.loss) =
          (ai.m.append(f1._1, f2._1), bi.m.append(f1._2, f2._2))
      }

      override def update(x: Either[A, B])(rate: Double)(l: (ai.loss, bi.loss)): Either[A, B] =
        x.left.map(y => ai.update(y)(rate)(l._1)).right.map(y => bi.update(y)(rate)(l._2))
    }

  override def sumLeftInfo[A, B]: LossInfo[Either[A, B]] => LossInfo[A] = l => witness(l.lc.unique(SumLC[A, B]()))(l.lca).Left

  override def sumRightInfo[A, B]: LossInfo[Either[A, B]] => LossInfo[B] = l => witness(l.lc.unique(SumLC[A, B]()))(l.lca).Right

  def sumEval[A, B](s: Either[BEval[A], BEval[B]])(implicit al: LossInfo[A], bl: LossInfo[B]) = new BEval[Either[A, B]] {
    override val loss: LossInfo[Either[A, B]] = sumInfo[A, B]

    override def eval: Either[A, B] = s match {
      case Left(x) => scala.Left(x.eval)
      case Right(x) => scala.Right(x.eval)
    }

    override val ec: BEvalCase.Aux[Either[A, B], Either[BEval[A], BEval[B]]] = SumBEC()

    override def eca: ec.ret = s
  }

  def seval[A, B](s: BEval[Either[A, B]]): Either[BEval[A], BEval[B]] = witness(s.ec.unique(SumBEC[A, B]()))(s.eca)
}

object BEvalSumInfo {
  implicit def apply: BEvalSumInfo = new BEvalSumInfo {}
}
