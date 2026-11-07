package lab.ds.core

/** Original ARM-state interpreter experiment; not a complete DS CPU or playable emulator. */
class Arm9Subset(val memory: ByteArray = ByteArray(1 shl 20)) {
    val r = IntArray(16)
    var cpsr = 0
    private fun check(a: Int, n: Int) { require(a >= 0 && a <= memory.size - n) { "test RAM access outside bounds: $a" } }
    private fun read32(a: Int): Int { check(a, 4); return (memory[a].toInt() and 255) or ((memory[a+1].toInt() and 255) shl 8) or ((memory[a+2].toInt() and 255) shl 16) or ((memory[a+3].toInt() and 255) shl 24) }
    private fun write32(a: Int, v: Int) { check(a, 4); for(i in 0..3) memory[a+i]=(v ushr (i*8)).toByte() }
    fun load(words: IntArray, address: Int=0) { words.forEachIndexed { i,w -> write32(address+i*4,w) }; r[15]=address }
    private fun bit(n:Int)=cpsr ushr n and 1 != 0
    private fun condition(c:Int):Boolean { val n=bit(31); val z=bit(30); val cbit=bit(29); val v=bit(28); return when(c) { 0->z;1->!z;2->cbit;3->!cbit;4->n;5->!n;6->v;7->!v;8->cbit&&!z;9->!cbit||z;10->n==v;11->n!=v;12->!z&&n==v;13->z||n!=v;14->true;else->throw UnsupportedOperationException("NV condition") } }
    private fun setNz(x:Int) { cpsr=(cpsr and 0x3fffffff) or (if(x<0) Int.MIN_VALUE else 0) or (if(x==0) 1 shl 30 else 0) }
    private fun setArithmeticFlags(a:Int, b:Int, carryIn:Int, result:Int, subtract:Boolean) {
        setNz(result)
        val unsignedA = a.toLong() and 0xffffffffL
        val unsignedB = b.toLong() and 0xffffffffL
        val carry = if (subtract) unsignedA >= unsignedB + (1L - carryIn.toLong()) else unsignedA + unsignedB + carryIn.toLong() > 0xffffffffL
        val overflow = if (subtract) ((a xor b) and (a xor result)) < 0 else ((a xor result) and (b xor result)) < 0
        cpsr = (cpsr and 0x8fffffff) or (if (carry) 1 shl 29 else 0) or (if (overflow) 1 shl 28 else 0)
    }
    fun step():Int {
        val pc=r[15]; val ins=read32(pc); r[15]=pc+4
        if(!condition(ins ushr 28)) return 1
        if(ins ushr 25 and 7==5) { val off=(ins shl 8) shr 6; if(ins ushr 24 and 1 != 0) r[14]=pc+4; r[15]=pc+8+off; return 3 }
        if(ins ushr 26 and 3==1) {
            if(ins ushr 25 and 1 != 0 || ins ushr 22 and 1 != 0) throw UnsupportedOperationException("only immediate word transfers")
            val rn=ins ushr 16 and 15; val rd=ins ushr 12 and 15; val base=if(rn==15) pc+8 else r[rn]; val off=ins and 0xfff
            val adjusted=if(ins ushr 23 and 1 != 0) base+off else base-off; val pre=ins ushr 24 and 1 != 0; val addr=if(pre) adjusted else base
            if(ins ushr 20 and 1 != 0) r[rd]=read32(addr) else write32(addr,if(rd==15) pc+8 else r[rd])
            if(!pre || ins ushr 21 and 1 != 0) r[rn]=adjusted
            return 2
        }
        if(ins ushr 26 and 3==0) {
            if(ins ushr 4 and 1 != 0 && ins ushr 25 and 1==0) throw UnsupportedOperationException("register shift")
            val opcode=ins ushr 21 and 15; val set=ins ushr 20 and 1 != 0; val rnIndex=ins ushr 16 and 15; val a=if(rnIndex==15) pc+8 else r[rnIndex]; val rd=ins ushr 12 and 15
            val immediate = ins ushr 25 and 1 != 0
            val op2=if(immediate) Integer.rotateRight(ins and 255,(ins ushr 8 and 15)*2) else { val x=if((ins and 15)==15) pc+8 else r[ins and 15]; Integer.rotateRight(x,ins ushr 7 and 31) }
            val carryIn=if(bit(29)) 1 else 0
            val result=when(opcode) {0,8->a and op2;1,9->a xor op2;2,10->a-op2;3->op2-a;4->a+op2;5->a+op2+carryIn;6->a-op2-(1-carryIn);7->op2-a-(1-carryIn);12->a or op2;13->op2;14->a and op2.inv();15->op2.inv();else->throw UnsupportedOperationException("data opcode $opcode")}
            val test=opcode in 8..11
            if(!test) { if(rd==15) throw UnsupportedOperationException("PC destination"); r[rd]=result }
            if(set || test) {
                when(opcode) {
                    2,10 -> setArithmeticFlags(a,op2,1,result,true)
                    3 -> setArithmeticFlags(op2,a,1,result,true)
                    4 -> setArithmeticFlags(a,op2,0,result,false)
                    5 -> setArithmeticFlags(a,op2,carryIn,result,false)
                    6 -> setArithmeticFlags(a,op2,carryIn,result,true)
                    7 -> setArithmeticFlags(op2,a,carryIn,result,true)
                    else -> setNz(result)
                }
            }
            return 1
        }
        throw UnsupportedOperationException("ARM instruction 0x${ins.toUInt().toString(16)} at 0x${pc.toUInt().toString(16)}")
    }
    fun run(maxSteps:Int=10000):Int { var count=0; while(count<maxSteps) { step(); count++ }; return count }
}
