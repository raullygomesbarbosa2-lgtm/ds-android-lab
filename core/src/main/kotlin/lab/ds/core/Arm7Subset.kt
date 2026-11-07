package lab.ds.core

/** Original minimal ARM-state interpreter for research; not a DS emulator core. */
class Arm7Subset(val memory: ByteArray = ByteArray(1 shl 20)) {
    val r = IntArray(16)
    var cpsr: Int = 0
    var halted = false
    private fun u32(a: Int) = a.toLong() and 0xffffffffL
    private fun read32(a: Int): Int {
        if (a < 0 || a > memory.size - 4) throw IllegalArgumentException("read outside RAM: $a")
        return (memory[a].toInt() and 255) or ((memory[a+1].toInt() and 255) shl 8) or ((memory[a+2].toInt() and 255) shl 16) or ((memory[a+3].toInt() and 255) shl 24)
    }
    private fun write32(a: Int, v: Int) {
        if (a < 0 || a > memory.size - 4) throw IllegalArgumentException("write outside RAM: $a")
        for (i in 0..3) memory[a+i] = (v ushr (8*i)).toByte()
    }
    fun load(words: IntArray, address: Int = 0) { words.forEachIndexed { i, w -> write32(address + i*4, w) }; r[15] = address }
    private fun cond(c: Int): Boolean {
        val n=cpsr ushr 31 != 0; val z=cpsr ushr 30 and 1 != 0; val cbit=cpsr ushr 29 and 1 != 0; val v=cpsr ushr 28 and 1 != 0
        return when(c) { 0->z; 1->!z; 2->cbit; 3->!cbit; 4->n; 5->!n; 6->v; 7->!v; 8->cbit&&!z; 9->!cbit||z; 10->n==v; 11->n!=v; 12->!z&&n==v; 13->z||n!=v; 14->true; 15->true; else->false }
    }
    private fun nz(x: Int) { cpsr = (cpsr and 0x3fffffff) or (if(x<0) Int.MIN_VALUE else 0) or (if(x==0) 1 shl 30 else 0) }
    fun step(): Int {
        if(halted) return 0
        val pc=r[15]; val ins=read32(pc); r[15]=pc+4
        if(!cond(ins ushr 28)) return 1
        if(ins ushr 25 and 7 == 5) { val off=(ins shl 8 shr 6); if(ins ushr 24 and 1 != 0) r[14]=r[15]; r[15]=r[15]+4+off; return 3 }
        if(ins ushr 26 and 3 == 1) {
            val load=ins ushr 20 and 1 != 0; val rn=ins ushr 16 and 15; val rd=ins ushr 12 and 15
            val base=r[rn]; val off=if(ins ushr 25 and 1 != 0) throw UnsupportedOperationException("register-offset transfer") else ins and 0xfff
            val addr=if(ins ushr 23 and 1 != 0) base+off else base-off
            if(ins ushr 24 and 1 != 0) r[rn]=addr
            if(load) r[rd]=read32(addr) else write32(addr,r[rd]); return 2
        }
        if(ins ushr 26 and 3 == 0) {
            val opcode=ins ushr 21 and 15; val set=ins ushr 20 and 1 != 0; val rn=r[ins ushr 16 and 15]; val rd=ins ushr 12 and 15
            val op2=if(ins ushr 25 and 1 != 0) ins and 255 else { if(ins and 0x10 != 0) throw UnsupportedOperationException("register-shift operand") else { val amount=(ins ushr 7 and 31); Integer.rotateRight(r[ins and 15], amount) } }
            val result=when(opcode) { 0->rn and op2; 1->rn xor op2; 2->rn-op2; 4->rn+op2; 12->rn or op2; 13->op2; 14->rn and op2.inv(); 15->op2.inv(); else->throw UnsupportedOperationException("data-processing opcode $opcode") }
            if(opcode !in listOf(8,9,10,11)) r[rd]=result
            if(set || opcode in 8..11) nz(result)
            return 1
        }
        throw UnsupportedOperationException("ARM instruction 0x${ins.toUInt().toString(16)} at 0x${pc.toUInt().toString(16)}")
    }
    fun run(maxSteps: Int = 10000): Int { var n=0; while(n<maxSteps&&!halted) { step(); n++ }; return n }
}
