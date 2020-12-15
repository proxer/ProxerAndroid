package me.proxer.app.util.data

import java.util.Queue

/**
 * @author Ruben Gees
 */
class UniqueQueue<T> : Queue<T> {

    private val internalList = LinkedHashSet<T>()

    override val size: Int
        get() = internalList.size

    override fun containsAll(elements: Collection<T>) = internalList.containsAll(elements)
    override fun removeAll(elements: Collection<T>) = internalList.removeAll(elements)
    override fun retainAll(elements: Collection<T>) = internalList.retainAll(elements)
    override fun addAll(elements: Collection<T>) = internalList.addAll(elements)
    override fun contains(element: T) = internalList.contains(element)
    override fun remove(element: T) = internalList.remove(element)
    override fun add(element: T) = internalList.add(element)
    override fun iterator() = internalList.iterator()
    override fun isEmpty() = internalList.isEmpty()
    override fun clear() = internalList.clear()

    override fun element() = internalList.firstOrNull() ?: throw NoSuchElementException()
    override fun peek() = internalList.firstOrNull()

    override fun offer(element: T): Boolean {
        val previousSize = internalList.size

        internalList.add(element)

        return previousSize != internalList.size
    }

    override fun poll(): T? = iterator().let { iterator ->
        when {
            iterator.hasNext() -> iterator.next().apply { iterator.remove() }
            else -> null
        }
    }

    override fun remove(): T = iterator().let { iterator ->
        when {
            iterator.hasNext() -> iterator.next().apply { iterator.remove() }
            else -> throw NoSuchElementException()
        }
    }
}
