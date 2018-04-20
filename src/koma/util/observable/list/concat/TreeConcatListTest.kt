package koma.util.observable.list.concat

import javafx.collections.FXCollections
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TreeConcatListTest {
    private var testConcatList = TreeConcatList<Int, String>()
    private var sublist0 = FXCollections.observableArrayList("a", "b", "c")
    private var sublist1 = FXCollections.observableArrayList("d", "e")
    private var sublist2 = FXCollections.observableArrayList("f")

    private val sublist_yz = FXCollections.observableArrayList("y", "z")
    private val sublist_c1 = FXCollections.observableArrayList("c1", "c2")
    private val sublist_gj = FXCollections.observableArrayList("g", "h", "i", "j")

    @BeforeEach
    fun setUp() {
        testConcatList = TreeConcatList<Int, String>()
        sublist0 = FXCollections.observableArrayList("a", "b", "c")
        sublist1 = FXCollections.observableArrayList("d", "e")
        sublist2 = FXCollections.observableArrayList("f")
        testConcatList.put(3, sublist0)
        testConcatList.put(5, sublist1)
        testConcatList.put(9, sublist2)
    }

    @Test
    fun getSize() {
        assertEquals(6, testConcatList.size)
        sublist0.add("c1")
        assertEquals(7, testConcatList.size)
        sublist0.add("c2")
        assertEquals(8, testConcatList.size)
        sublist0.add(1,"a1")
        assertEquals(9, testConcatList.size)
        sublist0.removeAt(2)
        assertEquals(8, testConcatList.size)
        sublist0.remove("a")
        assertEquals(7, testConcatList.size)
        sublist1.remove("d")
        assertEquals(6, testConcatList.size)
        sublist2.add("g")
        assertEquals(7, testConcatList.size)
    }

    @Test
    fun get() {
        assertEquals("a", testConcatList[0])
        sublist0.add("c1")
        assertEquals("c1", testConcatList[3])
        assertEquals("d", testConcatList[4])
        sublist0.add("c2")
        assertEquals("c2", testConcatList[4])
        sublist0.add(1,"a1")
        assertEquals("a1", testConcatList[1])
        assertEquals("b", testConcatList[2])
        sublist0.removeAt(2)
        assertEquals("a1", testConcatList[1])
        assertEquals("c", testConcatList[2])
        sublist0.remove("a")
        assertEquals("a1", testConcatList[0])
        sublist1.remove("d")
        assertEquals("e", testConcatList[4])
        sublist2.add("g")
        assertEquals("g", testConcatList[6])
    }

    @Test
    fun put() {
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), testConcatList)
        testConcatList.put(-1, sublist_yz)
        assertEquals(listOf("y", "z", "a", "b", "c", "d", "e", "f"), testConcatList)
        testConcatList.put(11, sublist_gj)
        assertEquals(listOf("y", "z", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j"),
                testConcatList)
        testConcatList.put(4, sublist_c1)
        assertEquals(listOf("y", "z", "a", "b", "c", "c1", "c2", "d", "e", "f", "g", "h", "i", "j"),
                testConcatList)
    }

    @Test
    fun remove() {
        testConcatList.remove(5)
        assertEquals(listOf("a", "b", "c", "f"), testConcatList)
        testConcatList.put(-1, sublist_yz)
        assertEquals(listOf("y", "z", "a", "b", "c", "f"), testConcatList)
        testConcatList.remove(9)
        assertEquals(listOf("y", "z", "a", "b", "c"), testConcatList)
        testConcatList.put(4, sublist_c1)
        assertEquals(listOf("y", "z", "a", "b", "c", "c1", "c2"), testConcatList)
        testConcatList.remove(3)
        assertEquals(listOf("y", "z", "c1", "c2"), testConcatList)
    }

    @Test
    fun testBoundsExceptions() {
        assertThrows<IndexOutOfBoundsException> { testConcatList.get(-1) }
        assertThrows<IndexOutOfBoundsException> { testConcatList.get(6) }
        assertEquals("f", testConcatList.get(5))
        testConcatList.remove(9)
        assertThrows<IndexOutOfBoundsException> { testConcatList.get(5) }
    }

    @Test
    fun testSublistAddRemove() {
        assertThrows<IllegalArgumentException> { testConcatList.put(5, sublist_yz) }
        assertThrows<IllegalArgumentException> { testConcatList.put(5, sublist_yz) }
        assertThrows<IllegalArgumentException> { testConcatList.put(9, sublist_yz) }
        assertEquals(sublist0, testConcatList.remove(3))
        assertEquals(null, testConcatList.remove(3))
        assertEquals(null, testConcatList.remove(3))
        assertEquals(sublist1, testConcatList.remove(5))
        assertEquals(null, testConcatList.remove(5))
        assertEquals(sublist2, testConcatList.remove(9))
    }

    @Test
    fun testSublistChange() {
        sublist0.clear()
        assertEquals(listOf("d", "e", "f"), testConcatList)
        sublist0.addAll("b", "c")
        assertEquals(listOf("b", "c", "d", "e", "f"), testConcatList)
        testConcatList.remove(3)
        sublist0.addAll("x", "y")
        assertEquals(listOf("d", "e", "f"), testConcatList)
    }

    @Test
    fun testDuplicateSublist() {
        testConcatList.put(4, sublist0)
        assertEquals(listOf("a", "b", "c", "a", "b", "c", "d", "e", "f"), testConcatList)
        sublist0.setAll("v", "w")
        assertEquals(listOf("v", "w", "v", "w", "d", "e", "f"), testConcatList)
        testConcatList.remove(3)
        assertEquals(listOf("v", "w", "d", "e", "f"), testConcatList)
        sublist0.add(0, "u")
        assertEquals(listOf("u", "v", "w", "d", "e", "f"), testConcatList)
    }

    @Test
    fun testNesting() {
        val concatList1 = TreeConcatList<Int, String>()
        concatList1.put(3, testConcatList)
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), concatList1)
        sublist1.setAll("d1", "e1")
        assertEquals(listOf("a", "b", "c", "d1", "e1", "f"), concatList1)
        sublist0.removeAt(2)
        assertEquals(listOf("a", "b", "d1", "e1", "f"), concatList1)
        concatList1.put(4, sublist_gj)
        assertEquals(listOf("a", "b", "d1", "e1", "f", "g", "h", "i", "j"), concatList1)
        sublist_gj.remove(1, 4)
        assertEquals(listOf("a", "b", "d1", "e1", "f", "g"), concatList1)

        val concatList2 = TreeConcatList<Int, String>()
        concatList2.put(5, concatList1)
        assertEquals(listOf("a", "b", "d1", "e1", "f", "g"), concatList2)
        concatList2.put(6, testConcatList)
        assertEquals(listOf("a", "b", "d1", "e1", "f", "g", "a", "b", "d1", "e1", "f"), concatList2)
        testConcatList.remove(3)
        assertEquals(listOf("d1", "e1", "f", "g", "d1", "e1", "f"), concatList2)
        sublist2[0] = "f1"
        assertEquals(listOf("d1", "e1", "f1", "g", "d1", "e1", "f1"), concatList2)
        concatList1.remove(3)
        assertEquals(listOf("g", "d1", "e1", "f1"), concatList2)
    }
}
