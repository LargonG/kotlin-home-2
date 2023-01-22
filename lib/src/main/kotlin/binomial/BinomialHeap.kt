package binomial

import java.rmi.UnexpectedException

/*
 * BinomialHeap - реализация биномиальной кучи
 *
 * https://en.wikipedia.org/wiki/Binomial_heap
 *
 * Запрещено использовать
 *
 *  - var
 *  - циклы
 *  - стандартные коллекции
 *
 * Детали внутренней реазации должны быть спрятаны
 * Создание - только через single() и plus()
 *
 * Куча совсем без элементов не предусмотрена => от Nullable значения можно избавиться
 *
 * Операции
 *
 * plus с кучей
 * plus с элементом
 * top - взятие минимального элемента
 * drop - удаление минимального элемента
 */
class BinomialHeap<T: Comparable<T>>
    private constructor(private val trees: FList<BinomialTree<T>>): SelfMergeable<BinomialHeap<T>> {
    companion object {
        fun <T: Comparable<T>> single(value: T): BinomialHeap<T> = BinomialHeap(flistOf(BinomialTree.single(value)))


        // Было бы очень интересно послушать, как вот от этого избавиться
        // (Не изменяя логики)


        // Вызывается в самом начале рекурсии
        private fun <T: Comparable<T>> plus(a: BinomialTree<T>, b: BinomialTree<T>,
                                    aIterator: Iterator<BinomialTree<T>>,
                                    bIterator: Iterator<BinomialTree<T>>): FList<BinomialTree<T>> {
            // Коллекции деревьев отсортированы в порядке возрастания order
            when {
                a.order == b.order -> {
                    val tree = a + b
                    return if (aIterator.hasNext() && bIterator.hasNext()) {
                        plus(aIterator.next(), bIterator.next(), tree, aIterator, bIterator)
                    } else if (aIterator.hasNext()) {
                        // Осталась коллекция A
                        plus(aIterator.next(), tree, aIterator)
                    } else if (bIterator.hasNext()) {
                        // Осталась коллекция B
                        plus(bIterator.next(), tree, bIterator)
                    } else {
                        // Ни одной коллекции не осталось
                        flistOf(tree)
                    }
                }

                a.order < b.order ->
                    return if (aIterator.hasNext())
                        plus(aIterator.next(), b, a, aIterator, bIterator)
                    else
                        plus(b, a, bIterator)

                else -> return plus(b, a, bIterator, aIterator)
            }
        }

        // Середина рекурсии
        private fun <T: Comparable<T>> plus(a: BinomialTree<T>, b: BinomialTree<T>,
                                    last: BinomialTree<T>,
                                    aIterator: Iterator<BinomialTree<T>>,
                                    bIterator: Iterator<BinomialTree<T>>): FList<BinomialTree<T>> {
            // a - элемент из первой кучи
            // b - элемент из второй кучи
            // last - последний элемент слияния двух куч, который мы ещё не добавили
            // aIterator - итератор по коллекции деревьев из первой кучи
            // bIterator - итератор по коллекции деревьев из второй кучи
            // Коллекции деревьев отсортированы в порядке возрастания их .order поля
            when {
                a.order == b.order -> {
                    // Сливаем деревья
                    val tree = a + b // tree.order > last (tree.order - last.order >= 1)
                    return if (aIterator.hasNext() && bIterator.hasNext()) {
                        FList.Cons(last, plus(aIterator.next(), bIterator.next(), tree, aIterator, bIterator))
                    } else if (aIterator.hasNext()) {
                        // Осталась коллекция A
                        FList.Cons(last, plus(aIterator.next(), tree, aIterator))
                    } else if (bIterator.hasNext()) {
                        // Осталась коллекция B
                        FList.Cons(last, plus(bIterator.next(), tree, bIterator))
                    } else {
                        // Ни одной коллекции не осталось
                        FList.Cons(last, flistOf(tree))
                    }
                }
                a.order < b.order -> {
                    if (a.order == last.order) {
                        val tree = a + last // tree.order = a.order + 1
                        return if (aIterator.hasNext()) {
                            plus(aIterator.next(), b, tree, aIterator, bIterator)
                        } else {
                            plus(b, tree, bIterator)
                        }
                    } else if (a.order > last.order) {
                        return if (aIterator.hasNext()) {
                            FList.Cons(last, plus(aIterator.next(), b, a, aIterator, bIterator))
                        } else {
                            FList.Cons(last, plus(b, a, bIterator))
                        }
                    } else {
                        // a.order < last.order
                        throw UnexpectedException("A order cannot be less than Last order")
                    }
                }
                // Симметричная ситуация с a.order < b.order, только для b
                else -> return plus(b, a, last, bIterator, aIterator)
            }
        }

        // Если одна из коллекций закончилась
        private fun <T: Comparable<T>> plus(elem: BinomialTree<T>, last: BinomialTree<T>,
                                    iterator: Iterator<BinomialTree<T>>): FList<BinomialTree<T>> {
            // Коллекция деревьев отсортирована в порядке возрастания order
            when {
                elem.order == last.order -> {
                    val tree = elem + last // tree.order = elem.order + 1
                    return if (iterator.hasNext()) {
                        plus(iterator.next(), tree, iterator)
                    } else {
                        flistOf(tree)
                    }
                }
                last.order < elem.order -> {
                    return if (iterator.hasNext()) {
                        FList.Cons(last, plus(iterator.next(), elem, iterator));
                    } else {
                        flistOf(last, elem)
                    }
                }
                else -> throw UnexpectedException("Last order cannot be greater than elem")
            }
        }
    }

    /*
     * слияние куч
     *
     * Требуемая сложность - O(log(n))
     */
    override fun plus(other: BinomialHeap<T>): BinomialHeap<T> {
        val aIterator = trees.iterator()
        val a = aIterator.next() // Существует
        val bIterator = other.trees.iterator()
        val b = bIterator.next() // Существует
        return BinomialHeap(plus(a, b, aIterator, bIterator))
    }

    /*
     * добавление элемента
     * 
     * Требуемая сложность - O(log(n))
     */
    operator fun plus(elem: T): BinomialHeap<T> {
        val other = single(elem)
        return this.plus(other)
    }

    /*
     * минимальный элемент
     *
     * Требуемая сложность - O(log(n))
     */
    fun top(): T = trees.fold(trees.first().value) {a, b -> if (a < b.value) a else b.value}

    /*
     * удаление элемента
     *
     * Требуемая сложность - O(log(n))
     */
    fun drop(): BinomialHeap<T> {
        val min = top()
        val elem = trees.filter { it.value == min }.first() // Существует
        val my = trees.filter { it.value != min }

        return if (!elem.children.isEmpty && !my.isEmpty) {
            val heap = BinomialHeap(my)
            val other = BinomialHeap(elem.children)

            heap.plus(other)
        } else if (!elem.children.isEmpty) {
            BinomialHeap(elem.children)
        } else if (!my.isEmpty) {
            BinomialHeap(my)
        } else {
            throw UnexpectedException("Drop: elem & my are both empty")
        }
    }
}

