package modux.model.context

import java.util.concurrent.atomic.AtomicReference

case object ContextInject{
  private final val instance = new AtomicReference[Context]()
  private [modux] def setInstance(context: Context): Unit = instance.set(context)

  def getInstance: Context = instance.get()
}
