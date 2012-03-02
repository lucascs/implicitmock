package com.github.lucascs.implicitmocks

trait CourseActivator {

  implicit def activateCourse(course:Course) = new ActiveRecord(course) with PimpedCourse {
    def totalSales = 0
  }
  
  
  trait PimpedCourse {
    def totalSales:Int
    def likes:Int
  }
}