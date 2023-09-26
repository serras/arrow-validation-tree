package arrow.validation

public interface InspectValidationTree<P, E> {
  public val problems: List<E>
  public val hasProblems: Boolean
    get() = problems.isNotEmpty()
  public fun <A> P.inspect(block: InspectValidationTree<P, E>.() -> A): A
  public fun <A> Int.inspect(block: InspectValidationTree<P, E>.() -> A): A
}

public inline fun <P, E, A> ValidationTree<P, E>?.inspect(
  block: InspectValidationTree<P, E>.() -> A
): A = block(InspectValidationTreeImpl(this))

public class InspectValidationTreeImpl<P, E>(
  public val tree: ValidationNode.WithChildren<P, E>?
): InspectValidationTree<P, E> {
  override val problems: List<E>
    get() = tree.problemsOrEmpty()

  @Suppress("OVERRIDE_BY_INLINE")
  override inline fun <A> P.inspect(
    block: InspectValidationTree<P, E>.() -> A
  ): A = block(InspectValidationTreeImpl(tree?.get(this)))

  @Suppress("OVERRIDE_BY_INLINE")
  override inline fun <A> Int.inspect(
    block: InspectValidationTree<P, E>.() -> A
  ): A = block(InspectValidationTreeImpl(tree?.get(this)))
}
