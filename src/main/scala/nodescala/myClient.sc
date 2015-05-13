package nodescala

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.language.postfixOps

object myClient {
  println("~~~~myClient TOP")                     //> ~~~~myClient TOP

  val working = Future.run() { ct =>
    println("~~~~ct: " + ct)
    Future {
      while (ct.nonCancelled) {
        //println("....working")
      }
      assert(ct.isCancelled)
      println("....done")
    }
  } // end working = Future.run()                 //> ~~~~ run TOP, f: <function1>
                                                  //| ~~~~ct: nodescala.package$CancellationTokenSource$$anon$1$$anon$2@d40869
                                                  //| working  : nodescala.Subscription = nodescala.package$CancellationTokenSourc
                                                  //| e$$anon$1@27242aae
  val f = Future.delay(5 seconds)                 //> f  : scala.concurrent.Future[Unit] = scala.concurrent.impl.Promise$DefaultPr
                                                  //| omise@1f4e1672
  f onSuccess {
    case _ => working.unsubscribe()
  }
  Await.ready(f, 6 seconds)                       //> res0: nodescala.myClient.f.type = scala.concurrent.impl.Promise$DefaultPromi
                                                  //| se@1f4e1672
  //Thread.sleep(8000)

  '''                                             //> ....done
                                                  //| res1: Char('\'') = '
}