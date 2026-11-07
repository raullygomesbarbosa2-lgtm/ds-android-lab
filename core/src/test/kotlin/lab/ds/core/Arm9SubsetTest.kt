package lab.ds.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Arm9SubsetTest {
    @Test fun movAddAndPcAdvance() {
        val cpu=Arm9Subset(); cpu.load(intArrayOf(0xE3A00007.toInt(),0xE2801005.toInt()))
        cpu.step(); cpu.step()
        assertEquals(7,cpu.r[0]); assertEquals(12,cpu.r[1]); assertEquals(8,cpu.r[15])
    }
    @Test fun branchSkipsInstructionAndLinkStoresReturnAddress() {
        val cpu=Arm9Subset(); cpu.load(intArrayOf(0xEB000000.toInt(),0xE3A00001.toInt(),0xE3A00002.toInt()))
        cpu.step(); assertEquals(4,cpu.r[14]); assertEquals(8,cpu.r[15]); cpu.step(); assertEquals(2,cpu.r[0])
    }
    @Test fun addSetsZeroAndCarryFlags() {
        val cpu=Arm9Subset(); cpu.r[0]=0xffffffff.toInt(); cpu.load(intArrayOf(0xE2901001.toInt()))
        cpu.step(); assertEquals(0,cpu.r[1]); assertEquals(1,cpu.cpsr ushr 30 and 1); assertEquals(1,cpu.cpsr ushr 29 and 1)
    }
    @Test fun failedConditionOnlyAdvancesPc() {
        val cpu=Arm9Subset(); cpu.load(intArrayOf(0x13A00001)) // MOVNE when Z=0 actually executes
        cpu.cpsr=1 shl 30; cpu.step(); assertEquals(0,cpu.r[0]); assertEquals(4,cpu.r[15])
    }
    @Test fun immediateWordStoreLoad() {
        val cpu=Arm9Subset(); cpu.r[1]=128; cpu.r[0]=0x12345678
        cpu.load(intArrayOf(0xE5810000.toInt(),0xE5912000.toInt()))
        cpu.step(); cpu.step(); assertEquals(0x12345678,cpu.r[2])
    }
    @Test fun unsupportedOpcodeIsExplicit() {
        val cpu=Arm9Subset(); cpu.load(intArrayOf(0xFFFFFFFF.toInt()))
        assertFailsWith<UnsupportedOperationException> { cpu.step() }
    }
}
