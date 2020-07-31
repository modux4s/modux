package modux.shared

import java.security.{AccessController, PrivilegedAction}

/**
 * provides helpers for managing ClassLoaders and Threads
 */
object Threads {

  /**
   * executes given function in the context of the provided classloader
   * @param classloader that should be used to execute given function
   * @param b function to be executed
   */
  def withContextClassLoader[T](classloader: ClassLoader)(b: => T): T = {
    val thread    = Thread.currentThread
    val oldLoader = thread.getContextClassLoader
    try {
      thread.setContextClassLoader(classloader)
      b
    } finally {
      thread.setContextClassLoader(oldLoader)
    }
  }

  def withContext[T](classLoader: ClassLoader)(f: => T): T = {
    val thread    = Thread.currentThread
    val oldLoader = thread.getContextClassLoader
    // we use accessControlContext & AccessController to avoid a ClassLoader leak (ProtectionDomain class)
    AccessController.doPrivileged(
      new PrivilegedAction[T]() {
        def run: T = {
          try {
            thread.setContextClassLoader(classLoader)
            f
          } finally {
            thread.setContextClassLoader(oldLoader)
          }
        }
      },
     AccessController.getContext
    )
  }
}
