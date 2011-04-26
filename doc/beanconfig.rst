.. _beanconfig:

###############################
Plain Java Object Configuration
###############################

Object creation proceeds in three stages, where the first two may inject value
specified in the configuration file which must be converted to the required
data types. This section describes first the conversion of data types followed
by the three stages instantiation, configuration and finalization.

How Literals are Converted
==========================

All literals given in the configuration file start out as strings of
characters. In the context of their use they must be converted to the required
data type as given by the declared signature of constructors or methods on the
class to be instantiated. The algorithm applied is quite simple:

 - for each of the eight primitive types (:ctype:`boolean`, :ctype:`byte`,
   :ctype:`char`, :ctype:`short`, :ctype:`int`, :ctype:`long`, :ctype:`float`,
   :ctype:`double`) and their boxed counterparts, the boxed type's
   :meth:`valueOf` method is used to parse the given string; If this fails,
   object creation is aborted and an error message generated.
 - for :class:`String` arguments, the literal is passed in unchanged
 - for all other reference types object creation is aborted and an error
   message generated; these types must be passed in either as
   :token:`reference` or nested object (:token:`configure`).

Class Instantiation
===================

The list of constructor arguments is given with the object definition; it may
be empty, which will probably be the most common case. The list of all declared
constructors for the desired class is filtered for those which accept the same
number of arguments for which the arguments given in the configuration can
successfully be converted to the desired parameter types. If none or multiple
constructors remain, object creation is aborted, otherwise the constructor is
used to create a new instance of the class.

.. _beanproperty:

Property Configuration
======================

For each property definition a matching setter method is searched and applied;
if none is found object configuration is aborted. A setter is a method which
takes a single parameter and for which the given value may be successfully
converted to the required data type. Setter names are found by filtering all
public methods by the following algorithm:

 - prefixes "add" or "set" are stripped
 - suffix "_$equals" is stripped
 - name is converted to lower-case
 - result is compared to lower-cased property name

Finalization
============

When all properties are configured, parameterless methods may be called on the
object to finish its configuration. The list of methods from the configuration
is applied in the same order as given. If a method cannot be found, object
configuration is aborted.
