.. _configurable:

###################################
The :class:`Configurable` Interface
###################################

For special applications (like using object factories) or enhanced user
experience, the objects to be configured may implement the
:class:`Configurable` interface.

.. class:: Configurable

   Interface which allows more sophisticated configuration.

   .. method:: useBeanSetters : Boolean
   
      Implement this method to define whether normal setters shall be tried
      before calling :meth:`configure`.

   .. method:: configure(config : List[(String, AnyRef)]) : AnyRef

      This abstract method will be passed all configuration items not processed
      by normal setters (if enabled) and must return a reference to the newly
      created object.

A class implementing this interface will be instantiated with its default
constructor (no arguments). If :meth:`~Configurable.useBeanSetters` returns
:obj:`true`, the configured properties will be tried in the same way as
described for the :ref:`plain Java objects <beanproperty>` where all properties
which cannot be successfully applied are stored in a list and passed into the
:meth:`~Configurable.configure` method. The reference returned by this method
is taken as the finished object resulting from this configuration; it should be
noted that this object need not be the same as the one receiving the
:meth:`~Configurable.configure` call, it does not even need to be of the same
type.

Custom constructors as well as finalizing method calls are not supported
because all configuration can easily be done within the
:meth:`~Configurable.configure` method.
