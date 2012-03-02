package com.github.lucascs.implicitmocks

class ActiveRecord[T](t:T) {
	def save = {
	  println("saving " + t)
	  t
	}
	def update = {
	  println("updating " + t)
	}
	def delete = {
	  println("deleting " + t)
	}
}