package nodescala

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.language.postfixOps

object myClient {
  println("~~~~myClient TOP")                     //> ~~~~myClient TOP

  /* 'working' becomes a reference to a Subscription.
   * The second run parameter (in curley brackets) is a function
   * CancellationToken => Future[Unit]
   * This function is called on (passed) the CancellationToken,
   * ct, that has been created in the run() method body, and in
   * its own body constructs a Future which references the
   * CancellationToken periodically/continually to check if it
   * has been cancelled, in which case it exits its computation
   * and completes.
   */
  val working = Future.run() { ct =>
    println("~~~~ct: " + ct)
    Future {
      while (ct.nonCancelled) {
        //println("....working")
        /* This emulates a computational block. */
      }
      assert(ct.isCancelled)
      println("....done")
    }
  } // end working = Future.run()                 //> ~~~~ct: nodescala.package$CancellationTokenSource$$anon$1$$anon$2@49ab1c60
                                                  //| working  : nodescala.Subscription = nodescala.package$CancellationTokenSourc
                                                  //| e$$anon$1@27242aae
  val f = Future.delay(5 seconds)                 //> f  : scala.concurrent.Future[Unit] = scala.concurrent.impl.Promise$DefaultP
                                                  //| romise@1f4e1672
  f onSuccess {
    case _ => working.unsubscribe()
  }
  Await.ready(f, 6 seconds)                       //> res0: nodescala.myClient.f.type = scala.concurrent.impl.Promise$DefaultProm
                                                  //| ise@1f4e1672
  //Thread.sleep(8000)

  '''                                             //> ....done
                                                  //| res1: Char('\'') = '
}