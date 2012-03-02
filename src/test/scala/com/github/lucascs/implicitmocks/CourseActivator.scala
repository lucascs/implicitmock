package com.github.lucascs.implicitmocks

trait CourseActivator {

  implicit def activateCourse(course:Course) = new ActiveRecord(course)
  
}