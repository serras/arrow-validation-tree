package arrow.validation

import arrow.core.NonEmptyList
import kotlin.reflect.KProperty

public typealias ValidationTree<P, E> = ValidationNode.Root<P, E>
public typealias PropertyValidationTree<E> = ValidationTree<KProperty<*>, E>

public sealed interface PathElement<out P> {
  public data class Field<out P>(val name: P) : PathElement<P>
  public data class Index<out P>(val index: Int) : PathElement<P>
}

public sealed interface ValidationNode<out P, out E> {
  public sealed interface Nested<out P, out E> :
    ValidationNode<P, E>

  public sealed interface WithChildren<out P, out E> :
    ValidationNode<P, E> {
    public val children: NonEmptyList<Nested<P, E>>

    public val problems: List<E>
      get() = children.filterIsInstance<Problem<E>>().map { it.error }

    public operator fun get(name: @UnsafeVariance P): Field<P, E>? = get(PathElement.Field(name))

    public operator fun get(index: Int): Field<P, E>? = get(PathElement.Index(index))

    public operator fun get(path: PathElement<@UnsafeVariance P>): Field<P, E>? =
      children.filterIsInstance<Field<P, E>>().firstOrNull { it.path == path }
  }

  public data class Root<out P, out E>(
    override val children: NonEmptyList<Nested<P, E>>,
  ) : WithChildren<P, E>

  public data class Problem<E>(
    val error: E,
  ) : Nested<Nothing, E>

  public data class Field<out P, out E>(
    val path: PathElement<P>,
    override val children: NonEmptyList<Nested<P, E>>,
  ) : Nested<P, E>, WithChildren<P, E>
}

public fun <P, E> ValidationNode.WithChildren<P, E>?.problemsOrEmpty(): List<E> =
  this?.problems.orEmpty()

public operator fun <P, E> ValidationNode.WithChildren<P, E>?.get(name: @UnsafeVariance P): ValidationNode.Field<P, E>? =
  this?.get(name)

public operator fun <P, E> ValidationNode.WithChildren<P, E>?.get(index: Int): ValidationNode.Field<P, E>? =
  this?.get(index)
