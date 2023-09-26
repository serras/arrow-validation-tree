package arrow.validation

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate

data class Person(val name: Name, val age: Int)
data class Name(val first: String, val last: String)

fun person(firstName: String, lastName: String, age: Int): Either<PropertyValidationTree<String>, Person> =
  validationTree {
    fields(
      Person::name to {
        fields(
          Name::first to { ensure(firstName.isNotEmpty()) { "empty" } },
          Name::last to { ensure(lastName.isNotEmpty()) { "empty" } },
        ) { _, _ -> Name(firstName, lastName) }
      },
      Person::age to { ensure(age > 0) { "age is negative" } },
    ) { name, _ -> Person(name, age) }
  }

fun personE(firstName: String, lastName: String, age: Int): Either<NonEmptyList<String>, Person> =
  either {
    zipOrAccumulate(
      { ensure(firstName.isNotEmpty()) { "first name is empty" } },
      { ensure(lastName.isNotEmpty()) { "last name is empty" } },
      { ensure(age > 0) { "age is negative" } },
    ) { _, _, _ -> Person(Name(firstName, lastName), age) }
  }

fun main() {
  val r = person("", ",", -2).swap().getOrNull()!!
  println(r[Person::name].problemsOrEmpty())
  println(r[Person::name][Name::first].problemsOrEmpty())

  r.inspect {
    Person::name.inspect {
      Name::first.inspect { "${problems.size} problems about first name" }
      Name::last.inspect { "${problems.size} problems about last name" }
    }
    Person::age.inspect { "${problems.size} problems about age" }
  }
}
