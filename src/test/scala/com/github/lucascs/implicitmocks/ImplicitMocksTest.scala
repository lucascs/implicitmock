package com.github.lucascs.implicitmocks
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class ImplicitMocksTest extends FlatSpec with ImplicitMocks with ShouldMatchers {

  val activator = mock[CourseActivator]
  import activator._
  
  "ImplicitMocks" should "be able to run implicit method without null pointer exceptions" in {
    val course = new Course
    
    course.save
  }
  
  it should "be able to stub implicit methods" in {
	  val course = new Course
	  val other = new Course
	  
	  course.save returns other
	  
	  course.save should be === other
  }
  
  it should "be able to verify implicit methods" in {
	  val course = new Course
	  
	  course.save
	  
	  verify(as[ActiveRecord[Course]](course)).save
	  verify(as[ActiveRecord[Course]](course), never).update
  }
  
  it should "be able to specify different outcomes for different objects" in {
	  val course = new Course
	  val other = new Course
	  
	  course.save returns other
	  other.save throws new RuntimeException("abc")
	  
	  course.save should be === other
	  evaluating(other.save) should produce[RuntimeException]
  }
  
  it should "be able to stub implicit methods from other traits" in {
	  val course = new Course
	  
	  course.totalSales returns 2
	  
	  course.totalSales should be === 2
  }
  
  it should "be able to verify implicit methods from other traits" in {
	  val course = new Course
	  
	  course.likes
	  
	  verify(as[PimpedCourse](course)).likes
	  verify(as[PimpedCourse](course), never).totalSales
  }
  
  it should "be able to stub any objects" in {
	  val course = new Course
	  
	  any[Course].likes returns 3
	  
	  course.likes should be === 3
  }
  it should "be able to verify any objects" in {
	  val course = new Course
	  
	  course.likes
	  
	  verify(as[PimpedCourse](any[Course])).likes
	  verify(as[PimpedCourse](any[Course]), never).totalSales
  }
}