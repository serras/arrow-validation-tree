# Arrow Validation Tree

[Arrow](https://arrow-kt.io/) provides powerful tools to deal with 
[validation](https://arrow-kt.io/learn/typed-errors/validation/), using its generic
[typed errors](https://arrow-kt.io/learn/typed-errors/working-with-typed-errors/) framework.
Alas, by default any validation errors are "flattened" into a single value or list, and that in turn
makes it difficult to figure out which fields have validation errors.

To make this more concrete, let's introduce a couple of data classes:

```kotlin
data class Person(val name: Name, val age: Int)
data class Name(val first: String, val last: String)
```

## Validation

To keep things simple, we'll just check that both parts of the name are not empty, and that the age
is positive. Using Arrow's [validation](https://arrow-kt.io/learn/typed-errors/validation/) operators,
we come to the following:

```kotlin
import arrow.core.*
import arrow.core.raise.*

fun person(firstName: String, lastName: String, age: Int): Either<NonEmptyList<String>, Person> =
  either {
    zipOrAccumulate(
      { ensure(firstName.isNotEmpty()) { "first name is empty" } },
      { ensure(lastName.isNotEmpty()) { "last name is empty" } },
      { ensure(age > 0) { "age is negative" } }
    ) { _, _, _ -> Person(Name(firstName, lastName), age) }
  }
```

The signature of this function tells us that we either get back a well-formed `Person`, or a
`NonEmptyList<String>` as validation errors. Since those errors are not linked to the different
components of a `Person`, we are forced to include that information as part of the message.
This is not great: in a user interface where errors are shown next to each input field, you
don't want that duplicate information.

Arrow Validation Tree allows you to label (or tag) the different sub-validations. You can use
any type as label, but `KProperty` works pretty well in that case. Here's the validation of a
`Person`, labelled accordingly.

```kotlin
import arrow.validation.*

fun person(firstName: String, lastName: String, age: Int): Either<PropertyValidationTree<String>, Person> =
  validationTree {
    fields(
      Person::name to {
        fields(
          Name::first to { ensure(firstName.isNotEmpty()) { "empty" } },
          Name::last to { ensure(lastName.isNotEmpty()) { "empty" } },
        ) { _, _ -> Name(firstName, lastName) }
      },
      Person::age to { ensure(age > 0) { "age is negative" } }
    ) { name, _ -> Person(name, age) }
  }
```

The API exposed by `validationTree` is quite small. The main difference is that we need to
provide the labels that ultimately appear in the validation tree; we can see that we change
`zipOrAccumulate` to `fields`. The table below summarizes the rest of the changes.

| `Raise` operation | `ValidationTreeRaise` operation | Label                  |
|-|-|------------------------|
| `zipOrAccumulate` | `fields` | Given explicitly       |
| `mapOrAccumulate` | `elements` | Taken from the indices |

## Inspection

The result of `validationTree` in the failure case is now a `ValidationTree`, instead of simply
a `NonEmptyList`. Such tree remembers the structure of the validation, so you can more easily
inspect the problems:

```kotlin
// the tree only contains 'problems' if any was present
validationTree[Person::name][Name::first]?.problems
// 'problemsOrEmpty' is a utility method to always get a List back
validationTree[Person::age].problemsOrEmpty()
```

We also provide _structured inspection_, in which nested blocks correspond to problems nested
in the tree. This is useful, among other scenarios, when designing a user interface that should
inform about those errrors.

```kotlin
@Composable
fun personForm(
  first: String, last: String, age: Int, 
  errors: PropertyValidationTree<String>
) {
  errors.inspect {
    Person::name.inspect {
      Name::first.inspect {
        TextField(value = first, isError = hasProblems)
        problems.forEach { Text(text = it) }
      }
      Name::last.inspect { /* as above */ }
    }
  }
}
```
