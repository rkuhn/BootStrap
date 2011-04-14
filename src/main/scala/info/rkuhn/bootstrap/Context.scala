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

case class Context(path : List[String] = List(""), dict : Map[String, AnyRef] = Map(), err : List[PositionalString] = Nil) {

  def addPath(s : String*) = copy(path = path ++ s)
  def +(s : String) = copy(path = path :+ s)
  def ++(s : Iterable[String])(implicit ev : String =:= String) = copy(path = path ++ s)

  def addObject(n : String, o : AnyRef) = copy(dict = dict + (n -> o))
  def +(n : (String, AnyRef)) = copy(dict = dict + n)
  def ++(n : Iterable[(String, AnyRef)]) = copy(dict = dict ++ n)

  def load(ast : AST) : Context = (this /: ast.nodes)(_.loadConfig(_)._1)

  def apply[T](name : String)(implicit m : Manifest[T]) : Option[T] =
    dict.get(name).flatMap(x => if (m.erasure.isInstance(x)) Some(x.asInstanceOf[T]) else None)

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
            case Some(x) => WithRewritten(ident, Object(x)).setPos(arg.pos)
            case None => PositionalString("object creation failed for "+ident).setPos(arg.pos)
          }
          (newctx, newarg :: args)
        case With(ident, Reference(r)) =>
          ctx.dict.get(r) match {
            case Some(x) => (ctx, WithRewritten(ident, Object(x)).setPos(arg.pos) :: args)
            case None => (ctx, PositionalString("no reference found for name "+r).setPos(arg.pos) :: args)
          }
        case With(ident, l : Literal) => (ctx, WithRewritten(ident, l).setPos(arg.pos) :: args)
        case Constructor(c : Configuration) =>
          val (newctx, obj) = ctx.loadConfig(c)
          val newarg = obj match {
            case Some(x) => ConstructorRewritten(Object(x)).setPos(arg.pos)
            case None => PositionalString("object creation failed for constructor arg of "+conf.ident).setPos(arg.pos)
          }
          (newctx, newarg :: args)
        case Constructor(Reference(r)) =>
          ctx.dict.get(r) match {
            case Some(x) => (ctx, ConstructorRewritten(Object(x)).setPos(arg.pos) :: args)
            case None => (ctx, PositionalString("no reference found for constructor "+r).setPos(arg.pos) :: args)
          }
        case Constructor(l : Literal) => (ctx, ConstructorRewritten(l).setPos(arg.pos) :: args)
        case c : Call => (ctx, c :: args)
      }
    }
    val errors = args_rev collect { case x : PositionalString => x }
    if (!errors.isEmpty) {
      return (ctx.copy(err = errors ::: ctx.err), None)
    }
    val args = args_rev.reverse

    ctx.loadClass(conf.ident) match {
      case Nil =>
        (ctx.copy(err = PositionalString("no class found for name "+conf.ident).setPos(conf.pos) :: ctx.err), None)
      case l @ _ :: _ :: _ =>
        (ctx.copy(err = PositionalString("multiple classes found for name"+conf.ident+": "+l.mkString(", ")).setPos(conf.pos) :: ctx.err), None)
      case c :: Nil =>
        if (classOf[Configurable].isAssignableFrom(c))
          ctx.loadConfigurable(c, conf, args)
        else
          ctx.loadBean(c, conf, args)
    }
  }

  private def loadConfigurable(c : Class[_], conf : Configuration, args : List[ArgumentRewritten])
      : (Context, Option[AnyRef]) = {
    val name = conf.name
    val obj = try c.newInstance.asInstanceOf[Configurable] catch {
      case e => return (copy(err = PositionalString("cannot instantiate configurable class "+c+": "+e.getMessage).setPos(conf.pos) :: err), None)
    }

    /*
     * if asked so, then try all normal setters first and retain only those which failed
     */
    val remaining : List[ArgumentRewritten] =
      if (obj.useBeanSetters) {
        args flatMap { arg =>
          arg match {
            case w : WithRewritten => set(obj, c, w) map ( err => w )
            case c : ConstructorRewritten => Some(PositionalString("constructor arg invalid for Configurable "+name).setPos(conf.pos))
            case c : Call => Some(PositionalString("method call invalid for Configurable "+name).setPos(conf.pos))
            case _ : PositionalString => sys.error("impossible")
          }
        }
      } else {
        args
      }
    val errors = remaining collect { case x : PositionalString => x }
    if (!errors.isEmpty) {
      return (copy(err = errors ::: err), None)
    }

    val config = remaining collect {
      case WithRewritten(ident, Literal(s)) => ident -> s
      case WithRewritten(ident, Object(o))  => ident -> o
    }

    obj.configure(config) match {
      case Success(o) => (copy(dict = dict ++ (name map (_ -> o))), Some(o))
      case Failure(errors) => (copy(err = errors.map(x => PositionalString(x).setPos(conf.pos)) ::: err), None)
    }
  }

  private def loadBean(c : Class[_], conf : Configuration, args : List[ArgumentRewritten])
      : (Context, Option[AnyRef]) = {
    val name = conf.name      
    val construct = args collect { case ConstructorRewritten(d) => d }
    val constructors = c.getConstructors filter (_.getParameterTypes.length == construct.length)
    val constructor = constructors flatMap { cc =>
      val arg = cc.getParameterTypes zip construct flatMap (p => mkValue(p._1, p._2))
      if (arg.length == construct.length) List[(Constr[_], Array[AnyRef])]((cc, arg)) else Nil
    }
    if (constructor.length == 0) {
      if (constructors.length == 0) {
        (copy(err = PositionalString("no constructor taking "+construct.length+" arguments on class "+c.getName).setPos(conf.pos) :: err), None)
      } else {
        (copy(err = PositionalString("type mismatch for constructor of class "+c.getName).setPos(conf.pos) :: err), None)
      }
    } else if (constructor.length > 1) {
      (copy(err = PositionalString("ambiguous constructor for class "+c.getName).setPos(conf.pos) :: err), None)
    } else {
      try {
        val (cc, arg) = constructor.apply(0)
        val obj = cc.asInstanceOf[Constr[AnyRef]].newInstance(arg : _*)
        setBean(obj, c, name, args)
      } catch {
        case e => (copy(err = PositionalString(e.getMessage).setPos(conf.pos) :: err), None)
      }
    }
  }

  private def setBean(obj : AnyRef, c : Class[_], name : Option[String], args : List[ArgumentRewritten])
      : (Context, Option[AnyRef]) = {
    val calls = args flatMap { arg =>
      arg match {
        case _ : ConstructorRewritten => None
        case c : Call => Some(c)
        case w : WithRewritten => set(obj, c, w) map ( err => PositionalString(err).setPos(arg.pos) )
        case _ : PositionalString => sys.error("impossible")
      }
    }
    val errors = calls collect { case x : PositionalString => x }
    if (!errors.isEmpty) {
      return (copy(err = errors ::: err), None)
    }
    for (cc @ Call(call) <- calls) {
      try {
        val m = c.getMethod(call)
        m.invoke(obj)
      } catch {
        case e => return (copy(err = PositionalString(e.getMessage).setPos(cc.pos) :: err), None)
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
