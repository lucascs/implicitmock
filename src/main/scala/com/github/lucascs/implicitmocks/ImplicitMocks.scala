package com.github.lucascs.implicitmocks

import org.mockito.Mockito._
import org.mockito.stubbing.Answer
import org.mockito.verification.VerificationMode
import org.mockito.invocation.InvocationOnMock
import collection.mutable
import java.lang.reflect.Method
import org.scalatest.{FlatSpec, BeforeAndAfterEach}
import org.mockito.{MockSettings, Matchers, Mockito}
import tools.scalap.scalax.rules.scalasig._

trait ImplicitMocks extends BeforeAndAfterEach { self:FlatSpec =>
  def mock[T](implicit manifest:Manifest[T]) = Mockito.mock(manifest.erasure, new ImplicitAnswer).asInstanceOf[T]
  def mock[T](answer:Answer[_>:T])(implicit manifest:Manifest[T]) = Mockito.mock(manifest.erasure, answer).asInstanceOf[T]
  def verify[T](obj:T) = Mockito.verify(obj)
  def verifyNoMoreInteractions(obj:AnyRef) { Mockito.verifyNoMoreInteractions(obj) }
  def verify[T](obj:T, verification:VerificationMode) = Mockito.verify(obj, verification)
  def spy[T](obj:T) = Mockito.spy(obj)
  def never = Mockito.never()
  def times(i : Int) = Mockito.times(i)
  def any[T](implicit manifest:Manifest[T]) = Matchers.any(manifest.erasure).asInstanceOf[T]
  def an[T]:T = null.asInstanceOf[T]
  def as[T](obj:T) = obj
  def eq[T](obj:T):T = Matchers.eq(obj)

  def theManifest[T](implicit m:Manifest[T]) = Matchers.eq(m)

  implicit def any2mockito[T](obj:T) = new MockitoExtensions(obj)
  override def afterEach() {
    cache = mutable.Map()
//    Mockito.validateMockitoUsage()
  }

  def mock[T](interfaces:Class[_]*)(implicit manifest:Manifest[T]) = Mockito.mock(manifest.erasure, Mockito.withSettings.extraInterfaces(interfaces:_*)).asInstanceOf[T]

  
  case class Arg(mock:AnyRef)
  case class AnyArg(override val mock:AnyRef) extends Arg(mock)
  case class SomeArg(arg:AnyRef, override val mock:AnyRef) extends Arg(mock)
  
  private var cache:mutable.Map[Method, List[Arg]] = mutable.Map()

  private implicit def list2exp[F <: AnyRef](l:Seq[F]) = new {
    def grep[T](implicit m:Manifest[T]):Seq[T] = l.filter(x => m.erasure.isAssignableFrom(x.getClass)).map(_.asInstanceOf[T])
  }
  class ImplicitAnswer extends Answer[Any] {

    implicit def toImplicit(method:Method) = new {
      def isImplicit:Boolean = {
        val clazz = method.getDeclaringClass
        (clazz :: clazz.getInterfaces.toList).exists (
          ScalaSigParser.parse(_).exists(
          	_.topLevelClasses.head.children.exists(m => m.name == method.getName && m.isImplicit)
          )
        )
      }
      trait Mock
      def extraInterfaces:Seq[Class[_]] = {
        val clazz = method.getDeclaringClass
        val interfaces = 
        	for {
        		c <- clazz :: clazz.getInterfaces.toList
        		p <- ScalaSigParser.parse(c).toSeq
        		s <- p.topLevelClasses.head.children.find(_.name == method.getName).toSeq
        		val sym = s.asInstanceOf[MethodSymbol]
        		i <- sym.children.grep[ClassSymbol]
        		val r = i.infoType.asInstanceOf[RefinedType]
        		x <- r.typeRefs.grep[TypeRefType].map(_.symbol).grep[ClassSymbol]
        	} yield Class.forName(x.parent.map((k) => x.path.replace("." + x.name, "$" + x.name)).getOrElse(x.path))
        	
        classOf[Mock] :: interfaces.filterNot(_ == method.getReturnType)
      }
    }
    
    override def answer(invocation:InvocationOnMock) = {
      if (invocation.getMethod.isImplicit || invocation.getMethod.getName()(0).isUpper) {
        
        val key = invocation.getMethod
        
        def updateAndGetMock(arg:Arg) = {
          cache.update(key, arg :: cache.getOrElse(key, Nil))
          arg.mock
        }
        invocation.getArguments().headOption match {
          case None | Some(null) => 
            cache.get(key) match {
              case None => updateAndGetMock(AnyArg(createMock(invocation))) 
              case Some(list) => list.head.mock 
            }
          case Some(arg) =>
            cache.get(key) match {
              case None => updateAndGetMock(SomeArg(arg, createMock(invocation)))
              case Some(list) => 
				list.grep[SomeArg].find(_.arg == arg)
				    .orElse(list.find(_.isInstanceOf[AnyArg]))
				    .map(_.mock)
				    .getOrElse(updateAndGetMock(SomeArg(arg, createMock(invocation))))
            }
        }
      } else {
        null
      }
    }
    
   def createMock(invocation:InvocationOnMock) = {
     Mockito.mock(invocation.getMethod.getReturnType.asInstanceOf[Class[AnyRef]],
            Mockito.withSettings().name(invocation.getMethod.getName()).extraInterfaces(invocation.getMethod.extraInterfaces:_*))
   } 

  }
}

class MockitoExtensions[T](obj:T) {
  def returns(result:T) = {
    when(obj).thenReturn(result)
  }

  def answers(function: Any => Any) {
    when(obj).thenAnswer(new Answer[Any] {
      def answer(invocation: InvocationOnMock) = function(invocation.getArguments()(0))
    })
  }

  def throws (e:Exception) {
    when(obj).thenThrow(e)
  }
}