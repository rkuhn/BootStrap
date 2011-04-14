package info.rkuhn.bootstrap

/**
 * 
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */

trait Configurable {
  def useBeanSetters = true
  def configure(dict : List[(String, AnyRef)]) : ConfigResult
}

sealed trait ConfigResult
case class Success(obj : AnyRef) extends ConfigResult
case class Failure(msg : List[String]) extends ConfigResult
