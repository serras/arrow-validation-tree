package arrow.validation

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.RaiseDSL
import arrow.core.raise.fold

public fun <P, E, A> validationTree(
  block: ValidationTreeRaise<P, E>.() -> A,
): Either<ValidationTree<P, E>, A> = fold<NonEmptyList<ValidationElement<P, E>>, A, Either<ValidationTree<P, E>, A>>(
  block = { ValidationTreeRaise(emptyList(), RaiseAccumulate(this)).block() },
  recover = { Either.Left(it.toValidationTree()) },
  transform = { Either.Right(it) },
)

internal data class ValidationElement<P, E>(
  val path: List<PathElement<P>>,
  val error: E,
)

private fun <P, E> NonEmptyList<ValidationElement<P, E>>.toValidationTree(): ValidationTree<P, E> {
  var current = nonEmptyListOf(head.initial())
  tail.forEach { current = current.add(it) }
  return ValidationNode.Root(current)
}

private fun <P, E> NonEmptyList<ValidationNode.Nested<P, E>>.add(e: ValidationElement<P, E>): NonEmptyList<ValidationNode.Nested<P, E>> =
  when (val first = e.path.firstOrNull()) {
    null -> this + ValidationNode.Problem(e.error)
    else -> {
      var found = false
      val firstGo = map {
        when {
          it is ValidationNode.Field && it.path == first -> {
            found = true
            it.copy(children = it.children.add(e))
          }

          else -> it
        }
      }
      if (found) firstGo else this + e.initial()
    }
  }

private fun <P, E> ValidationElement<P, E>.initial(): ValidationNode.Nested<P, E> = when (val first = path.firstOrNull()) {
  null -> ValidationNode.Problem(error)
  else -> ValidationNode.Field(first, nonEmptyListOf(copy(path = path.drop(1)).initial()))
}

public class ValidationTreeRaise<P, E> internal constructor(
  private val currentPath: List<PathElement<P>>,
  private val underlying: RaiseAccumulate<ValidationElement<P, E>>,
) : Raise<E> {

  @RaiseDSL
  public override fun raise(r: E): Nothing =
    underlying.raise(ValidationElement(currentPath, r))

  @RaiseDSL
  public fun <A, B, C> field(
    name: P,
    action: ValidationTreeRaise<P, E>.() -> A,
  ): A =
    action.invoke(ValidationTreeRaise(currentPath + PathElement.Field(name), underlying))

  @RaiseDSL
  public fun <A, B, C> fields(
    action1: Pair<P, ValidationTreeRaise<P, E>.() -> A>,
    action2: Pair<P, ValidationTreeRaise<P, E>.() -> B>,
    block: (A, B) -> C,
  ): C = with(underlying) {
    val x1 = extend(PathElement.Field(action1.first), action1.second)
    val x2 = extend(PathElement.Field(action2.first), action2.second)
    return block(x1(), x2())
  }

  @RaiseDSL
  public fun <A, B, C, D> fields(
    action1: Pair<P, ValidationTreeRaise<P, E>.() -> A>,
    action2: Pair<P, ValidationTreeRaise<P, E>.() -> B>,
    action3: Pair<P, ValidationTreeRaise<P, E>.() -> C>,
    block: (A, B, C) -> D,
  ): D = with(underlying) {
    val x1 = extend(PathElement.Field(action1.first), action1.second)
    val x2 = extend(PathElement.Field(action2.first), action2.second)
    val x3 = extend(PathElement.Field(action3.first), action3.second)
    return block(x1(), x2(), x3())
  }

  @RaiseDSL
  public fun <A, B, C, D, F> fields(
    action1: Pair<P, ValidationTreeRaise<P, E>.() -> A>,
    action2: Pair<P, ValidationTreeRaise<P, E>.() -> B>,
    action3: Pair<P, ValidationTreeRaise<P, E>.() -> C>,
    action4: Pair<P, ValidationTreeRaise<P, E>.() -> D>,
    block: (A, B, C, D) -> F,
  ): F = with(underlying) {
    val x1 = extend(PathElement.Field(action1.first), action1.second)
    val x2 = extend(PathElement.Field(action2.first), action2.second)
    val x3 = extend(PathElement.Field(action3.first), action3.second)
    val x4 = extend(PathElement.Field(action4.first), action4.second)
    return block(x1(), x2(), x3(), x4())
  }

  @RaiseDSL
  public fun <A, B, C, D, F, G> fields(
    action1: Pair<P, ValidationTreeRaise<P, E>.() -> A>,
    action2: Pair<P, ValidationTreeRaise<P, E>.() -> B>,
    action3: Pair<P, ValidationTreeRaise<P, E>.() -> C>,
    action4: Pair<P, ValidationTreeRaise<P, E>.() -> D>,
    action5: Pair<P, ValidationTreeRaise<P, E>.() -> F>,
    block: (A, B, C, D, F) -> G,
  ): G = with(underlying) {
    val x1 = extend(PathElement.Field(action1.first), action1.second)
    val x2 = extend(PathElement.Field(action2.first), action2.second)
    val x3 = extend(PathElement.Field(action3.first), action3.second)
    val x4 = extend(PathElement.Field(action4.first), action4.second)
    val x5 = extend(PathElement.Field(action5.first), action5.second)
    return block(x1(), x2(), x3(), x4(), x5())
  }

  @RaiseDSL
  public fun <A, B> Iterable<A>.elements(
    block: ValidationTreeRaise<P, E>.(A) -> B,
  ): List<B> = with(underlying) {
    mapOrAccumulate(this@elements.withIndex()) { (ix, value) ->
      extend(PathElement.Index(ix), block)(value)
    }
  }

  private fun <A> extend(path: PathElement<P>, block: ValidationTreeRaise<P, E>.() -> A): RaiseAccumulate<ValidationElement<P, E>>.() -> A = {
    val r = ValidationTreeRaise(currentPath + path, this)
    block.invoke(r)
  }

  private fun <A, B> extend(path: PathElement<P>, block: ValidationTreeRaise<P, E>.(A) -> B): RaiseAccumulate<ValidationElement<P, E>>.(A) -> B = {
    val r = ValidationTreeRaise(currentPath + path, this)
    block.invoke(r, it)
  }
}
