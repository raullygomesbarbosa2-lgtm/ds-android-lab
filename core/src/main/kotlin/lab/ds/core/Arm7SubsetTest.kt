package lab.ds.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Arm7SubsetTest {
    @Test fun movAndAdd() {
        val cpu=Arm7Subset(); cpu.load(intArrayOf(0xE3A00007.toInt(),0xE2801005.toInt()))
        cpu.step(); cpu.step(); assertEquals(7,cpu.r[0]); assertEquals(12,cpu.r[1])
    }
    @Test fun branchSkipsWord() {
        val cpu=Arm7Subset(); cpu.load(intArrayOf(0xEA000000.toInt(),0xE3A00001.toInt(),0xE3A00002.toInt()))
        cpu.step(); cpu.step(); assertEquals(2,cpu.r[0])
    }
    @Test fun storeThenLoad() {
        val cpu=Arm7Subset(); cpu.r[1]=100; cpu.r[0]=0x12345678
        cpu.load(intArrayOf(0xE5810000.toInt(),0xE5912000.toInt()))
        cpu.r[0]=0x12345678; cpu.r[1]=100; cpu.step(); cpu.r[0]=0; cpu.step(); assertEquals(0x12345678,cpu.r[2])
    }
    @Test fun rejectsUnknownOpcode() {
        val cpu=Arm7Subset(); cpu.load(intArrayOf(0xFFFFFFFF.toInt()))
        assertFailsWith<UnsupportedOperationException> { cpu.step() }
    }
}
