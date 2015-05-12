package nodescala

import scala.language.postfixOps
import scala.util.{Try, Success, Failure}
import scala.collection._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.async.Async.{async, await}
import org.scalatest._
import NodeScala._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner 

@RunWith(classOf[JUnitRunner])
class NodeScalaSuite extends FunSuite {

  // always
  test("A Future should be completed with the value passed in to \"always\" method. [Int]") {
    val always = Future.always(517)
    assert(Await.result(always, 0 nanos) == 517)
  }
  
  // always
  test("A Future should be completed with the value passed in to \"always\" method. [String]") {
      val always = Future.always("foo")
      assert(Await.result(always, 0 nanos) == "foo")
  }
  
  // never
  test("A Future should never be completed. [Int]") {
    val never = Future.never[Int]

    try {
      /* Await should throw a TimeoutException which bypasses assert(false)
       * assert == false will fail a test */
      Await.result(never, 1 second)
      assert(false)
    } catch {
      case t: TimeoutException => {
          // ok! 
      }
    }
  }
  
  // any
  test("A Future should return the future holding the value of the future that completed first. test 1") {
      val one = Future {
        Thread.sleep(100)
        1
      }                                                 
      val two = Future {
        2
      }  
      val ex = Future {
        Thread.sleep(200)
        throw new Exception
      }  
      val any = Future.any(List(one, two, ex))
      assert(Await.result(any, 1 second) == 2)
  }
  
  // any
  test("A Future should return the future holding the value of the future that completed first. test 2") {
      val one = Future {
        1
      }                                                 
      val two = Future {
        Thread.sleep(100)
        2
      }  
      val ex = Future {
        Thread.sleep(200)
        throw new Exception
      }  
      val any = Future.any(List(two, ex, one))
      assert(Await.result(any, 1 second) == 1)
  }
  
  // any
  test("A Future should return the future holding the value of the future that completed first. test 3") {
      val one = Future {
        Thread.sleep(200)
        1
      }                                                 
      val two = Future {
        Thread.sleep(100)
        2
      }  
      val ex = Future {
        throw new Exception
      }
      try{
          Future.any(List(two, ex, one))
          assert(false)
      } catch {
          case e: Exception => {
              // ok!
          }
      }
  }
  
  // all
  test("A Future should take a List of Futures and return a Future of the List values. test 1") {
      val one = Future {
        Thread.sleep(100)
        1
      }                                                 
      val two = Future {
        2
      }
      val three = Future {
        Thread.sleep(200)
        3
      }
      val all = Future.all(List(three, two, one))
      assert(Await.result(all, 1 second) == List(3,2,1))
  }
  
  // all
  test("A Future should take a List of Futures and fail on exception. test 2") {
      val one = Future {
        Thread.sleep(100)
        1
      }                                                 
      val two = Future {
        2
      }
      val three = Future {
        Thread.sleep(200)
        throw new Exception
      }
      try{
          val all = Future.all(List(one, two, three))
          Await.result(all, 1 second)
          assert(false)
      } catch {
          case e: Exception => {
              // ok!
          }
      }
  }
  
  // delay
  test("A Future should be completed after 1s delay") { 
    val start = System.currentTimeMillis()  

    Future.delay(1 second) onComplete { case _ =>  
      val duration = System.currentTimeMillis() - start 
      assert (duration >= 1000L && duration < 1100L)
    }
  }  

  // delay
  test("Two sequential delays of 1s should delay by 2s") {  
    val start = System.currentTimeMillis()  

    val combined = for {  
      f1 <- Future.delay(1 second)  
      f2 <- Future.delay(1 second)  
    } yield ()  

    combined onComplete { case _ =>  
      val duration = System.currentTimeMillis() - start  
      assert (duration >= 2000L && duration < 2100L)
    }
  }
  
  // delay
  test("A Future should complete after 3s when using a delay of 1s") {  
      val p = Promise[Unit]()  
    
      Future {  
        blocking {  
          Future.delay(1 second) onSuccess {  
            case _ => p.complete(Try(()))  
          }  
        }  
      }  
    
      Await.ready(p.future, 3 second)  
  }  
    
  // delay  
  test("A Future should not complete after 1s when using a delay of 3s") {  
      val p = Promise[Unit]()  
    
      Future {  
        blocking {  
          Future.delay(3 second) onSuccess {  
            case _ => p.complete(Try(()))  
          }  
        }  
      }  
    
      try {  
        Await.result(p.future, 1 second)  
        assert(false)  
      } catch {  
        case t: TimeoutException => // ok!  
      }  
  }
  
  // delay
  test("A Future should not complete after 2s when using a delay of 5s") {
    val p = Future.delay(5 second)
    try {
      val z = Await.result(p, 2 second) // block for future to complete
      assert(false)
    } catch {
      case _: TimeoutException => // Ok!
    }
  }
  
  // now
  test("A Future should return its result if it is completed now") {
      val p = Promise[Unit]()
      p.success(())
      p.future.now
      assert(true)
  }
  
  // now
  test("*never.now* should throw a NoSuchElementException") {
    intercept[NoSuchElementException] {
      Future.never.now
    }
  }
  
  // continueWith
  test("continueWith should wait for the first future to complete") {
      val delay = Future.delay(1 second)
      val always = (f: Future[Unit]) => 42
    
      try {
        Await.result(delay.continueWith(always), 500 millis)
        assert(false)
      }
      catch {
        case t: TimeoutException => // ok
      }
   }

  // ----------------------- //
  
  class DummyExchange(val request: Request) extends Exchange {
    @volatile var response = ""
    val loaded = Promise[String]()
    def write(s: String) {
      response += s
    }
    def close() {
      loaded.success(response)
    }
  }

  class DummyListener(val port: Int, val relativePath: String) extends NodeScala.Listener {
    self =>

    @volatile private var started = false
    var handler: Exchange => Unit = null

    def createContext(h: Exchange => Unit) = this.synchronized {
      assert(started, "is server started?")
      handler = h
    }

    def removeContext() = this.synchronized {
      assert(started, "is server started?")
      handler = null
    }

    def start() = self.synchronized {
      started = true
      new Subscription {
        def unsubscribe() = self.synchronized {
          started = false
        }
      }
    }

    def emit(req: Request) = {
      val exchange = new DummyExchange(req)
      if (handler != null) handler(exchange)
      exchange
    }
  }

  class DummyServer(val port: Int) extends NodeScala {
    self =>
    val listeners = mutable.Map[String, DummyListener]()

    def createListener(relativePath: String) = {
      val l = new DummyListener(port, relativePath)
      listeners(relativePath) = l
      l
    }

    def emit(relativePath: String, req: Request) = this.synchronized {
      val l = listeners(relativePath)
      l.emit(req)
    }
  }
  
  test("Server should serve requests") {
    val dummy = new DummyServer(8191)
    val dummySubscription = dummy.start("/testDir") {
      request => for (kv <- request.iterator) yield (kv + "\n").toString
    }

    // wait until server is really installed
    Thread.sleep(500)

    def test(req: Request) {
      val webpage = dummy.emit("/testDir", req)
      val content = Await.result(webpage.loaded.future, 1 second)
      val expected = (for (kv <- req.iterator) yield (kv + "\n").toString).mkString
      assert(content == expected, s"'$content' vs. '$expected'")
    }

    test(immutable.Map("StrangeRequest" -> List("Does it work?")))
    test(immutable.Map("StrangeRequest" -> List("It works!")))
    test(immutable.Map("WorksForThree" -> List("Always works. Trust me.")))

    dummySubscription.unsubscribe()
  }

}




