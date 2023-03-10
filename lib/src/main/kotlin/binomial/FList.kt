package binomial

import java.util.NoSuchElementException

/*
 * FList - реализация функционального списка
 *
 * Пустому списку соответствует тип Nil, непустому - Cons
 *
 * Запрещено использовать
 *
 *  - var
 *  - циклы
 *  - стандартные коллекции
 *
 *  Исключение Array-параметр в функции flistOf. Но даже в ней нельзя использовать цикл и forEach.
 *  Только обращение по индексу
 */
sealed class FList<T>: Iterable<T> {
    // размер списка, 0 для Nil, количество элементов в цепочке для Cons
    abstract val size: Int
    // пустой ли списк, true для Nil, false для Cons
    abstract val isEmpty: Boolean

    // получить список, применив преобразование
    // требуемая сложность - O(n)
    abstract fun <U> map(f: (T) -> U): FList<U>

    // получить список из элементов, для которых f возвращает true
    // требуемая сложность - O(n)
    abstract fun filter(f: (T) -> Boolean): FList<T>

    // свертка
    // требуемая сложность - O(n)
    // Для каждого элемента списка (curr) вызываем f(acc, curr),
    // где acc - это base для начального элемента, или результат вызова
    // f(acc, curr) для предыдущего
    // Результатом fold является результат последнего вызова f(acc, curr)
    // или base, если список пуст
    abstract fun <U> fold(base: U, f: (U, T) -> U): U

    // разворот списка
    // требуемая сложность - O(n)
    fun reverse(): FList<T> = fold<FList<T>>(nil()) { acc, current ->
        Cons(current, acc)
    }

    // Решил реализовать собственный метод,
    // чтобы каждый раз не писать что-то в духе: iterator().next()
    // Берёт первый элемент
    // O(1)
    abstract fun first(): T

    /*
     * Это не очень красиво, что мы заводим отдельный Nil на каждый тип
     * И вообще лучше, чтобы Nil был объектом
     *
     * Но для этого нужны приседания с ковариантностью
     *
     * dummy - костыль для того, что бы все Nil-значения были равны
     *         и чтобы Kotlin-компилятор был счастлив (он требует, чтобы у Data-классов
     *         были свойство)
     *
     * Также для борьбы с бойлерплейтом были введены функция и свойство nil в компаньоне
     */
    data class Nil<T>(private val dummy: Int=0) : FList<T>() {
        override val size = 0
        override val isEmpty = true
        override fun <U> fold(base: U, f: (U, T) -> U): U = base

        override fun filter(f: (T) -> Boolean): FList<T> = this

        override fun <U> map(f: (T) -> U): FList<U> = Nil()

        override fun iterator(): Iterator<T> = FListIterator(this)

        override fun first(): T = throw NoSuchElementException("It's empty list")
    }

    data class Cons<T>(val head: T, val tail: FList<T>) : FList<T>() {
        override val size = 1 + tail.size;
        override val isEmpty = false
        override fun <U> fold(base: U, f: (U, T) -> U): U = foldRec(base, f, iterator())

        override fun filter(f: (T) -> Boolean): FList<T> = filterRec(Nil(), f, iterator()).reverse()

        override fun <U> map(f: (T) -> U): FList<U> = mapRec(Nil(), f, iterator()).reverse()

        override fun iterator(): Iterator<T> = FListIterator(this)

        override fun first(): T = head

        companion object {
            // Функции выглядят похоже, но вроде их нельзя эффективно объединить,
            // потому что типы разные и принимаемые параметры тоже разные
            private tailrec fun <U, T> foldRec(base: U, f: (U, T) -> U, iterator: Iterator<T>): U {
                if (iterator.hasNext()) {
                    val next = iterator.next();
                    return foldRec(f(base, next), f, iterator)
                }
                return base;
            }

            // Возвращает перевёрнутый отфильтрованный список
            private tailrec fun <T> filterRec(tail: FList<T>, f: (T) -> Boolean, iterator: Iterator<T>): FList<T> {
                if (iterator.hasNext()) {
                    val next = iterator.next();
                    return if (f(next)) filterRec(Cons(next, tail), f, iterator) else filterRec(tail, f, iterator)
                }
                return tail;
            }

            // Возвращает перевёрнутый отформатированный список
            private tailrec fun <U, T> mapRec(tail: FList<U>, f: (T) -> U, iterator: Iterator<T>): FList<U> {
                if (iterator.hasNext()) {
                    val next = iterator.next();
                    return mapRec(Cons(f(next), tail), f, iterator)
                }
                return tail;
            }
        }
    }

    companion object {
        fun <T> nil() = Nil<T>()
        val nil = Nil<Any>()
    }
}

class FListIterator<T>(private var node: FList<T>): Iterator<T> {

    override fun hasNext(): Boolean = !node.isEmpty

    override fun next(): T = when(node) {
        is FList.Nil<T> -> throw NoSuchElementException("Next element does not exist")
        is FList.Cons<T> -> {
            // Mutable - классная вещь, ничего не скажешь
            // Вот эти вот страшная конструкции из-за того, что компилятор думает,
            // что эта переменная может меняться в других потоках,
            // но мы ведь делаем без многопоточки
            val cnode = node as FList.Cons<T>
            val res = cnode.head
            node = cnode.tail
            res
        }
    }
}


// конструирование функционального списка в порядке следования элементов
// требуемая сложность - O(n)
fun <T> flistOf(vararg values: T): FList<T> = flistOf(FList.Nil(), values.iterator()).reverse()

private tailrec fun <T> flistOf(tail: FList<T>, iterator: Iterator<T>): FList<T> {
    if (iterator.hasNext()) {
        val head = iterator.next();
        return flistOf(FList.Cons(head, tail), iterator)
    }
    return tail
}