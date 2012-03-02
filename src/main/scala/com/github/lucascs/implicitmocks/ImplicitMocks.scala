package com.github.lucascs.implicitmocks

import org.mockito.Mockito._
import org.mockito.stubbing.Answer
import org.mockito.verification.VerificationMode
import org.mockito.invocation.InvocationOnMock
import collection.mutable
import java.lang.reflect.Method
import org.scalatest.{FlatSpec, BeforeAndAfterEach}
import org.mockito.{MockSettings, Matchers, Mockito => Mojito}
import tools.scalap.scalax.rules.scalasig._

trait Mockito extends BeforeAndAfterEach { self:FlatSpec =>
  def mock[T](implicit manifest:Manifest[T]) = Mojito.mock(manifest.erasure, new ImplicitAnswer).asInstanceOf[T]
  def mock[T](answer:Answer[_>:T])(implicit manifest:Manifest[T]) = Mojito.mock(manifest.erasure, answer).asInstanceOf[T]
  def verify[T](obj:T) = Mojito.verify(obj)
  def verifyNoMoreInteractions(obj:AnyRef) { Mojito.verifyNoMoreInteractions(obj) }
  def verify[T](obj:T, verification:VerificationMode) = Mojito.verify(obj, verification)
  def spy[T](obj:T) = Mojito.spy(obj)
  def never = Mojito.never()
  def times(i : Int) = Mojito.times(i)
  def any[T](implicit manifest:Manifest[T]) = Matchers.any(manifest.erasure).asInstanceOf[T]
  def an[T]:T = null.asInstanceOf[T]
  def as[T](obj:T) = obj
  def eq[T](obj:T):T = Matchers.eq(obj)

  def theManifest[T](implicit m:Manifest[T]) = Matchers.eq(m)

  implicit def any2mockito[T](obj:T) = new MockitoExtensions(obj)
  override def afterEach() {
    cache = mutable.Map()
//    Mojito.validateMockitoUsage()
  }

  def mock[T](interfaces:Class[_]*)(implicit manifest:Manifest[T]) = Mojito.mock(manifest.erasure, Mojito.withSettings.extraInterfaces(interfaces:_*)).asInstanceOf[T]

  
  case class Arg(mock:AnyRef)
  case class AnyArg(mock:AnyRef) extends Arg(mock)
  case class SomeArg(arg:AnyRef, mock:AnyRef) extends Arg(mock)
  
  private var cache:mutable.Map[Method, List[Arg]] = mutable.Map()

  private implicit def list2exp[F <: AnyRef](l:Seq[F]) = new {
    def grep[T](implicit m:Manifest[T]):Seq[T] = l.filter(x => m.erasure.isAssignableFrom(x.getClass)).map(_.asInstanceOf[T])
  }
  class ImplicitAnswer extends Answer[Any] {

    implicit def toImplicit(method:Method) = new {
      def isImplicit:Boolean = {
        val clazz = method.getDeclaringClass
        (clazz :: clazz.getInterfaces.toList).exists(
          ScalaSigParser.parse(_) match {
            case Some(p) =>
              p.topLevelClasses.head.children.find(_.name == method.getName) match {
                case Some(m) => m.isImplicit
                case None => false
              }
            case None => false
          }
        );
      }
      trait Mock
      def extraInterfaces:Seq[Class[_]] = {
        val clazz = method.getDeclaringClass
        val interfaces = (clazz :: clazz.getInterfaces.toList).map(
          ScalaSigParser.parse(_) match {
            case Some(p) => p.topLevelClasses.head.children.find(_.name == method.getName)
            case None => None
          }
        ).map {
          case Some(s) =>
            val sym = s.asInstanceOf[MethodSymbol]
             sym.children.grep[ClassSymbol].map(_.infoType match {
               case r:RefinedType => r.typeRefs.grep[TypeRefType].map(_.symbol).grep[ClassSymbol].map( x =>
                 Class.forName(x.parent.map((k) => x.path.replace("." + x.name, "$" + x.name)).getOrElse(x.path))
               )
             }).flatten
          case None => Nil
        }
        classOf[Mock] :: interfaces.flatten.filterNot(_ == method.getReturnType)
      }
    }
    
    override def answer(invocation:InvocationOnMock) = {
      if (invocation.getMethod.isImplicit || invocation.getMethod.getName()(0).isUpper) {
        
        val key = invocation.getMethod
        
        invocation.getArguments().headOption.orNull match {
          case null => 
            cache.get(key) match {
              case None => 
                val mock = createMock(invocation)
                cache.put(key, AnyArg(mock) :: Nil)
                mock
              case Some(list) => list.head.mock 
            }
          case arg =>
            cache.get(key) match {
              case None => 
                val mock = createMock(invocation)
                cache.put(key, SomeArg(arg, mock) :: Nil)
                mock
              case Some(list) => 
                val someargs = list.grep[SomeArg]
                someargs.find(_.arg == arg)
            }
        }
      } else {
        null
      }
    }
    
   def createMock(invocation:InvocationOnMock) = {
     Mojito.mock(invocation.getMethod.getReturnType.asInstanceOf[Class[AnyRef]],
            Mojito.withSettings().name(invocation.getMethod.getName()).extraInterfaces(invocation.getMethod.extraInterfaces:_*))
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