package info.rkuhn.bootstrap

import AST._
import java.lang.reflect.{Method, Constructor => Constr}
import scala.collection.breakOut

/**
 * 
 * This file is (c) 2011, Roland Kuhn
 *
 * License: LGPLv2.1, for more information see COPYING in top-level directory
 */

case class Context(path : List[String] = List(""), dict : Map[String, AnyRef] = Map(), err : List[String] = Nil) {

  def addPath(s : String*) = copy(path = path ++ s)
  def +(s : String) = copy(path = path :+ s)
  def ++(s : Iterable[String])(implicit ev : String =:= String) = copy(path = path ++ s)

  def addObject(n : String, o : AnyRef) = copy(dict = dict + (n -> o))
  def +(n : (String, AnyRef)) = copy(dict = dict + n)
  def ++(n : Iterable[(String, AnyRef)]) = copy(dict = dict ++ n)

  def load(ast : AST) : Context = (this /: ast.nodes)(_.loadConfig(_)._1)

  private def loadConfig(conf : Configuration) : (Context, Option[AnyRef]) = {
    /*
     * first rewrite AST to create nested objects and resolve references
     */
    val (ctx, args_rev) = ((this, List.empty[ArgumentRewritten]) /: conf.args){ (p, arg) =>
      val (ctx, args) = p
      arg match {
        case With(ident, c : Configuration) =>
          val (newctx, obj) = ctx.loadConfig(c)
          val newarg = obj match {
            case Some(x) => WithRewritten(ident, Object(x))
            case None => ArgFail("object creation failed for "+ident)
          }
          (newctx, newarg :: args)
        case With(ident, Reference(r)) =>
          ctx.dict.get(r) match {
            case Some(x) => (ctx, WithRewritten(ident, Object(x)) :: args)
            case None => (ctx, ArgFail("no reference found for name "+r) :: args)
          }
        case With(ident, l : Literal) => (ctx, WithRewritten(ident, l) :: args)
        case Constructor(c : Configuration) =>
          val (newctx, obj) = ctx.loadConfig(c)
          val newarg = obj match {
            case Some(x) => ConstructorRewritten(Object(x))
            case None => ArgFail("object creation failed for constructor arg of "+conf.ident)
          }
          (newctx, newarg :: args)
        case Constructor(Reference(r)) =>
          ctx.dict.get(r) match {
            case Some(x) => (ctx, ConstructorRewritten(Object(x)) :: args)
            case None => (ctx, ArgFail("no reference found for constructor "+r) :: args)
          }
        case Constructor(l : Literal) => (ctx, ConstructorRewritten(l) :: args)
        case c : Call => (ctx, c :: args)
      }
    }
    val errors = args_rev collect { case ArgFail(msg) => msg }
    if (!errors.isEmpty) {
      return (ctx.copy(err = errors ::: ctx.err), None)
    }
    val args = args_rev.reverse

    ctx.loadClass(conf.ident) match {
      case Nil =>
        (ctx.copy(err = "no class found for name "+conf.ident :: ctx.err), None)
      case l @ _ :: _ :: _ =>
        (ctx.copy(err = "multiple classes found for name"+conf.ident+": "+l.mkString(", ") :: ctx.err), None)
      case c :: Nil =>
        if (classOf[Configurable].isAssignableFrom(c))
          ctx.loadConfigurable(c, conf.name, args)
        else
          ctx.loadBean(c, conf.name, args)
    }
  }

  private def loadConfigurable(c : Class[_], name : Option[String], args : List[ArgumentRewritten])
      : (Context, Option[AnyRef]) = {
    val obj = try c.newInstance.asInstanceOf[Configurable] catch {
      case e => return (copy(err = "cannot instantiate configurable class "+c+": "+e.getMessage :: err), None)
    }

    /*
     * if asked so, then first try all normal setters first and retain only those which failed
     */
    val remaining : List[ArgumentRewritten] =
      if (obj.useBeanSetters) {
        args flatMap { arg =>
          arg match {
            case w : WithRewritten => set(obj, c, w) map ( err => w )
            case c : ConstructorRewritten => Some(ArgFail("constructor arg invalid for Configurable "+name))
            case c : Call => Some(ArgFail("method call invalid for Configurable "+name))
            case _ : ArgFail => sys.error("impossible")
          }
        }
      } else {
        args
      }
    val errors = remaining collect { case ArgFail(msg) => msg }
    if (!errors.isEmpty) {
      return (copy(err = errors ::: err), None)
    }

    val config : Map[String, AnyRef] = remaining.collect{
      case WithRewritten(ident, Literal(s)) => ident -> s
      case WithRewritten(ident, Object(o))  => ident -> o
    }(breakOut)

    obj.configure(config) match {
      case Success(o) => (copy(dict = dict ++ (name map (_ -> o))), Some(o))
      case Failure(errors) => (copy(err = errors ::: err), None)
    }
  }

  private def loadBean(c : Class[_], name : Option[String], args : List[ArgumentRewritten])
      : (Context, Option[AnyRef]) = {
    val construct = args collect { case ConstructorRewritten(d) => d }
    val constructors = c.getConstructors filter (_.getParameterTypes.length == construct.length)
    val constructor = constructors flatMap { cc =>
      val arg = cc.getParameterTypes zip construct flatMap (p => mkValue(p._1, p._2))
      if (arg.length == construct.length) List[(Constr[_], Array[AnyRef])]((cc, arg)) else Nil
    }
    if (constructor.length == 0) {
      if (constructors.length == 0) {
        (copy(err = "no constructor taking "+construct.length+" arguments on class "+c.getName :: err), None)
      } else {
        (copy(err = "type mismatch for constructor of class "+c.getName :: err), None)
      }
    } else if (constructor.length > 1) {
      (copy(err = "ambiguous constructor for class "+c.getName :: err), None)
    } else {
      try {
        val (cc, arg) = constructor.apply(0)
        val obj = cc.asInstanceOf[Constr[AnyRef]].newInstance(arg)
        setBean(obj, c, name, args)
      } catch {
        case e => (copy(err = e.getMessage :: err), None)
      }
    }
  }

  private def setBean(obj : AnyRef, c : Class[_], name : Option[String], args : List[ArgumentRewritten])
      : (Context, Option[AnyRef]) = {
    val calls = args flatMap { arg =>
      arg match {
        case _ : ConstructorRewritten => None
        case c : Call => Some(c)
        case w : WithRewritten => set(obj, c, w) map ( err => ArgFail(err) )
        case _ : ArgFail => sys.error("impossible")
      }
    }
    val errors = calls collect { case ArgFail(msg) => msg }
    if (!errors.isEmpty) {
      return (copy(err = errors ::: err), None)
    }
    for (Call(call) <- calls) {
      try {
        val m = c.getMethod(call)
        m.invoke(obj)
      } catch {
        case e => return (copy(err = e.getMessage :: err), None)
      }
    }
    (copy(dict = dict ++ (name map (_ -> obj))), Some(obj))
  }

  private def loadClass(n : String) : List[Class[_]] = {
    val cl = Thread.currentThread.getContextClassLoader
    path flatMap { p =>
      try Some[Class[_]](cl.loadClass(p + n)) catch { case e => None }
    }
  }

  /**
   * Try to call a setter on an object, possibly returning an error message or None if successful.
   */
  private def set(o : AnyRef, c : Class[_], w : WithRewritten) : Option[String] = {
    import scala.collection.JavaConversions._

    val settername = "set"+w.ident
    val setters : List[Method] = c.getMethods.toList filter (x => x.getName == settername && x.getParameterTypes.length == 1)
    val setter = setters flatMap { m =>
      val clazz = m.getParameterTypes.apply(0)
      mkValue(clazz, w.definition) match {
        case Some(x) => Some((m, x))
        case None => None
      }
    }
    setter match {
      case Nil =>
        if (setters.isEmpty) {
          Some("no setters for "+w.ident+" on "+c.getName)
        } else {
          val c = setters map (_.getParameterTypes.apply(0).getName) mkString ", "
          Some("argument type for property "+w.ident+" not fitting for possibible choices "+c)
        }
      case _ :: _ :: _ => Some("ambiguous argument type for property "+w.ident)
      case (method, arg) :: Nil =>
        try {
          method.invoke(o, arg)
          None
        } catch {
          case e => Some(e.getMessage)
        }
    }

  }

  private def mkValue(c : Class[_], definition : DefinitionRewritten) : Option[AnyRef] = {
    definition match {
      case Object(o) => if (c.isInstance(o)) Some(o) else None
      case Literal(s) =>
        try {
          c.getName match {
            case "boolean" | "java.lang.Boolean"   => Some(java.lang.Boolean.valueOf(s))
            case "byte"    | "java.lang.Byte"      => Some(java.lang.Byte.valueOf(s))
            case "short"   | "java.lang.Short"     => Some(java.lang.Short.valueOf(s))
            case "int"     | "java.lang.Integer"   => Some(java.lang.Integer.valueOf(s))
            case "long"    | "java.lang.Long"      => Some(java.lang.Long.valueOf(s))
            case "float"   | "java.lang.Float"     => Some(java.lang.Float.valueOf(s))
            case "double"  | "java.lang.Double"    => Some(java.lang.Double.valueOf(s))
            case "char"    | "java.lang.Character" => Some(java.lang.Character.valueOf(s.charAt(0)))
            case "java.lang.String" => Some(s)
            case "java.lang.Class" => loadClass(s).headOption
            case _ => None
          }
        } catch {
          case e => None
        }
    }
  }

}